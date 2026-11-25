package com.xigua.client.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xigua.client.dto.TreeHoleAddDTO;
import com.xigua.client.dto.TreeHoleLikeDTO;
import com.xigua.client.entity.TreeHole;
import com.xigua.client.mapper.TreeHoleMapper;
import com.xigua.client.vo.TreeHoleVO;
import com.xigua.common.core.base.R;
import com.xigua.common.sequence.sequence.Sequence;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * @ClassName TreeHoleController
 * @Description 匿名树洞：匿名发布 / 匿名浏览 / 点赞，不暴露发布者身份
 * @Author xigua
 * @Date 2026/10/16 19:30
 */
@Slf4j
@Tag(name = "匿名树洞接口")
@RequestMapping("/treehole")
@RestController
public class TreeHoleController {

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    @Autowired
    private Sequence sequence;

    /**
     * 树洞列表（按时间倒序，匿名展示，不返回发布者）
     */
    @Operation(summary = "树洞列表（匿名）")
    @GetMapping("/list")
    public R<Page<TreeHoleVO>> list(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "20") long size) {
        Page<TreeHole> p = treeHoleMapper.selectPage(new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<TreeHole>().orderByDesc(TreeHole::getCreateTime));
        Page<TreeHoleVO> vo = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        vo.setRecords(p.getRecords().stream().map(TreeHoleVO::from).collect(Collectors.toList()));
        return R.ok(vo);
    }

    /**
     * 发布树洞（内容 1-500 字）
     */
    @Operation(summary = "发布树洞（匿名）")
    @PostMapping("/add")
    public R<Boolean> add(@RequestBody TreeHoleAddDTO dto) {
        String content = dto == null || dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty() || content.length() > 500) {
            return R.fail("树洞内容需在 1-500 字之间");
        }
        TreeHole th = new TreeHole();
        th.setId(sequence.nextNo());
        th.setContent(content);
        th.setLikeCount(0);
        treeHoleMapper.insert(th);
        return R.ok(true, "树洞发布成功");
    }

    /**
     * 树洞点赞
     */
    @Operation(summary = "树洞点赞")
    @PostMapping("/like")
    public R<Boolean> like(@RequestBody TreeHoleLikeDTO dto) {
        if (dto == null || dto.getId() == null) {
            return R.fail("树洞id不能为空");
        }
        TreeHole th = treeHoleMapper.selectById(dto.getId());
        if (th == null) {
            return R.fail("树洞不存在");
        }
        th.setLikeCount((th.getLikeCount() == null ? 0 : th.getLikeCount()) + 1);
        treeHoleMapper.updateById(th);
        return R.ok(true, "点赞成功");
    }
}
