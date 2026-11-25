package com.xigua.search.job;

import com.xigua.search.service.SearchSyncServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @ClassName ChatMessageIndexReconcileJob
 * @Description 聊天记录索引增量对账任务。
 *              <p>消息发送时的写 ES 是异步、且失败只打日志的旁路流程：xigua-search 没起、
 *              ES 不可用时，这条消息就永久不在索引里，表现为「新发的消息搜不到」。
 *              这里按固定间隔用 ES 里最新一条消息的时间做水位线，把漏掉的补回去，
 *              顺带把上一轮对账期间新进来的消息收进来。</p>
 * @Author wangjinfei
 * @Date 2026/11/12
 */
@Slf4j
@Component
public class ChatMessageIndexReconcileJob {

    /**
     * 防止上一轮没跑完又起一轮，重复扫表
     */
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    @Autowired
    private SearchSyncServiceImpl searchSyncService;

    /**
     * 聊天记录索引增量对账
     * 首次延迟默认 60 秒（等服务注册到 nacos），之后默认每 5 分钟一次
     * @author wangjinfei
     * @date 2026/11/12
     */
    @Scheduled(
            initialDelayString = "${xigua.search.reconcile-initial-delay-ms:60000}",
            fixedDelayString = "${xigua.search.reconcile-interval-ms:300000}"
    )
    public void reconcile() {
        if (!RUNNING.compareAndSet(false, true)) {
            log.info("上一轮聊天记录增量对账还在执行，跳过本轮");
            return;
        }
        long start = System.currentTimeMillis();
        try {
            Long written = searchSyncService.reconcileChatMessage(null);
            log.info("聊天记录增量对账任务结束，写入 {} 条，耗时 {} ms", written, System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 对账失败不影响搜索服务对外提供检索，下一轮继续
            log.error("聊天记录增量对账任务失败，耗时 {} ms", System.currentTimeMillis() - start, e);
        } finally {
            RUNNING.set(false);
        }
    }
}
