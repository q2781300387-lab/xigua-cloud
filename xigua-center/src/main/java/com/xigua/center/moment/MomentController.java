package com.xigua.center.moment;

import com.xigua.common.core.base.R;
import com.xigua.domain.dto.MomentDTO;
import com.xigua.domain.vo.MomentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 朋友圈接口：发布 / 列表 / 点赞 / 评论 / 删除
 */
@Tag(name = "朋友圈")
@RestController
@RequestMapping("/moment")
public class MomentController {

    @Autowired
    private MomentService momentService;

    @Operation(summary = "发布动态")
    @PostMapping("/publish")
    public R<MomentVO> publish(@RequestBody MomentDTO dto) {
        try {
            return R.ok(momentService.publish(dto), "发布成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "朋友圈列表（自己 + 好友）")
    @PostMapping("/list")
    public R<List<MomentVO>> list(@RequestBody MomentDTO dto) {
        try {
            return R.ok(momentService.list(dto), "查询成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "点赞 / 取消点赞")
    @PostMapping("/like")
    public R<MomentVO> like(@RequestBody MomentDTO dto) {
        try {
            return R.ok(momentService.toggleLike(dto.getUserId(), dto.getMomentId()), "操作成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "评论")
    @PostMapping("/comment")
    public R<MomentVO> comment(@RequestBody MomentDTO dto) {
        try {
            return R.ok(momentService.addComment(dto.getUserId(), dto.getMomentId(), dto.getContent()), "评论成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "删除动态")
    @PostMapping("/delete")
    public R<Boolean> delete(@RequestBody MomentDTO dto) {
        try {
            return R.ok(momentService.delete(dto.getUserId(), dto.getMomentId()), "已删除");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "删除评论")
    @PostMapping("/comment/delete")
    public R<MomentVO> deleteComment(@RequestBody MomentDTO dto) {
        try {
            return R.ok(momentService.deleteComment(dto.getUserId(), dto.getCommentId()), "已删除");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "未读互动（别人给我的点赞/评论）")
    @PostMapping("/interact-unread")
    public R<Map<String, Object>> interactUnread(@RequestBody MomentDTO dto) {
        try {
            return R.ok(momentService.interactUnread(dto.getUserId(), dto.getSinceTime()));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "动态详情")
    @PostMapping("/detail")
    public R<MomentVO> detail(@RequestBody MomentDTO dto) {
        try {
            return R.ok(momentService.detail(dto.getMomentId(), dto.getUserId()));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
