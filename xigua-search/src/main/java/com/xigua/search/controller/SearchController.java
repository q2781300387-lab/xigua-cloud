package com.xigua.search.controller;

import com.xigua.common.core.base.R;
import com.xigua.domain.dto.GlobalSearchChatMessageDTO;
import com.xigua.domain.dto.SearchChatMessageDTO;
import com.xigua.domain.vo.ChatMessageSearchResultVO;
import com.xigua.domain.vo.SearchPageVO;
import com.xigua.search.service.ChatMessageSearchService;
import com.xigua.search.service.SearchSyncServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName SearchController
 * @Description 搜索接口
 * @Author wangjinfei
 * @Date 2026/9/29
 */
@Tag(name = "搜索接口")
@RequestMapping("/chat/message")
@RestController
public class SearchController {
    @Autowired
    private ChatMessageSearchService chatMessageSearchService;

    @Autowired
    private SearchSyncServiceImpl searchSyncService;

    /**
     * 搜索历史聊天记录
     * @author wangjinfei
     * @date 2026/9/29
     * @param dto
     * @return R<SearchPageVO<ChatMessageSyncVO>>
     */
    @Operation(summary = "搜索历史聊天记录")
    @PostMapping("/search")
    public R<SearchPageVO<ChatMessageSearchResultVO>> search(@RequestBody SearchChatMessageDTO dto) {
        return R.ok(chatMessageSearchService.search(dto), "搜索成功");
    }

    /**
     * 全局搜索：跨当前用户的全部会话检索消息
     */
    @Operation(summary = "全局搜索聊天记录")
    @PostMapping("/global")
    public R<SearchPageVO<ChatMessageSearchResultVO>> searchGlobal(@RequestBody GlobalSearchChatMessageDTO dto) {
        return R.ok(chatMessageSearchService.searchGlobal(dto), "搜索成功");
    }

    /**
     * 查询聊天记录索引的水位线（ES 里最新一条消息的 createTime）
     * 索引为空时返回 null，此时对账会退化为全量同步
     * @author wangjinfei
     * @date 2026/11/12
     * @return R<String>
     */
    @Operation(summary = "查询聊天记录索引水位线")
    @GetMapping("/index/watermark")
    public R<String> indexWatermark() {
        return R.ok(searchSyncService.latestIndexedCreateTime(), "查询成功");
    }

    /**
     * 手动触发聊天记录增量对账（定时任务每 5 分钟自动跑一次，这个接口用于排障和验证）
     * @author wangjinfei
     * @date 2026/11/12
     * @param sinceTime 起始时间，可选；不传则从索引水位线开始
     * @return R<Long> 本次写入 ES 的条数
     */
    @Operation(summary = "手动触发聊天记录增量对账")
    @PostMapping("/index/reconcile")
    public R<Long> reconcile(@RequestParam(required = false) String sinceTime) {
        return R.ok(searchSyncService.reconcileChatMessage(sinceTime), "增量对账完成");
    }
}
