package com.xigua.common.core.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @ClassName ThreadMgr
 * @Description
 * @Author wangjinfei
 * @Date 2026/1/15 10:54
 */
@Slf4j
public final class ThreadMgr {
    private static final int CORE_THREAD_SIZE = Runtime.getRuntime().availableProcessors() + 1;
    private static final int MAX_THREAD_SIZE = Runtime.getRuntime().availableProcessors() * 2 + 1;
    private static final int WORK_QUEUE = 1000;
    private static final int KEEP_ALIVE_SECONDS = 60;
    private static final String THREAD_NAME_PRE = "xg-thread-";

    private static volatile Executor DEFAULT_EXECUTOR;
    private static boolean isSpringEnv = false;
    private static volatile Executor springExecutor;

    static {
        boolean springOnClasspath;
        try {
            Class.forName("org.springframework.context.ApplicationContext");
            springOnClasspath = true;
        } catch (ClassNotFoundException e) {
            springOnClasspath = false;
        }

        if (springOnClasspath) {
            log.info("ThreadMgr 启动环境检测：classpath 中检测到 Spring 相关（尚未确认是否已启动 Spring 容器）");
        } else {
            log.info("ThreadMgr 启动环境检测：原生 JDK 环境");
        }
    }

    private ThreadMgr() {
    }

    public static int coreThreadSize() {
        return CORE_THREAD_SIZE;
    }

    public static int maxThreadSize() {
        return MAX_THREAD_SIZE;
    }

    public static int workQueueSize() {
        return WORK_QUEUE;
    }

    public static int keepAliveSeconds() {
        return KEEP_ALIVE_SECONDS;
    }

    public static String threadNamePrefix() {
        return THREAD_NAME_PRE;
    }

    public static void markSpringEnv(Executor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("spring executor must not be null");
        }
        springExecutor = executor;
        isSpringEnv = true;
    }

    public static String runtimeEnv() {
        return isSpringEnv ? "Spring 环境" : "原生 JDK 环境";
    }


    /**
     * 获取默认线程池（双检锁）
     */
    public static Executor getExecutor() {
        if (DEFAULT_EXECUTOR == null) {
            synchronized (ThreadMgr.class) {
                if (DEFAULT_EXECUTOR == null) {
                    DEFAULT_EXECUTOR = buildDefaultExecutor();
                }
            }
        }
        return DEFAULT_EXECUTOR;
    }

    /**
     * 构建默认线程池
     */
    private static Executor buildDefaultExecutor() {
        return isSpringEnv ? springExecutor : buildJdkThreadPoolExecutor();
    }

    /**
     * 构建JDK默认线程池（原生Java环境）
     */
    private static Executor buildJdkThreadPoolExecutor() {
        log.info("构建JDK默认线程池");

        return new ThreadPoolExecutor(
                CORE_THREAD_SIZE,
                MAX_THREAD_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(WORK_QUEUE),
                new JDKThreadFactory(THREAD_NAME_PRE),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public static class JDKThreadFactory implements ThreadFactory {
        private final String namePrefix;

        public JDKThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    }
}
