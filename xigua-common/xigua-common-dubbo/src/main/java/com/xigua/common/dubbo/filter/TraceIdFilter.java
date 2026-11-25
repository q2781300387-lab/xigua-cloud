package com.xigua.common.dubbo.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

/**
 * @ClassName TraceIdFilter
 * @Description TODO
 * @Author wangjinfei
 * @Date 2025/4/5 10:10
 */
@Slf4j
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER})
public class TraceIdFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcContext rpcContext = RpcContext.getContext();
        URL url = rpcContext.getUrl();
        String traceId;

        if(url == null){
            return invoker.invoke(invocation);
        }

        if (rpcContext.isConsumerSide()) {
            traceId = MDC.get("traceId");

            if (StringUtils.isNotEmpty(traceId)) {
                rpcContext.setAttachment("traceId", traceId);
            }
            return invoker.invoke(invocation);
        }

        if (rpcContext.isProviderSide()) {
            traceId = rpcContext.getAttachment("traceId");
            if(StringUtils.isNotEmpty(traceId)){
                MDC.put("traceId", traceId);
            }
            try{
                return invoker.invoke(invocation);
            } catch (Exception e) {
                log.error("---->>>>>[dubbo] 服务调用异常：", e);
                throw e;
            } finally {
                MDC.remove("traceId");
            }
        }

        return invoker.invoke(invocation);
    }
}
