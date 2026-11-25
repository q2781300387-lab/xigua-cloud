package com.xigua.common.dubbo.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Dubbo 工具类
 * 用于指定节点 RPC 调用和节点状态判断
 *
 * @author Claude
 * @date 2026/03/20
 */
@Slf4j
public class DubboUtil {

    /**
     * 按节点缓存 Dubbo 引用，避免每次推送都新建 ReferenceConfig。
     * 冷启动时新建引用会阻塞在注册中心服务发现上，呼叫/消息推送会表现为长时间无响应。
     */
    private static final ConcurrentHashMap<String, ReferenceConfig<?>> REFERENCE_CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static <T> T getReference(Class<T> serviceClass, String ip, int port) {
        String key = serviceClass.getName() + "@" + ip + ":" + port;
        ReferenceConfig<T> referenceConfig = (ReferenceConfig<T>) REFERENCE_CACHE.computeIfAbsent(key, k -> {
            ReferenceConfig<T> config = new ReferenceConfig<>();
            config.setInterface(serviceClass);
            // 直连目标节点，完全绕过注册中心，避免冷启动时在请求线程里等待服务发现
            config.setApplication(new ApplicationConfig("xigua-center-push"));
            config.setRegistry(new RegistryConfig("N/A"));
            config.setUrl("dubbo://" + ip + ":" + port);
            config.setCheck(false);
            config.setTimeout(3000);
            config.setRetries(0);
            return config;
        });
        return referenceConfig.get();
    }

    /**
     * 通过指定节点调用 Dubbo 服务（无返回值）
     *
     * @param serviceClass 服务接口类
     * @param ip           节点 IP 地址
     * @param port         Dubbo 端口
     * @param action       要执行的操作
     * @param <T>          服务接口类型
     */
    public static <T> void invoke(Class<T> serviceClass, String ip, int port, Consumer<T> action) {
        try {
            T service = getReference(serviceClass, ip, port);
            action.accept(service);
            log.debug("通过节点 {}:{} 调用服务 {} 成功", ip, port, serviceClass.getSimpleName());
        } catch (Exception e) {
            log.error("通过节点 {}:{} 调用服务 {} 失败", ip, port, serviceClass.getSimpleName(), e);
            throw new RuntimeException("调用指定节点服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过指定节点调用 Dubbo 服务（有返回值）
     *
     * @param serviceClass 服务接口类
     * @param ip           节点 IP 地址
     * @param port         Dubbo 端口
     * @param function     要执行的函数
     * @param <T>          服务接口类型
     * @param <R>          返回值类型
     * @return 函数执行结果
     */
    public static <T, R> R invoke(Class<T> serviceClass, String ip, int port, Function<T, R> function) {
        try {
            T service = getReference(serviceClass, ip, port);
            R result = function.apply(service);
            log.debug("通过节点 {}:{} 调用服务 {} 成功", ip, port, serviceClass.getSimpleName());
            return result;
        } catch (Exception e) {
            log.error("通过节点 {}:{} 调用服务 {} 失败", ip, port, serviceClass.getSimpleName(), e);
            throw new RuntimeException("调用指定节点服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查指定节点服务是否可用
     *
     * @param serviceClass 服务接口类
     * @param ip           节点 IP 地址
     * @param port         Dubbo 端口
     * @param <T>          服务接口类型
     * @return true: 可用; false: 不可用
     */
    public static <T> boolean isNodeAvailable(Class<T> serviceClass, String ip, int port) {
        try {
            return getReference(serviceClass, ip, port) != null;
        } catch (Exception e) {
            log.debug("节点 {}:{} 服务 {} 不可用: {}", ip, port, serviceClass.getSimpleName(), e.getMessage());
            return false;
        }
    }
}
