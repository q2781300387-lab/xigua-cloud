package com.xigua.common.core.util;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 高德地图 IP 定位工具类
 * 用于根据 IP 地址查询地理位置及经纬度
 *
 * @author Claude
 * @date 2026/03/20
 */
@Slf4j
public class AmapUtil {

    /**
     * 高德地图 IP 定位 API 地址
     */
    private static final String AMAP_IP_API_URL = "https://restapi.amap.com/v3/ip";

    /**
     * 高德地图 API Key（需要在高德开放平台申请）
     * 建议通过配置文件或环境变量配置
     */
    private static String apiKey = "xxx";

    /**
     * OkHttp 客户端
     */
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    /**
     * 设置高德地图 API Key
     *
     * @param key API Key
     */
    public static void setApiKey(String key) {
        apiKey = key;
    }

    /**
     * 根据 IP 地址查询地理位置信息
     *
     * @param ip IP 地址
     * @return 地理位置信息
     */
    public static IpLocationInfo getLocation(String ip) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("高德地图 API Key 未配置");
            return null;
        }

        if (ip == null || ip.isEmpty()) {
            log.warn("IP 地址为空");
            return null;
        }

        // 本地回环地址直接返回 null
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            log.debug("本地回环地址，跳过查询");
            return null;
        }

        try {
            // 构建请求 URL
            HttpUrl url = HttpUrl.parse(AMAP_IP_API_URL).newBuilder()
                    .addQueryParameter("key", apiKey)
                    .addQueryParameter("ip", ip)
                    .addQueryParameter("output", "json")
                    .build();

            // 创建请求
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            // 执行请求
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.error("高德 IP 定位请求失败，状态码：{}", response.code());
                return null;
            }

            // 解析响应
            String json = response.body().string();
            JSONObject jsonObject = JSONObject.parseObject(json);

            // 检查返回状态
            String status = jsonObject.getString("status");
            if (!"1".equals(status)) {
                log.error("高德 IP 定位返回错误：{}", jsonObject.getString("info"));
                return null;
            }

            // 构建返回对象
            IpLocationInfo info = new IpLocationInfo();
            info.setIp(ip);
            info.setProvince(jsonObject.getString("province"));
            info.setCity(jsonObject.getString("city"));
            info.setAdcode(jsonObject.getString("adcode"));

            // 解析矩形范围并计算中心点经纬度
            String rectangle = jsonObject.getString("rectangle");
            if (rectangle != null && !rectangle.isEmpty()) {
                String[] lngLat = calculateCenter(rectangle);
                if (lngLat != null) {
                    info.setLng(lngLat[0]);
                    info.setLat(lngLat[1]);
                }
            }

            return info;

        } catch (IOException e) {
            log.error("高德 IP 定位请求异常：ip={}", ip, e);
            return null;
        } catch (Exception e) {
            log.error("高德 IP 定位解析异常：ip={}", ip, e);
            return null;
        }
    }

    /**
     * 从矩形范围计算中心点经纬度
     * 矩形范围格式：lng1,lat1;lng2,lat2
     *
     * @param rectangle 矩形范围字符串
     * @return [经度, 纬度]
     */
    private static String[] calculateCenter(String rectangle) {
        try {
            String[] points = rectangle.split(";");
            if (points.length != 2) {
                return null;
            }

            String[] point1 = points[0].split(",");
            String[] point2 = points[1].split(",");

            if (point1.length != 2 || point2.length != 2) {
                return null;
            }

            // 计算中心点
            double lng1 = Double.parseDouble(point1[0]);
            double lat1 = Double.parseDouble(point1[1]);
            double lng2 = Double.parseDouble(point2[0]);
            double lat2 = Double.parseDouble(point2[1]);

            double centerLng = (lng1 + lng2) / 2;
            double centerLat = (lat1 + lat2) / 2;

            return new String[]{
                    String.format("%.6f", centerLng),
                    String.format("%.6f", centerLat)
            };

        } catch (Exception e) {
            log.warn("解析矩形范围失败：{}", rectangle, e);
            return null;
        }
    }

    /**
     * IP 定位信息
     */
    @Data
    public static class IpLocationInfo {
        /**
         * IP 地址
         */
        private String ip;

        /**
         * 省份
         */
        private String province;

        /**
         * 城市
         */
        private String city;

        /**
         * 区域编码
         */
        private String adcode;

        /**
         * 经度
         */
        private String lng;

        /**
         * 纬度
         */
        private String lat;
    }

    public static void main(String[] args) {
        IpLocationInfo location = getLocation("123.114.102.198");
        System.out.println("location = " + location);
    }
}
