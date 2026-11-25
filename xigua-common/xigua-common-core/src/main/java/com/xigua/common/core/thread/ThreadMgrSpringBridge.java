package com.xigua.common.core.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @ClassName ThreadMgrSpringBridge
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/15 13:22
 */
@Slf4j
@Component
public class ThreadMgrSpringBridge implements ApplicationContextAware {
    private static final String BEAN_NAME = "xgExecutor";

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        ThreadMgrSpringBridge.applicationContext = applicationContext;

        log.info("ThreadMgrSpringBridge: Spring 环境检测成功，将使用Spring线程池");
        if (applicationContext instanceof ConfigurableApplicationContext ctx) {
            Executor executor = createSpringThreadPoolExecutor();
            registerExecutor(ctx, executor);
            ThreadMgr.markSpringEnv(executor);
        }
    }

    private static Executor createSpringThreadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(ThreadMgr.coreThreadSize());
        executor.setMaxPoolSize(ThreadMgr.maxThreadSize());
        executor.setQueueCapacity(ThreadMgr.workQueueSize());
        executor.setKeepAliveSeconds(ThreadMgr.keepAliveSeconds());
        executor.setThreadNamePrefix(ThreadMgr.threadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 线程装饰器 传递上下文
        executor.setTaskDecorator(new ThreadContextDecorator());
        executor.initialize();
        return executor;
    }

    public static void registerExecutor(ConfigurableApplicationContext context,
                                        Executor executor) {

        if (context.containsBean(BEAN_NAME)) {
            return;
        }

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(
                        ThreadPoolTaskExecutor.class, () -> (ThreadPoolTaskExecutor) executor
                );

        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

        if (beanFactory instanceof BeanDefinitionRegistry registry) {
            registry.registerBeanDefinition(BEAN_NAME, builder.getBeanDefinition());
        }
    }
}
