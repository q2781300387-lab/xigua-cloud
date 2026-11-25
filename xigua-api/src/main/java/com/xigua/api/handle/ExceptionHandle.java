package com.xigua.api.handle;

import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.base.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * @ClassName ExceptionHandler
 * @Description
 * @Author wangjinfei
 * @Date 2025/4/27 10:15
 */
@Slf4j
@RestControllerAdvice
public class ExceptionHandle {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R MethodArgumentNotValidExceptionHandle(MethodArgumentNotValidException exception) {
        BindingResult result = exception.getBindingResult();
        if (result.hasErrors()) {
            // 拼接所有错误提示
            String errorMsg = result.getAllErrors()
                    .stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining(","));
            return R.fail(errorMsg);
        }
        return R.fail("参数不可为空！");
    }

    @ExceptionHandler(BusinessException.class)
    public R BusinessExceptionHandle(BusinessException e) {
        log.error("业务异常捕获 --->>>", e);
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R MaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("文件上传异常捕获 --->>>", e);
        return R.fail("文件大小超过限制");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("请求方式不支持：method={}, uri={}, supported={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getSupportedHttpMethods());

        return R.fail("请求方式不支持");
    }

    @ExceptionHandler(RpcException.class)
    public R RpcException(RpcException e) {
        log.error("dubbo rpc 异常捕获 --->>>", e);
        String msg = e.getMessage();
        if (msg != null && msg.contains("BusinessException")) {
            return R.fail(extractBizMsg(msg));
        }
        return R.fail("RPC调用异常");
    }

    private String extractBizMsg(String msg) {
        if (msg == null) {
            return null;
        }

        final String BIZ_EXCEPTION_FLAG = "BusinessException:";
        int index = msg.indexOf(BIZ_EXCEPTION_FLAG);
        if (index == -1) {
            return msg;
        }

        return msg.substring(index + BIZ_EXCEPTION_FLAG.length()).trim();
    }

    @ExceptionHandler(Exception.class)
    public R exceptionHandle(Exception e) {
        log.error("异常捕获 --->>>", e);
        return R.fail("系统错误");
    }
}
