package com.xigua.admin.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xigua.admin.domain.OperLog;
import com.xigua.admin.service.AdminOperLogService;
import com.xigua.common.core.base.R;
import com.xigua.common.core.model.UserToken;
import com.xigua.common.core.util.UserContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录管理端写操作。只审计 POST/PUT/DELETE，分页查询和认证接口不记录。
 */
@Slf4j
@Aspect
@Component
public class AdminOperLogAspect {

    private final AdminOperLogService adminOperLogService;
    private final ObjectMapper objectMapper;

    public AdminOperLogAspect(AdminOperLogService adminOperLogService, ObjectMapper objectMapper) {
        this.adminOperLogService = adminOperLogService;
        this.objectMapper = objectMapper;
    }

    @Around("execution(* com.xigua.admin.controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null || !shouldAudit(request)) {
            return joinPoint.proceed();
        }

        long startedAt = System.currentTimeMillis();
        Throwable failure = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            try {
                writeLog(request, joinPoint.getArgs(), result, failure,
                        System.currentTimeMillis() - startedAt);
            } catch (Exception ex) {
                log.warn("记录后台操作日志失败: {}", ex.getMessage());
            }
        }
    }

    private boolean shouldAudit(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method)
                && !"PUT".equalsIgnoreCase(method)
                && !"DELETE".equalsIgnoreCase(method)) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        if (uri.startsWith("/admin/")) {
            uri = uri.substring("/admin".length());
        }
        if (uri.startsWith("/auth") || uri.startsWith("/captcha") || uri.startsWith("/oper-log")) {
            return false;
        }
        return !uri.endsWith("/page");
    }

    private void writeLog(HttpServletRequest request, Object[] args, Object result,
                          Throwable failure, long costMs) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/admin/")) {
            uri = uri.substring("/admin".length());
        }

        OperLog operLog = new OperLog();
        UserToken user = UserContext.get();
        operLog.setAdminId(user == null ? "system" : user.getUserId());
        operLog.setAdminName(user == null || user.getUserName() == null ? "system" : user.getUserName());
        operLog.setModule(resolveModule(uri));
        operLog.setAction(resolveAction(uri, request.getMethod()));
        operLog.setRequestUri(uri);
        operLog.setMethod(request.getMethod());
        operLog.setParams(serializeArgs(args));
        operLog.setIp(resolveIp(request));
        operLog.setCostMs(costMs);

        boolean success = failure == null;
        if (success && result instanceof R<?> response) {
            success = response.getCode() == R.SUCCESS;
        }
        operLog.setStatus(success ? 1 : 0);
        if (failure != null) {
            String message = failure.getMessage();
            operLog.setErrorMsg(message == null ? failure.getClass().getSimpleName() : truncate(message, 900));
        }
        adminOperLogService.record(operLog);
    }

    private String resolveModule(String uri) {
        if (uri == null) {
            return "系统管理";
        }
        String first = uri.replaceFirst("^/+", "").split("/")[0];
        return switch (first) {
            case "config" -> "全局配置";
            case "user" -> "用户管理";
            case "group" -> "群组管理";
            case "bot" -> "机器人管理";
            case "online-user" -> "在线会话";
            case "middleware" -> "中间件监控";
            case "behavior" -> "异常行为";
            default -> "系统管理";
        };
    }

    private String resolveAction(String uri, String method) {
        if (uri != null && uri.startsWith("/config")) {
            if ("POST".equalsIgnoreCase(method)) {
                return "新增配置项";
            }
            if ("PUT".equalsIgnoreCase(method)) {
                return "修改配置项";
            }
            if ("DELETE".equalsIgnoreCase(method)) {
                return "删除配置项";
            }
        }
        if (uri != null && uri.startsWith("/user") && "PUT".equalsIgnoreCase(method)) {
            return "更新用户资料";
        }
        if (uri != null && uri.startsWith("/middleware") && uri.contains("refresh")) {
            return "刷新中间件实例";
        }
        return switch (method.toUpperCase()) {
            case "POST" -> "新增操作";
            case "PUT" -> "更新操作";
            case "DELETE" -> "删除操作";
            default -> method.toUpperCase();
        };
    }

    private String serializeArgs(Object[] args) {
        try {
            List<Object> safeArgs = new ArrayList<>();
            for (Object arg : args) {
                if (arg == null || arg instanceof ServletRequest || arg instanceof ServletResponse
                        || arg instanceof MultipartFile) {
                    continue;
                }
                safeArgs.add(arg);
            }
            if (safeArgs.isEmpty()) {
                return "";
            }
            String json = objectMapper.writeValueAsString(safeArgs);
            json = json.replaceAll("(?i)(\"(?:password|oldPassword|newPassword|token)\"\\s*:\\s*\")[^\"]*(\")",
                    "$1***$2");
            return truncate(json, 4000);
        } catch (Exception ex) {
            return "";
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}
