package com.xigua.client.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xigua.client.dto.VoteCreateDTO;
import com.xigua.client.dto.VoteSubmitDTO;
import com.xigua.client.entity.GroupVote;
import com.xigua.client.entity.GroupVoteRecord;
import com.xigua.client.mapper.GroupMemberMapper;
import com.xigua.client.mapper.GroupVoteMapper;
import com.xigua.client.mapper.GroupVoteRecordMapper;
import com.xigua.common.core.base.R;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.UserContext;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.entity.GroupMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName GroupVoteController
 * @Description 群投票：发起 / 查看结果 / 投票 / 结束
 * @Author xigua
 * @Date 2026/11/13 16:00
 */
@Slf4j
@Tag(name = "群投票接口")
@RequestMapping("/group/vote")
@RestController
public class GroupVoteController {

    @Autowired
    private GroupVoteMapper voteMapper;

    @Autowired
    private GroupVoteRecordMapper recordMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private Sequence sequence;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_OPTIONS = 10;
    private static final int MAX_OPTION_LENGTH = 30;
    private static final int MAX_TITLE_LENGTH = 60;

    @Operation(summary = "发起群投票")
    @PostMapping("/create")
    public R<Map<String, Object>> create(@RequestBody VoteCreateDTO dto) {
        if (dto == null || dto.getGroupId() == null) {
            return R.fail("群id不能为空");
        }
        String title = dto.getTitle() == null ? "" : dto.getTitle().trim();
        if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH) {
            return R.fail("投票标题需在 1-" + MAX_TITLE_LENGTH + " 字之间");
        }
        List<String> options = dto.getOptions() == null ? new ArrayList<>() : dto.getOptions();
        List<String> cleaned = options.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (cleaned.size() < 2 || cleaned.size() > MAX_OPTIONS) {
            return R.fail("选项数量需在 2-" + MAX_OPTIONS + " 个之间");
        }
        for (String opt : cleaned) {
            if (opt.length() > MAX_OPTION_LENGTH) {
                return R.fail("单个选项不能超过 " + MAX_OPTION_LENGTH + " 字");
            }
        }
        Long userId = currentUserId();
        if (!isGroupMember(dto.getGroupId(), userId)) {
            return R.fail("你不在该群聊中");
        }
        GroupVote vote = new GroupVote();
        vote.setId(sequence.nextNo());
        vote.setGroupId(dto.getGroupId());
        vote.setCreatorId(userId);
        vote.setTitle(title);
        try {
            vote.setOptions(objectMapper.writeValueAsString(cleaned));
        } catch (Exception e) {
            return R.fail("选项格式不正确");
        }
        vote.setMulti(dto.getMulti() != null && dto.getMulti() ? 1 : 0);
        vote.setStatus(0);
        voteMapper.insert(vote);
        return R.ok(buildDetail(vote, userId), "投票已发起");
    }

    @Operation(summary = "投票详情与结果")
    @GetMapping("/detail")
    public R<Map<String, Object>> detail(@RequestParam Long voteId) {
        GroupVote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            return R.fail("投票不存在或已删除");
        }
        Long userId = currentUserId();
        if (!isGroupMember(vote.getGroupId(), userId)) {
            return R.fail("你不在该群聊中");
        }
        return R.ok(buildDetail(vote, userId), "查询成功");
    }

    @Operation(summary = "提交投票")
    @PostMapping("/submit")
    public R<Map<String, Object>> submit(@RequestBody VoteSubmitDTO dto) {
        if (dto == null || dto.getVoteId() == null || dto.getOptionIndex() == null) {
            return R.fail("参数不完整");
        }
        GroupVote vote = voteMapper.selectById(dto.getVoteId());
        if (vote == null) {
            return R.fail("投票不存在或已删除");
        }
        if (vote.getStatus() != null && vote.getStatus() == 1) {
            return R.fail("投票已结束");
        }
        Long userId = currentUserId();
        if (!isGroupMember(vote.getGroupId(), userId)) {
            return R.fail("你不在该群聊中");
        }
        List<String> options = parseOptions(vote);
        int index = dto.getOptionIndex();
        if (index < 0 || index >= options.size()) {
            return R.fail("选项不存在");
        }
        boolean multi = vote.getMulti() != null && vote.getMulti() == 1;
        List<GroupVoteRecord> mine = recordMapper.selectList(new LambdaQueryWrapper<GroupVoteRecord>()
                .eq(GroupVoteRecord::getVoteId, dto.getVoteId())
                .eq(GroupVoteRecord::getUserId, userId));
        Optional<GroupVoteRecord> same = mine.stream().filter(r -> r.getOptionIndex() == index).findFirst();
        if (multi) {
            if (same.isPresent()) {
                recordMapper.delete(new LambdaQueryWrapper<GroupVoteRecord>()
                        .eq(GroupVoteRecord::getVoteId, dto.getVoteId())
                        .eq(GroupVoteRecord::getUserId, userId)
                        .eq(GroupVoteRecord::getOptionIndex, index));
            } else {
                safeInsert(vote.getId(), userId, index);
            }
        } else {
            // 单选：先清空本人在该投票下的全部旧记录，再按需写入，避免重复记录累积
            recordMapper.delete(new LambdaQueryWrapper<GroupVoteRecord>()
                    .eq(GroupVoteRecord::getVoteId, dto.getVoteId())
                    .eq(GroupVoteRecord::getUserId, userId));
            if (!same.isPresent()) {
                safeInsert(vote.getId(), userId, index);
            }
        }
        return R.ok(buildDetail(vote, userId), "投票成功");
    }

    @Operation(summary = "取消投票（清空本人全部选择）")
    @PostMapping("/cancel")
    public R<Map<String, Object>> cancel(@RequestBody VoteSubmitDTO dto) {
        if (dto == null || dto.getVoteId() == null) {
            return R.fail("投票id不能为空");
        }
        GroupVote vote = voteMapper.selectById(dto.getVoteId());
        if (vote == null) {
            return R.fail("投票不存在或已删除");
        }
        if (vote.getStatus() != null && vote.getStatus() == 1) {
            return R.fail("投票已结束");
        }
        Long userId = currentUserId();
        if (!isGroupMember(vote.getGroupId(), userId)) {
            return R.fail("你不在该群聊中");
        }
        recordMapper.delete(new LambdaQueryWrapper<GroupVoteRecord>()
                .eq(GroupVoteRecord::getVoteId, dto.getVoteId())
                .eq(GroupVoteRecord::getUserId, userId));
        return R.ok(buildDetail(vote, userId), "已取消投票");
    }

    @Operation(summary = "结束投票（仅发起人）")
    @PostMapping("/close")
    public R<Boolean> close(@RequestBody VoteSubmitDTO dto) {
        if (dto == null || dto.getVoteId() == null) {
            return R.fail("投票id不能为空");
        }
        GroupVote vote = voteMapper.selectById(dto.getVoteId());
        if (vote == null) {
            return R.fail("投票不存在或已删除");
        }
        if (!Objects.equals(vote.getCreatorId(), currentUserId())) {
            return R.fail("只有发起人可以结束投票");
        }
        vote.setStatus(1);
        voteMapper.updateById(vote);
        return R.ok(true, "投票已结束");
    }

    /**
     * 唯一键 uk_vote_user_option 兜底：并发重复提交只保留一条，重复插入视为幂等成功
     */
    private void safeInsert(String voteId, Long userId, int index) {
        try {
            recordMapper.insert(newRecord(voteId, userId, index));
        } catch (DuplicateKeyException e) {
            log.info("忽略重复投票记录 voteId={} userId={} option={}", voteId, userId, index);
        }
    }

    private GroupVoteRecord newRecord(String voteId, Long userId, int index) {
        GroupVoteRecord record = new GroupVoteRecord();
        record.setId(sequence.nextNo());
        record.setVoteId(Long.valueOf(voteId));
        record.setUserId(userId);
        record.setOptionIndex(index);
        return record;
    }

    private Map<String, Object> buildDetail(GroupVote vote, Long userId) {
        List<String> options = parseOptions(vote);
        List<GroupVoteRecord> records = recordMapper.selectList(new LambdaQueryWrapper<GroupVoteRecord>()
                .eq(GroupVoteRecord::getVoteId, vote.getId()));
        Map<Integer, Set<Long>> votersByOption = new HashMap<>();
        Set<Long> allVoters = new HashSet<>();
        for (GroupVoteRecord r : records) {
            votersByOption.computeIfAbsent(r.getOptionIndex(), k -> new HashSet<>()).add(r.getUserId());
            allVoters.add(r.getUserId());
        }
        Set<Integer> myOptions = records.stream()
                .filter(r -> Objects.equals(r.getUserId(), userId))
                .map(GroupVoteRecord::getOptionIndex)
                .collect(Collectors.toSet());
        int total = allVoters.size();
        List<Map<String, Object>> optionList = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            int count = votersByOption.getOrDefault(i, Collections.emptySet()).size();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", i);
            item.put("text", options.get(i));
            item.put("count", count);
            item.put("ratio", total == 0 ? 0 : (int) Math.round(count * 100.0 / total));
            optionList.add(item);
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("voteId", vote.getId());
        detail.put("groupId", vote.getGroupId());
        detail.put("title", vote.getTitle());
        detail.put("multi", vote.getMulti() != null && vote.getMulti() == 1);
        detail.put("status", vote.getStatus() == null ? 0 : vote.getStatus());
        detail.put("creatorId", vote.getCreatorId());
        detail.put("total", total);
        detail.put("options", optionList);
        detail.put("myOptions", new ArrayList<>(myOptions));
        return detail;
    }

    private List<String> parseOptions(GroupVote vote) {
        try {
            return objectMapper.readValue(vote.getOptions(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("解析投票选项失败：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean isGroupMember(Long groupId, Long userId) {
        long count = groupMemberMapper.selectCount(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, String.valueOf(groupId))
                .eq(GroupMember::getUserId, String.valueOf(userId))
                .eq(GroupMember::getStatus, 1)
                .eq(GroupMember::getDelFlag, 0));
        return count > 0;
    }

    private Long currentUserId() {
        if (UserContext.get() == null || StringUtils.isEmpty(UserContext.get().getUserId())) {
            throw new BusinessException("请先登录");
        }
        try {
            return Long.valueOf(UserContext.get().getUserId());
        } catch (NumberFormatException e) {
            throw new BusinessException("登录用户信息异常");
        }
    }
}
