package com.xigua.client.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigua.api.service.ContactService;
import com.xigua.client.entity.GroupFile;
import com.xigua.client.mapper.GroupFileMapper;
import com.xigua.common.core.base.R;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.core.util.UserContext;
import com.xigua.domain.vo.FriendVO;
import com.xigua.domain.vo.GlobalSearchResultVO;
import com.xigua.domain.vo.GroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 全局搜索：联系人 / 已加入群 / 群文件。
 * 消息检索由 xigua-search 的 /chat/message/global 负责，前端并行请求并按分区展示。
 */
@Tag(name = "全局搜索")
@RequestMapping("/search")
@RestController
public class GlobalSearchController {

    private static final int CONTACT_LIMIT = 20;
    private static final int GROUP_LIMIT = 20;
    private static final int FILE_LIMIT = 30;

    @Autowired
    private ContactService contactService;

    @Autowired
    private GroupFileMapper groupFileMapper;

    @Operation(summary = "搜索联系人、群与群文件")
    @GetMapping("/all")
    public R<GlobalSearchResultVO> searchAll(@RequestParam String keyword,
                                             @RequestParam(required = false) Integer limit) {
        String kw = StringUtils.trimToEmpty(keyword);
        if (StringUtils.isBlank(kw)) {
            return R.ok(new GlobalSearchResultVO(), "请输入关键词");
        }
        currentUserId();
        int max = limit == null || limit < 1 ? 20 : Math.min(limit, 50);
        GlobalSearchResultVO result = new GlobalSearchResultVO();
        String lower = kw.toLowerCase(Locale.ROOT);

        List<FriendVO> friends = safeList(contactService.getFriendList());
        for (FriendVO friend : friends) {
            if (result.getContacts().size() >= Math.min(max, CONTACT_LIMIT)) {
                break;
            }
            if (!matches(friend.getUsername(), lower)) {
                continue;
            }
            GlobalSearchResultVO.ContactItem item = new GlobalSearchResultVO.ContactItem();
            item.setId(StringUtils.defaultIfBlank(friend.getFriendId(), friend.getUserId()));
            item.setName(friend.getUsername());
            item.setAvatar(friend.getAvatar());
            item.setSignature(friend.getSignature());
            item.setOnline(friend.getIsOnline());
            result.getContacts().add(item);
        }

        List<GroupVO> groups = safeList(contactService.getGroupList());
        Map<String, String> groupNames = new HashMap<>();
        for (GroupVO group : groups) {
            groupNames.put(group.getGroupId(), group.getGroupName());
        }
        for (GroupVO group : groups) {
            if (result.getGroups().size() >= Math.min(max, GROUP_LIMIT)) {
                break;
            }
            if (!matches(group.getGroupName(), lower)) {
                continue;
            }
            GlobalSearchResultVO.GroupItem item = new GlobalSearchResultVO.GroupItem();
            item.setId(group.getGroupId());
            item.setName(group.getGroupName());
            item.setAvatar(group.getGroupAvatar());
            item.setMemberCount(group.getCurrentMember());
            result.getGroups().add(item);
        }

        List<String> groupIds = new ArrayList<>(groupNames.keySet());
        if (!groupIds.isEmpty()) {
            LambdaQueryWrapper<GroupFile> wrapper = new LambdaQueryWrapper<GroupFile>()
                    .in(GroupFile::getGroupId, groupIds)
                    .like(GroupFile::getFileName, kw)
                    .orderByDesc(GroupFile::getCreateTime)
                    .last("limit " + Math.min(max, FILE_LIMIT));
            for (GroupFile file : groupFileMapper.selectList(wrapper)) {
                GlobalSearchResultVO.FileItem item = new GlobalSearchResultVO.FileItem();
                item.setId(String.valueOf(file.getId()));
                item.setGroupId(file.getGroupId() == null ? null : String.valueOf(file.getGroupId()));
                item.setGroupName(groupNames.get(String.valueOf(file.getGroupId())));
                item.setName(file.getFileName());
                item.setUrl(file.getFileUrl());
                item.setSize(file.getFileSize());
                item.setExt(file.getFileExt());
                item.setUploaderName(file.getUploaderName());
                item.setCreateTime(file.getCreateTime());
                result.getFiles().add(item);
            }
        }

        return R.ok(result, "搜索成功");
    }

    private boolean matches(String text, String lowerKeyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private String currentUserId() {
        if (UserContext.get() == null || StringUtils.isEmpty(UserContext.get().getUserId())) {
            throw new BusinessException("请先登录");
        }
        return UserContext.get().getUserId();
    }
}
