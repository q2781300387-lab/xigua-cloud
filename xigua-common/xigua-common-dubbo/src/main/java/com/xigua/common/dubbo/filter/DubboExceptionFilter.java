package com.xigua.common.dubbo.filter;

import com.xigua.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.filter.ExceptionFilter;
import org.apache.dubbo.rpc.service.GenericService;

/**
 * @ClassName DubboExceptionFilter
 * @Description dubbo异常处理过滤器
 * @Author wangjinfei
 * @Date 2025/4/27 22:28
 */
@Slf4j
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER})
public class DubboExceptionFilter extends ExceptionFilter {
    /*
    * 如果代码抛出自定义异常，会被dubbo默认ExecuteLimitFilter处理，又会把异常包装成RuntimeException再抛出
    * 导致ExceptionHandle会捕捉到Exception，而不是自定义异常
    * 所以需要自定义异常过滤器，判断是否是自定义异常，如果是，直接抛出，否则使用dubbo的默认异常处理
    */

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        // 没异常直接返回
        if (!appResponse.hasException() || GenericService.class == invoker.getInterface()) {
            return;
        }

        Throwable exception = appResponse.getException();
        boolean isConsumer = RpcContext.getContext().isConsumerSide();
        boolean isProvider = RpcContext.getContext().isProviderSide();

        // 生产者逻辑：打印完整堆栈，保证异常可序列化
        if (isProvider) {
            log.error("---->>>>>[dubbo-provider] 业务异常: interface={} method={}",
                    invoker.getInterface().getName(),
                    invocation.getMethodName(),
                    exception);

            // 设置响应异常为 RpcException 类型，避免序列化失败
            appResponse.setException(new RpcException("业务异常: " +exception.getClass().getName() + ": " + exception.getMessage()));
            return;
        }

        // 消费者逻辑：打印日志，包装成 RpcException 抛出
        if (isConsumer) {
            log.error("---->>>>>[dubbo-consumer] 调用远程服务异常: interface={} method={}",
                    invoker.getInterface().getName(),
                    invocation.getMethodName(),
                    exception);

            // 设置异常为null，抛出异常
            appResponse.setException(null);
            throw new RpcException("远程服务调用失败：" + exception.getMessage(), exception);
        }
    }
}
