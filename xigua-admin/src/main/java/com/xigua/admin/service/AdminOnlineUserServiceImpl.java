package com.xigua.admin.service;

import com.alibaba.fastjson2.JSONObject;
import com.xigua.api.service.OnlineUserService;
import com.xigua.api.service.UserService;
import com.xigua.api.service.admin.AdminOnlineUserService;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.AmapUtil;
import com.xigua.common.core.util.Ip2RegionUtil;
import com.xigua.common.core.util.IpUtil;
import com.xigua.common.core.util.UserAgentUtil;
import com.xigua.common.redis.client.RedisClient;
import com.xigua.domain.dto.QueryOnlineUserPageDTO;
import com.xigua.domain.entity.User;
import com.xigua.domain.enums.RedisEnum;
import com.xigua.domain.vo.admin.AdminOnlineUserDetailVO;
import com.xigua.domain.vo.admin.AdminOnlineUserStatsVO;
import com.xigua.domain.vo.admin.AdminOnlineUserVO;
import com.xigua.domain.vo.admin.OnlineUserInfoVO;
import com.xigua.domain.vo.admin.OnlineUserLocationVO;
import com.xigua.domain.vo.admin.OnlineUserLocationResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName AdminOnlineUserServiceImpl
 * @Description 管理端在线用户服务实现
 * @Author wangjinfei
 * @Date 2026/3/10
 */
@Slf4j
@Service
@DubboService
public class AdminOnlineUserServiceImpl implements AdminOnlineUserService {

    @Autowired
    private RedisClient redisClient;

    @DubboReference
    private UserService userService;
    @DubboReference
    private OnlineUserService onlineUserService;

    @Override
    public com.xigua.common.mybatis.base.BasePageVO<AdminOnlineUserVO> page(QueryOnlineUserPageDTO dto) {
        // todo 暂时从redis中查询所有在线用户，做伪分页
        Set<String> userIds = onlineUserService.getOnlineUsers();

        if (CollectionUtils.isEmpty(userIds)) {
            return com.xigua.domain.util.BasePage.emptyResult(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(dto.getPageNum(), dto.getPageSize())
            );
        }

        List<User> users = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(userIds)) {
            users = userService.getListByIds(new ArrayList<>(userIds));
        }

        String username = dto.getUsername();
        if (StringUtils.isNotEmpty(username)) {
            users = users.stream()
                .filter(user -> user.getUsername() != null && user.getUsername().contains(username))
                .collect(Collectors.toList());
        }

        List<AdminOnlineUserVO> voList = new ArrayList<>();
        for (User user : users) {
            String onlineUserKey = RedisEnum.ONLINE_USER.getKey() + user.getId();
            String onlineUserData = redisClient.get(onlineUserKey);
            if (StringUtils.isEmpty(onlineUserData)) {
                continue;
            }

            try {
                JSONObject onlineJson = JSONObject.parseObject(onlineUserData);
                AdminOnlineUserVO vo = new AdminOnlineUserVO();
                vo.setUserId(user.getId());
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
                vo.setLoginIp(IpUtil.maskIp(onlineJson.getString("loginIp")));
                vo.setLoginRegion(IpUtil.maskRegion(Ip2RegionUtil.search(onlineJson.getString("loginIp"))));
                vo.setHost(onlineJson.getString("host"));
                vo.setPort(onlineJson.getInteger("port"));
                vo.setUa(onlineJson.getString("ua"));

                Long loginTime = onlineJson.getLong("loginTime");
                if (loginTime != null) {
                    vo.setLoginTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(loginTime), ZoneId.systemDefault()));
                }

                voList.add(vo);
            } catch (Exception e) {
                log.warn("解析在线用户数据失败: {}", onlineUserData, e);
            }
        }

        int total = voList.size();
        int fromIndex = (dto.getPageNum() - 1) * dto.getPageSize();
        int toIndex = Math.min(fromIndex + dto.getPageSize(), total);

        List<AdminOnlineUserVO> pageList = voList.subList(fromIndex, toIndex);

        com.xigua.common.mybatis.base.BasePageVO<AdminOnlineUserVO> result = new com.xigua.common.mybatis.base.BasePageVO<>();
        result.setPageNum(dto.getPageNum());
        result.setPageSize(dto.getPageSize());
        result.setRows(pageList);
        result.setTotalCount((long) total);
        result.setTotalPage(total == 0 ? 0L : (long) ((total + dto.getPageSize() - 1) / dto.getPageSize()));

        return result;
    }

    @Override
    public AdminOnlineUserStatsVO getStats() {
        Set<String> userIds = onlineUserService.getOnlineUsers();
        AdminOnlineUserStatsVO stats = new AdminOnlineUserStatsVO();

        if (CollectionUtils.isEmpty(userIds)) {
            stats.setTotalOnline(0L);
            stats.setWebOnline(0L);
            stats.setMobileOnline(0L);
            return stats;
        }

        long totalOnline = userIds.size();
        long webOnline = 0L;
        long mobileOnline = 0L;

        for (String userId : userIds) {
            String onlineUserData = redisClient.get(RedisEnum.ONLINE_USER.getKey() + userId);
            if (StringUtils.isEmpty(onlineUserData)) {
                continue;
            }

            try {
                JSONObject onlineJson = JSONObject.parseObject(onlineUserData);
                String ua = onlineJson.getString("ua");
                // 使用 UserAgentUtil 判断设备类型
                if (UserAgentUtil.isMobile(ua)) {
                    mobileOnline++;
                } else {
                    webOnline++;
                }
            } catch (Exception e) {
                log.warn("解析在线用户数据失败: {}", onlineUserData, e);
            }
        }

        stats.setTotalOnline(totalOnline);
        stats.setWebOnline(webOnline);
        stats.setMobileOnline(mobileOnline);
        return stats;
    }

    @Override
    public AdminOnlineUserDetailVO getDetail(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return null;
        }

        // 从 Redis 获取在线用户信息
        String onlineUserKey = RedisEnum.ONLINE_USER.getKey() + userId;
        String onlineUserData = redisClient.get(onlineUserKey);

        if (StringUtils.isEmpty(onlineUserData)) {
            throw new BusinessException("该用户已下线");
        }

        try {
            JSONObject onlineJson = JSONObject.parseObject(onlineUserData);
            AdminOnlineUserDetailVO detail = new AdminOnlineUserDetailVO();

            // ========== 设备信息 ==========
            String ua = onlineJson.getString("ua");
            // 使用 UserAgentUtil 解析设备信息
            detail.setDeviceType(UserAgentUtil.getPlatform(ua));
            detail.setDevice(UserAgentUtil.getModel(ua));
            detail.setOsVersion(UserAgentUtil.getOS(ua));
            detail.setClientVersion("v1.0.0"); // 固定返回 v1.0.0

            // ========== 连接信息 ==========
            Long loginTime = onlineJson.getLong("loginTime");
            if (loginTime != null) {
                detail.setLoginTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(loginTime), ZoneId.systemDefault()));
                // 计算在线时长
                long durationSeconds = (System.currentTimeMillis() - loginTime) / 1000;
                detail.setOnlineDuration(formatDuration(durationSeconds));
            }
            detail.setHeartbeatStatus("在线"); // 能查到说明在线

            // ========== 网络信息 ==========
            String loginIp = onlineJson.getString("loginIp");
            detail.setIpAddress(loginIp);

            // 使用高德 API 获取地理位置和经纬度
            if (StringUtils.isNotEmpty(loginIp)) {
                try {
                    AmapUtil.IpLocationInfo locationInfo = AmapUtil.getLocation(loginIp);
                    if (locationInfo != null) {
                        // 设置登录地点（优先使用高德返回的城市信息）
                        String location = locationInfo.getProvince();
                        if (StringUtils.isNotEmpty(locationInfo.getCity()) && !"[]".equals(locationInfo.getCity())) {
                            location = locationInfo.getProvince() + "|" + locationInfo.getCity();
                        }
                        detail.setLoginLocation(location);

                        // 设置经纬度
                        detail.setLng(locationInfo.getLng());
                        detail.setLat(locationInfo.getLat());
                    } else {
                        // 高德 API 调用失败，降级使用 ip2region
                        detail.setLoginLocation(Ip2RegionUtil.search(loginIp));
                        detail.setLng(null);
                        detail.setLat(null);
                    }
                } catch (Exception e) {
                    log.warn("高德 IP 定位失败，使用 ip2region 降级处理: ip={}", loginIp, e);
                    detail.setLoginLocation(Ip2RegionUtil.search(loginIp));
                    detail.setLng(null);
                    detail.setLat(null);
                }
            }

            return detail;
        } catch (Exception e) {
            log.error("解析在线用户详情失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 格式化时长为 HH:mm:ss
     * @author Claude
     * @date 2026/3/20
     * @param seconds 秒数
     * @return String
     */
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    @Override
    public OnlineUserLocationResponseVO getOnlineUserLocations() {
        Set<String> userIds = onlineUserService.getOnlineUsers();

        if (CollectionUtils.isEmpty(userIds)) {
            return OnlineUserLocationResponseVO.builder()
                    .onlineUsers(Collections.emptyList())
                    .build();
        }

        // 获取所有用户信息
        List<User> users = userService.getListByIds(new ArrayList<>(userIds));
        if (CollectionUtils.isEmpty(users)) {
            return OnlineUserLocationResponseVO.builder()
                    .onlineUsers(Collections.emptyList())
                    .build();
        }

        // 按位置聚合用户
        Map<String, LocationGroup> locationMap = new LinkedHashMap<>();

        for (User user : users) {
            String onlineUserKey = RedisEnum.ONLINE_USER.getKey() + user.getId();
            String onlineUserData = redisClient.get(onlineUserKey);

            if (StringUtils.isEmpty(onlineUserData)) {
                continue;
            }

            try {
                JSONObject onlineJson = JSONObject.parseObject(onlineUserData);
                String loginIp = onlineJson.getString("loginIp");

                // 获取地理位置信息
                AmapUtil.IpLocationInfo locationInfo = null;
                String country = "";
                String city = "";
                Double lat = null;
                Double lng = null;

                if (StringUtils.isNotEmpty(loginIp)) {
                    try {
                        locationInfo = AmapUtil.getLocation(loginIp);
                        if (locationInfo != null) {
                            country = locationInfo.getProvince();
                            city = locationInfo.getCity();
                            if (StringUtils.isNotEmpty(city) && !"[]".equals(city)) {
                                // 去掉城市名称的[]
                                city = city.replaceAll("\\[|\\]", "");
                            } else {
                                city = "";
                            }
                            if (StringUtils.isNotEmpty(locationInfo.getLat())) {
                                lat = Double.parseDouble(locationInfo.getLat());
                            }
                            if (StringUtils.isNotEmpty(locationInfo.getLng())) {
                                lng = Double.parseDouble(locationInfo.getLng());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("获取IP地理位置失败: ip={}", loginIp, e);
                    }
                }

                // 构建位置聚合key
                String locationKey = country + "|" + city;

                // 添加用户信息
                OnlineUserInfoVO userInfo = OnlineUserInfoVO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .avatar(user.getAvatar())
                        .onlineStatus("ONLINE")
                        .build();

                // 聚合到对应位置
                if (locationMap.containsKey(locationKey)) {
                    LocationGroup group = locationMap.get(locationKey);
                    group.getUsers().add(userInfo);
                    group.setUserCount(group.getUsers().size());
                } else {
                    LocationGroup group = new LocationGroup();
                    group.setLat(lat);
                    group.setLng(lng);
                    group.setCountry(country);
                    group.setCity(city);
                    group.setUsers(new ArrayList<>());
                    group.getUsers().add(userInfo);
                    group.setUserCount(1);
                    locationMap.put(locationKey, group);
                }

            } catch (Exception e) {
                log.warn("解析在线用户数据失败: userId={}", user.getId(), e);
            }
        }

        // 构建返回结果
        List<OnlineUserLocationVO> onlineUserLocations = locationMap.values().stream()
                .map(group -> OnlineUserLocationVO.builder()
                        .lat(group.getLat())
                        .lng(group.getLng())
                        .country(group.getCountry())
                        .city(group.getCity())
                        .userCount(group.getUserCount())
                        .users(group.getUsers())
                        .build())
                .collect(Collectors.toList());

        return OnlineUserLocationResponseVO.builder()
                .onlineUsers(onlineUserLocations)
                .build();
    }

    /**
     * 位置聚合内部类
     */
    private static class LocationGroup {
        private Double lat;
        private Double lng;
        private String country;
        private String city;
        private Integer userCount;
        private List<OnlineUserInfoVO> users;

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public Integer getUserCount() {
            return userCount;
        }

        public void setUserCount(Integer userCount) {
            this.userCount = userCount;
        }

        public List<OnlineUserInfoVO> getUsers() {
            return users;
        }

        public void setUsers(List<OnlineUserInfoVO> users) {
            this.users = users;
        }
    }
}
