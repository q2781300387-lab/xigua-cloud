package com.xigua.center.util;

import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Web Push 协议自检：
 * 1) 用产线 encryptPayload 加密，再用 RFC 8291 解密流程还原明文（自洽性）
 * 2) 校验 aes128gcm 消息体布局（salt16 / rs4 / keyidLen1 / key65）
 * 3) 校验 VAPID JWT 结构与被公钥验签
 * 4) 起本地 HttpServer 接收 send() 的真实报文，校验 4 个必需头与包体
 */
public class WebPushSelfTest {

    private static final String CURVE = "secp256r1";

    public static void main(String[] args) throws Exception {
        int pass = 0, fail = 0;

        // ---------- 0. 生成模拟浏览器(UA)密钥对 与 模拟服务端 VAPID 密钥对 ----------
        KeyPair ua = newKeyPair();
        KeyPair vapid = newKeyPair();
        String uaP256dh = b64u(uncompressed((ECPublicKey) ua.getPublic()));
        String uaAuth = b64u(randomBytes(16));
        String vapidPub = b64u(uncompressed((ECPublicKey) vapid.getPublic()));
        String vapidPriv = b64u(fixed(((ECPrivateKey) vapid.getPrivate()).getS(), 32));

        System.out.println("[SETUP] uaP256dh.len=" + uaP256dh.length() + " uaAuth.len=" + uaAuth.length()
                + " vapidPub.len=" + vapidPub.length());

        // ---------- 1. 加密 ----------
        JSONObject payload = new JSONObject();
        payload.put("title", "新消息");
        payload.put("body", "C47 VAPID 自检：aes128gcm 加解密往返");
        payload.put("chatId", "234318157114769408");
        payload.put("chatType", 1);
        byte[] plain = payload.toJSONString().getBytes(StandardCharsets.UTF_8);
        byte[] body = WebPushSender.encryptPayload(uaP256dh, uaAuth, plain);
        System.out.println("[ENC] plaintext=" + plain.length + "B body=" + body.length + "B");

        // ---------- 2. 布局校验 ----------
        byte[] salt = slice(body, 0, 16);
        int rs = ((body[16] & 0xFF) << 24) | ((body[17] & 0xFF) << 16) | ((body[18] & 0xFF) << 8) | (body[19] & 0xFF);
        int keyIdLen = body[20] & 0xFF;
        byte[] ephPub = slice(body, 21, keyIdLen);
        byte[] ct = slice(body, 21 + keyIdLen, body.length - 21 - keyIdLen);

        pass += check("消息体 salt 长度 = 16", salt.length == 16);
        pass += check("record size = 4096", rs == 4096);
        pass += check("keyid 长度 = 65（uncompressed P-256 点）", keyIdLen == 65);
        pass += check("临时公钥首字节 = 0x04", (ephPub[0] & 0xFF) == 0x04);
        pass += check("密文长度 = 明文+分隔符+16B tag", ct.length == plain.length + 1 + 16);

        // ---------- 3. 解密还原（模拟浏览器侧） ----------
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ua.getPrivate());
        ka.doPhase(KeyFactory.getInstance("EC").generatePublic(
                new ECPublicKeySpec(decodePoint(ephPub), ecParams())), true);
        byte[] shared = ka.generateSecret();

        byte[] prkKey = hkdfExtract(b64uDecode(uaAuth), shared);
        byte[] info2 = concat("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[]{0},
                uncompressed((ECPublicKey) ua.getPublic()), ephPub);
        byte[] ikm = hkdfExpand(prkKey, info2, 32);
        byte[] prk = hkdfExtract(salt, ikm);
        byte[] cek = hkdfExpand(prk, concat("Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII), new byte[]{0}), 16);
        byte[] nonce = hkdfExpand(prk, concat("Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII), new byte[]{0}), 12);

        Cipher dec = Cipher.getInstance("AES/GCM/NoPadding");
        dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] recovered = dec.doFinal(ct);
        // 去掉尾部 0x02 分隔符
        int end = recovered.length;
        while (end > 0 && recovered[end - 1] == 0x02) end--;
        String text = new String(slice(recovered, 0, end), StandardCharsets.UTF_8);

        pass += check("解密可还原原始 JSON", text.equals(payload.toJSONString()));
        System.out.println("[DEC] " + text);

        // ---------- 4. VAPID JWT 结构与验签 ----------
        String jwt = buildJwt(vapidPub, vapidPriv, "https://fcm.googleapis.com");
        String[] parts = jwt.split("\\.");
        JSONObject hdr = JSONObject.parseObject(new String(b64uDecode(parts[0]), StandardCharsets.UTF_8));
        JSONObject clm = JSONObject.parseObject(new String(b64uDecode(parts[1]), StandardCharsets.UTF_8));
        pass += check("JWT 为三段式", parts.length == 3);
        pass += check("JWT header alg=ES256", "ES256".equals(hdr.getString("alg")));
        pass += check("JWT aud = 端点 origin", "https://fcm.googleapis.com".equals(clm.getString("aud")));
        pass += check("JWT sub 存在", clm.getString("sub") != null && !clm.getString("sub").isEmpty());
        long expDelta = clm.getLongValue("exp") - (System.currentTimeMillis() / 1000);
        pass += check("JWT exp 在 11~12 小时内", expDelta > 11 * 3600 && expDelta <= 12 * 3600 + 60);

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(KeyFactory.getInstance("EC").generatePublic(
                new ECPublicKeySpec(decodePoint(b64uDecode(vapidPub)), ecParams())));
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        pass += check("JWT 签名可被 VAPID 公钥验签", verifier.verify(rawToDer(b64uDecode(parts[2]))));

        // ---------- 5. 真实 HTTP 报文校验 ----------
        final Map<String, String> headers = new HashMap<>();
        final byte[][] recv = new byte[1][];
        final CountDownLatch latch = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8099), 0);
        server.createContext("/push", ex -> {
            headers.put("Content-Encoding", String.valueOf(ex.getRequestHeaders().getFirst("Content-Encoding")));
            headers.put("TTL", String.valueOf(ex.getRequestHeaders().getFirst("TTL")));
            headers.put("Authorization", String.valueOf(ex.getRequestHeaders().getFirst("Authorization")));
            headers.put("Content-Type", String.valueOf(ex.getRequestHeaders().getFirst("Content-Type")));
            try (InputStream in = ex.getRequestBody(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                recv[0] = bos.toByteArray();
            }
            ex.sendResponseHeaders(201, -1);
            ex.close();
            latch.countDown();
        });
        server.start();

        WebPushSender sender = new WebPushSender(vapidPub, vapidPriv, "mailto:admin@xgchat.cn");
        boolean sent = sender.send("http://127.0.0.1:8099/push", uaP256dh, uaAuth, payload);
        boolean got = latch.await(6, TimeUnit.SECONDS);
        server.stop(0);

        pass += check("send() 对 201 返回 true", sent);
        pass += check("服务端收到报文", got);
        pass += check("Content-Encoding = aes128gcm", "aes128gcm".equals(headers.get("Content-Encoding")));
        pass += check("TTL = 86400", "86400".equals(headers.get("TTL")));
        pass += check("Content-Type = application/octet-stream", "application/octet-stream".equals(headers.get("Content-Type")));
        pass += check("Authorization 以 vapid t= 开头且带 k=",
                headers.get("Authorization").startsWith("vapid t=") && headers.get("Authorization").contains(",k="));
        pass += check("收到的包体长度与加密结果一致", recv[0] != null && recv[0].length == body.length);
        pass += check("收到的包体可解密还原", recv[0] != null
                && decryptEquals(recv[0], ua.getPrivate(), (ECPublicKey) ua.getPublic(), uaAuth, payload.toJSONString()));

        System.out.println("--------------------------------------------------");
        System.out.println("[RESULT] PASS=" + pass + "  FAIL=" + (20 - pass));
        System.out.println(pass == 20 ? "[VERDICT] ALL PASS" : "[VERDICT] HAS FAILURE");
        if (pass != 20) System.exit(1);
    }

    // ------------ helpers ------------

    private static boolean decryptEquals(byte[] body, PrivateKey uaPriv, ECPublicKey uaPub,
                                         String auth, String expect) throws Exception {
        byte[] salt = slice(body, 0, 16);
        int keyIdLen = body[20] & 0xFF;
        byte[] ephPub = slice(body, 21, keyIdLen);
        byte[] ct = slice(body, 21 + keyIdLen, body.length - 21 - keyIdLen);

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(uaPriv);
        ka.doPhase(KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(decodePoint(ephPub), ecParams())), true);
        byte[] shared = ka.generateSecret();
        byte[] prkKey = hkdfExtract(b64uDecode(auth), shared);
        byte[] info = concat("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[]{0},
                uncompressed(uaPub), ephPub);
        byte[] ikm = hkdfExpand(prkKey, info, 32);
        byte[] prk = hkdfExtract(salt, ikm);
        byte[] cek = hkdfExpand(prk,
                concat("Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII), new byte[]{0}), 16);
        byte[] nonce = hkdfExpand(prk,
                concat("Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII), new byte[]{0}), 12);

        Cipher dec = Cipher.getInstance("AES/GCM/NoPadding");
        dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] rec = dec.doFinal(ct);
        int end = rec.length;
        while (end > 0 && rec[end - 1] == 0x02) end--;
        return new String(slice(rec, 0, end), StandardCharsets.UTF_8).equals(expect);
    }

    private static int check(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        return ok ? 1 : 0;
    }

    private static String buildJwt(String pub, String priv, String origin) throws Exception {
        JSONObject h = new JSONObject();
        h.put("typ", "JWT");
        h.put("alg", "ES256");
        JSONObject c = new JSONObject();
        c.put("aud", origin);
        c.put("exp", (System.currentTimeMillis() / 1000) + 43200);
        c.put("sub", "mailto:admin@xgchat.cn");
        String unsigned = b64u(h.toJSONString().getBytes(StandardCharsets.UTF_8)) + "."
                + b64u(c.toJSONString().getBytes(StandardCharsets.UTF_8));
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(KeyFactory.getInstance("EC").generatePrivate(
                new java.security.spec.ECPrivateKeySpec(new BigInteger(1, b64uDecode(priv)), ecParams())));
        s.update(unsigned.getBytes(StandardCharsets.US_ASCII));
        return unsigned + "." + b64u(derToRaw(s.sign()));
    }

    private static byte[] derToRaw(byte[] der) {
        int idx = 2;
        if ((der[1] & 0xFF) > 127) idx = 3;
        int lenR = der[idx + 1] & 0xFF;
        byte[] r = part(der, idx + 2, lenR);
        int iS = idx + 2 + lenR;
        int lenS = der[iS + 1] & 0xFF;
        return concat(r, part(der, iS + 2, lenS));
    }

    private static byte[] part(byte[] d, int off, int len) {
        byte[] out = new byte[32];
        int o = off, l = len;
        while (l > 32 && d[o] == 0) { o++; l--; }
        System.arraycopy(d, o, out, 32 - l, l);
        return out;
    }

    private static byte[] rawToDer(byte[] raw) {
        byte[] r = slice(raw, 0, 32), s = slice(raw, 32, 32);
        byte[] rb = trim(r), sb = trim(s);
        byte[] body = concat(new byte[]{0x02, (byte) rb.length}, rb, new byte[]{0x02, (byte) sb.length}, sb);
        return concat(new byte[]{0x30, (byte) body.length}, body);
    }

    private static byte[] trim(byte[] b) {
        int i = 0;
        while (i < b.length - 1 && b[i] == 0) i++;
        byte[] out = slice(b, i, b.length - i);
        if ((out[0] & 0x80) != 0) return concat(new byte[]{0}, out);
        return out;
    }

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(new ECGenParameterSpec(CURVE));
        return g.generateKeyPair();
    }

    private static ECParameterSpec ecParams() throws Exception {
        java.security.AlgorithmParameters ap = java.security.AlgorithmParameters.getInstance("EC");
        ap.init(new ECGenParameterSpec(CURVE));
        return ap.getParameterSpec(ECParameterSpec.class);
    }

    private static ECPoint decodePoint(byte[] d) {
        int half = (d.length - 1) / 2;
        return new ECPoint(new BigInteger(1, slice(d, 1, half)), new BigInteger(1, slice(d, 1 + half, half)));
    }

    private static byte[] uncompressed(ECPublicKey k) {
        return concat(new byte[]{0x04}, fixed(k.getW().getAffineX(), 32), fixed(k.getW().getAffineY(), 32));
    }

    private static byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(salt, "HmacSHA256"));
        return m.doFinal(ikm);
    }

    private static byte[] hkdfExpand(byte[] prk, byte[] info, int len) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(prk, "HmacSHA256"));
        m.update(info);
        m.update((byte) 1);
        return slice(m.doFinal(), 0, len);
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }

    private static byte[] fixed(BigInteger v, int len) {
        byte[] raw = v.toByteArray();
        byte[] out = new byte[len];
        int c = Math.min(raw.length, len);
        System.arraycopy(raw, raw.length - c, out, len - c, c);
        return out;
    }

    private static byte[] slice(byte[] s, int o, int l) {
        byte[] out = new byte[l];
        System.arraycopy(s, o, out, 0, l);
        return out;
    }

    private static byte[] concat(byte[]... p) {
        int t = 0;
        for (byte[] x : p) t += x.length;
        byte[] out = new byte[t];
        int pos = 0;
        for (byte[] x : p) { System.arraycopy(x, 0, out, pos, x.length); pos += x.length; }
        return out;
    }

    private static String b64u(byte[] d) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
    }

    private static byte[] b64uDecode(String s) {
        String p = s + "====".substring(0, (4 - s.length() % 4) % 4);
        return Base64.getUrlDecoder().decode(p.replace('+', '-').replace('/', '_'));
    }
}
