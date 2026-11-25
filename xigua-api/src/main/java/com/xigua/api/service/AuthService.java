package com.xigua.api.service;

import com.xigua.domain.dto.LoginDTO;
import com.xigua.domain.dto.RegisterUserDTO;
import com.xigua.domain.entity.User;
import com.xigua.domain.vo.LoginVO;

import java.util.Map;

/**
 * @ClassName AuthService
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/6/12 21:03
 */
public interface AuthService {
    /**
     * 注册
     * @author wangjinfei
     * @date 2025/4/27 9:53
     * @param dto
     * @return Boolean
     */
    Boolean register(RegisterUserDTO dto);

    /**
     * 登录
     * @author wangjinfei
     * @date 2025/5/7 13:40
     * @param loginDTO
     * @return Boolean
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 登出
     * @author wangjinfei
     * @date 2026/1/4 20:10
     * @param userId
    */
    void logout(String userId);

    /**
     * 只登出当前设备，其他端会话继续有效
     */
    void logoutCurrent(String userId, String token);

    /**
     * 创建token
     * @author wangjinfei
     * @date 2025/5/10 12:20
     * @param user
     * @return String
     */
    String createToken(User user);

    /**
     * 创建一次性ticket
     * @author wangjinfei
     * @date 2025/6/14 16:07
     * @param userId
     * @return String
    */
    String createTicket(String userId);

    /**
     * ticket兑换token
     * @author wangjinfei
     * @date 2025/6/14 21:46
     * @param ticket
     * @return String
    */
    String redeemToken(String ticket);

    /**
     * 查询当前用户名/IP 的登录失败次数，判断是否需要验证码
     */
    Map<String, Object> loginRisk(String username);

    /**
     * 生成一次性算术验证码
     */
    Map<String, Object> captcha(String username);
}
