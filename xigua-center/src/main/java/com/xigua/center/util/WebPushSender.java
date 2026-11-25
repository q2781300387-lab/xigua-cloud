package com.xigua.center.util;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Base64;

/**
 * @ClassName WebPushSender
 * @Description Web Push 协议实现（RFC 8030 / 8291 / 8292）：
 *              1) 用 aes128gcm 加密 payload（ECDH + HKDF + AES-GCM）
 *              2) 用 VAPID(ES256) 对请求签名
 *              3) POST 到浏览器订阅端点
 *              纯 JDK 实现，不引入第三方推送库。
 * @Author xigua
 * @Date 2026/11/12 19:30
 */
@Slf4j
public class WebPushSender {

    private static final String CURVE = "secp256r1";
    private static final int RECORD_SIZE = 4096;
    private static final int TTL_SECONDS = 86400;

    private final String vapidPublicKey;
    private final String vapidPrivateKey;
    private final String subject;

    public WebPushSender(String vapidPublicKey, String vapidPrivateKey, String subject) {
        this.vapidPublicKey = vapidPublicKey;
        this.vapidPrivateKey = vapidPrivateKey;
        this.subject = subject;
    }

    /**
     * 按 RFC 8291 生成 aes128gcm 消息体：salt(16) | rs(4) | keyidLen(1) | 临时公钥(65) | 密文+tag
     * 独立成方法便于协议自检。
     */
    public static byte[] encryptPayload(String p256dh, String auth, byte[] plaintext) throws Exception {
        byte[] uaPublic = b64uDecode(p256dh);
        byte[] authSecret = b64uDecode(auth);

        // 1) 临时密钥对
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec(CURVE));
        KeyPair ephemeral = kpg.generateKeyPair();
        byte[] ephPublicBytes = toUncompressedPoint((ECPublicKey) ephemeral.getPublic());

        // 2) ECDH 共享密钥
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(KeyFactory.getInstance("EC").generatePublic(
                new ECPublicKeySpec(decodePoint(uaPublic), ecParams())), true);
        byte[] sharedSecret = ka.generateSecret();

        // 3) HKDF：auth_secret 作 extract salt → IKM → 再以随机 salt 派生 CEK/NONCE（RFC 8291 §3.3-3.4）
        byte[] salt = randomBytes(16);
        byte[] prkKey = hkdfExtract(authSecret, sharedSecret);
        byte[] info = concat("WebPush: info".getBytes(StandardCharsets.US_ASCII),
                new byte[]{0}, uaPublic, ephPublicBytes);
        byte[] ikm = hkdfExpand(prkKey, info, 32);
        byte[] prk = hkdfExtract(salt, ikm);
        byte[] cek = hkdfExpand(prk,
                concat("Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII), new byte[]{0}), 16);
        byte[] nonce = hkdfExpand(prk,
                concat("Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII), new byte[]{0}), 12);

        // 4) AES-128-GCM 加密（明文尾加 0x02 记录分隔符）
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[]{0x02}));

        // 5) 组装消息体
        return concat(salt, intToBytes(RECORD_SIZE), new byte[]{(byte) ephPublicBytes.length},
                ephPublicBytes, ciphertext);
    }

    /**
     * 向一条订阅推送通知
     *
     * @return true=推送成功；false=失败（410/404 表示订阅已失效，调用方可清理）
     */
    public boolean send(String endpoint, String p256dh, String auth, JSONObject payload) {
        try {
            byte[] body = encryptPayload(p256dh, auth, payload.toJSONString().getBytes(StandardCharsets.UTF_8));

            // VAPID 头
            String jwt = buildVapidJwt(originOf(endpoint));

            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Encoding", "aes128gcm");
            conn.setRequestProperty("TTL", String.valueOf(TTL_SECONDS));
            conn.setRequestProperty("Authorization", "vapid t=" + jwt + ",k=" + vapidPublicKey);
            conn.setFixedLengthStreamingMode(body.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return true;
            }
            log.warn("----->>>>>> Web Push 下发失败：status={}，endpoint={}...",
                    code, endpoint.length() > 50 ? endpoint.substring(0, 50) : endpoint);
            return false;
        } catch (Exception e) {
            log.error("----->>>>>> Web Push 下发异常：{}", e.getMessage(), e);
            return false;
        }
    }

    /** VAPID JWT：aud 取端点 origin，sub 用固定联系地址，有效期 12 小时 */
    private String buildVapidJwt(String origin) throws Exception {
        JSONObject header = new JSONObject();
        header.put("typ", "JWT");
        header.put("alg", "ES256");

        JSONObject claim = new JSONObject();
        claim.put("aud", origin);
        claim.put("exp", (System.currentTimeMillis() / 1000) + 43200);
        claim.put("sub", subject);

        String unsigned = b64u(header.toJSONString().getBytes(StandardCharsets.UTF_8))
                + "." + b64u(claim.toJSONString().getBytes(StandardCharsets.UTF_8));

        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(derivePrivateKey());
        signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
        byte[] der = signer.sign();

        // DER(Sig) → raw R||S（各 32 字节），Web Push 要求 raw
        return unsigned + "." + b64u(derToRaw(der));
    }

    private ECPrivateKey derivePrivateKey() throws Exception {
        byte[] d = b64uDecode(vapidPrivateKey);
        byte[] pub = b64uDecode(vapidPublicKey);
        ECParameterSpec params = ecParams();
        ECPrivateKeySpec privSpec = new ECPrivateKeySpec(new BigInteger(1, d), params);
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(privSpec);
    }

    private static ECParameterSpec ecParams() throws Exception {
        AlgorithmParameters ap = AlgorithmParameters.getInstance("EC");
        ap.init(new ECGenParameterSpec(CURVE));
        return ap.getParameterSpec(ECParameterSpec.class);
    }

    /** base64url 的 65 字节 uncompressed 点 → ECPoint */
    private static ECPoint decodePoint(byte[] data) {
        int half = (data.length - 1) / 2;
        return new ECPoint(new BigInteger(1, slice(data, 1, half)),
                new BigInteger(1, slice(data, 1 + half, half)));
    }

    private static byte[] toUncompressedPoint(ECPublicKey key) {
        byte[] x = toFixed(key.getW().getAffineX(), 32);
        byte[] y = toFixed(key.getW().getAffineY(), 32);
        return concat(new byte[]{0x04}, x, y);
    }

    /** DER 编码的 ECDSA 签名转 raw（防 sig 高位需补 0） */
    private static byte[] derToRaw(byte[] der) {
        int idx = 2;
        if ((der[1] & 0xFF) > 127) idx = 3;
        int lenR = der[idx + 1] & 0xFF;
        byte[] r = derToRawPart(der, idx + 2, lenR);
        int idxS = idx + 2 + lenR;
        int lenS = der[idxS + 1] & 0xFF;
        byte[] s = derToRawPart(der, idxS + 2, lenS);
        return concat(r, s);
    }

    private static byte[] derToRawPart(byte[] der, int off, int len) {
        byte[] out = new byte[32];
        int srcOff = off;
        int srcLen = len;
        // 去掉前导 0x00
        while (srcLen > 32 && der[srcOff] == 0) {
            srcOff++;
            srcLen--;
        }
        System.arraycopy(der, srcOff, out, 32 - srcLen, srcLen);
        return out;
    }

    private static byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        return mac.doFinal(ikm);
    }

    private static byte[] hkdfExpand(byte[] prk, byte[] info, int len) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        mac.update(info);
        mac.update((byte) 1);
        byte[] first = mac.doFinal();
        if (len <= 32) return slice(first, 0, len);
        byte[] out = new byte[len];
        System.arraycopy(first, 0, out, 0, 32);
        mac.reset();
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        mac.update(first);
        mac.update(info);
        mac.update((byte) 2);
        byte[] second = mac.doFinal();
        System.arraycopy(second, 0, out, 32, Math.min(len - 32, second.length));
        return out;
    }

    private static byte[] intToBytes(int v) {
        return new byte[]{(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v};
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }

    private static byte[] toFixed(BigInteger v, int len) {
        byte[] raw = v.toByteArray();
        byte[] out = new byte[len];
        int copy = Math.min(raw.length, len);
        System.arraycopy(raw, raw.length - copy, out, len - copy, copy);
        return out;
    }

    private static byte[] slice(byte[] src, int off, int len) {
        byte[] out = new byte[len];
        System.arraycopy(src, off, out, 0, len);
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    private static String originOf(String endpoint) {
        try {
            URL u = new URL(endpoint);
            return u.getProtocol() + "://" + u.getHost();
        } catch (Exception e) {
            return "https://fcm.googleapis.com";
        }
    }

    private static String b64u(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] b64uDecode(String s) {
        String padded = s + "====".substring(0, (4 - s.length() % 4) % 4);
        return Base64.getUrlDecoder().decode(padded.replace('+', '-').replace('/', '_'));
    }
}
