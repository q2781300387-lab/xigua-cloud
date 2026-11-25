package com.xigua.common.core.thread;

import com.xigua.common.core.model.UserToken;
import com.xigua.common.core.util.UserContext;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @ClassName ThreadContextDecorator
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/15 11:01
 */
public class ThreadContextDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        RequestAttributes requestAttributes =
                RequestContextHolder.getRequestAttributes();

        // 优化 UserContext应该定义成接口
        UserToken userToken = UserContext.get();
        return () -> {
            try {
                if (requestAttributes != null) {
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                }
                if (userToken != null) {
                    UserContext.set(userToken);
                }
                runnable.run();
            }finally {
                UserContext.clear();
            }
        };
    }
}
