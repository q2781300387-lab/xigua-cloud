package com.xigua.center.moment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigua.center.mapper.MomentCommentMapper;
import com.xigua.center.mapper.MomentLikeMapper;
import com.xigua.center.mapper.MomentMapper;
import com.xigua.common.core.exception.BusinessException;
import com.xigua.common.sequence.sequence.Sequence;
import com.xigua.domain.dto.MomentDTO;
import com.xigua.domain.entity.Moment;
import com.xigua.domain.entity.MomentComment;
import com.xigua.domain.entity.MomentLike;
import com.xigua.domain.vo.MomentCommentVO;
import com.xigua.domain.vo.MomentUserVO;
import com.xigua.domain.vo.MomentVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 朋友圈：动态落库、好友可见范围、点赞与评论。
 * 数据全部来自 xg_moment / xg_moment_like / xg_moment_comment，不再使用前端演示数据。
 */
@Slf4j
@Service
public class MomentService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String IMAGE_SEPARATOR = "|";

    @Autowired
    private MomentMapper momentMapper;

    @Autowired
    private MomentLikeMapper momentLikeMapper;

    @Autowired
    private MomentCommentMapper momentCommentMapper;

    @Autowired
    private Sequence sequence;

    /**
     * 发布一条动态
     */
    public MomentVO publish(MomentDTO dto) {
        String userId = dto.getUserId();
        if (StringUtils.isBlank(userId)) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        List<String> images = dto.getImages() == null ? Collections.emptyList() : dto.getImages();
        if (content.isEmpty() && images.isEmpty()) {
            throw new BusinessException("正文和图片不能都为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException("正文不能超过 2000 字");
        }
        if (images.size() > 9) {
            throw new BusinessException("最多只能发 9 张图片");
        }

        Moment moment = new Moment();
        moment.setId(sequence.nextNo());
        moment.setUserId(userId);
        moment.setContent(content);
        moment.setImages(joinImages(images));
        moment.setLocation(dto.getLocation() == null ? "" : dto.getLocation().trim());
        moment.setLikeCount(0);
        moment.setCommentCount(0);
        moment.setDelFlag(0);
        moment.setCreateTime(LocalDateTime.now());
        moment.setUpdateTime(LocalDateTime.now());
        momentMapper.insert(moment);
        log.info("朋友圈发布成功 id={} userId={} images={}", moment.getId(), userId, images.size());
        return toVO(moment, userId);
    }

    /**
     * 朋友圈列表：全站公开，按时间倒序
     */
    public List<MomentVO> list(MomentDTO dto) {
        String userId = dto.getUserId();
        if (StringUtils.isBlank(userId)) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null ? 20 : Math.min(Math.max(dto.getPageSize(), 1), 50);

        LambdaQueryWrapper<Moment> query = new LambdaQueryWrapper<>();
        query.eq(Moment::getDelFlag, 0)
                .orderByDesc(Moment::getCreateTime)
                .orderByDesc(Moment::getId)
                .last("limit " + ((pageNum - 1) * pageSize) + "," + pageSize);

        List<Moment> rows = momentMapper.selectList(query);
        List<MomentVO> result = new ArrayList<>();
        for (Moment row : rows) {
            result.add(toVO(row, userId));
        }
        return result;
    }

    /**
     * 单条动态（点赞 / 评论后返回最新状态）
     */
    public MomentVO detail(String momentId, String viewerId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null || (moment.getDelFlag() != null && moment.getDelFlag() == 1)) {
            throw new BusinessException("动态不存在或已删除");
        }
        return toVO(moment, viewerId);
    }

    /**
     * 点赞 / 取消点赞
     */
    public MomentVO toggleLike(String userId, String momentId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(momentId)) {
            throw new BusinessException("参数不完整");
        }
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null || (moment.getDelFlag() != null && moment.getDelFlag() == 1)) {
            throw new BusinessException("动态不存在或已删除");
        }

        LambdaQueryWrapper<MomentLike> query = new LambdaQueryWrapper<>();
        query.eq(MomentLike::getMomentId, momentId).eq(MomentLike::getUserId, userId);
        MomentLike exist = momentLikeMapper.selectOne(query);
        if (exist != null) {
            momentLikeMapper.deleteById(exist.getId());
        } else {
            MomentLike like = new MomentLike();
            like.setId(sequence.nextNo());
            like.setMomentId(momentId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            momentLikeMapper.insert(like);
        }
        // 同步冗余计数，便于直接用 SQL 核对
        int likeCount = Math.toIntExact(momentLikeMapper.selectCount(
                new LambdaQueryWrapper<MomentLike>().eq(MomentLike::getMomentId, momentId)));
        moment.setLikeCount(likeCount);
        moment.setUpdateTime(LocalDateTime.now());
        momentMapper.updateById(moment);
        return toVO(moment, userId);
    }

    /**
     * 发表评论
     */
    public MomentVO addComment(String userId, String momentId, String content) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(momentId)) {
            throw new BusinessException("参数不完整");
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (text.length() > 500) {
            throw new BusinessException("评论不能超过 500 字");
        }
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null || (moment.getDelFlag() != null && moment.getDelFlag() == 1)) {
            throw new BusinessException("动态不存在或已删除");
        }

        MomentComment comment = new MomentComment();
        comment.setId(sequence.nextNo());
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setContent(text);
        comment.setDelFlag(0);
        comment.setCreateTime(LocalDateTime.now());
        momentCommentMapper.insert(comment);
        // 同步冗余计数
        int commentCount = Math.toIntExact(momentCommentMapper.selectCount(
                new LambdaQueryWrapper<MomentComment>()
                        .eq(MomentComment::getMomentId, momentId)
                        .eq(MomentComment::getDelFlag, 0)));
        moment.setCommentCount(commentCount);
        moment.setUpdateTime(LocalDateTime.now());
        momentMapper.updateById(moment);
        return toVO(moment, userId);
    }

    /**
     * 删除自己的动态
     */
    public boolean delete(String userId, String momentId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(momentId)) {
            throw new BusinessException("参数不完整");
        }
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null) {
            throw new BusinessException("动态不存在");
        }
        if (!userId.equals(moment.getUserId())) {
            throw new BusinessException("只能删除自己发布的动态");
        }
        moment.setDelFlag(1);
        moment.setUpdateTime(LocalDateTime.now());
        return momentMapper.updateById(moment) > 0;
    }

    /**
     * 删除自己发的评论（软删，并同步动态的评论计数）
     */
    public MomentVO deleteComment(String userId, String commentId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(commentId)) {
            throw new BusinessException("参数不完整");
        }
        MomentComment comment = momentCommentMapper.selectById(commentId);
        if (comment == null || (comment.getDelFlag() != null && comment.getDelFlag() == 1)) {
            throw new BusinessException("评论不存在或已删除");
        }
        if (!userId.equals(comment.getUserId())) {
            throw new BusinessException("只能删除自己发的评论");
        }
        comment.setDelFlag(1);
        momentCommentMapper.updateById(comment);

        Moment moment = momentMapper.selectById(comment.getMomentId());
        if (moment == null) {
            throw new BusinessException("动态不存在或已删除");
        }
        int commentCount = Math.toIntExact(momentCommentMapper.selectCount(
                new LambdaQueryWrapper<MomentComment>()
                        .eq(MomentComment::getMomentId, comment.getMomentId())
                        .eq(MomentComment::getDelFlag, 0)));
        moment.setCommentCount(commentCount);
        moment.setUpdateTime(LocalDateTime.now());
        momentMapper.updateById(moment);
        return toVO(moment, userId);
    }

    /**
     * 未读互动：别人对我发布的动态产生的点赞 / 评论（晚于 sinceTime）。
     * 自己给自己的点赞、自己发的评论一律不计入。
     */
    public Map<String, Object> interactUnread(String userId, String sinceTime) {
        long likeCount = 0L;
        long commentCount = 0L;
        if (StringUtils.isNotBlank(userId)) {
            List<Moment> mine = momentMapper.selectList(new LambdaQueryWrapper<Moment>()
                    .eq(Moment::getUserId, userId)
                    .eq(Moment::getDelFlag, 0));
            if (!mine.isEmpty()) {
                List<String> ids = mine.stream().map(Moment::getId).collect(Collectors.toList());
                LocalDateTime since = LocalDateTime.now().minusDays(30);
                if (StringUtils.isNotBlank(sinceTime)) {
                    try {
                        since = LocalDateTime.parse(sinceTime.trim(), FMT);
                    } catch (Exception ignore) {
                        // 时间格式异常时回落到近 30 天，避免整个红点接口失败
                    }
                }
                likeCount = momentLikeMapper.selectCount(new LambdaQueryWrapper<MomentLike>()
                        .in(MomentLike::getMomentId, ids)
                        .ne(MomentLike::getUserId, userId)
                        .gt(MomentLike::getCreateTime, since));
                commentCount = momentCommentMapper.selectCount(new LambdaQueryWrapper<MomentComment>()
                        .in(MomentComment::getMomentId, ids)
                        .eq(MomentComment::getDelFlag, 0)
                        .ne(MomentComment::getUserId, userId)
                        .gt(MomentComment::getCreateTime, since));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("likeCount", likeCount);
        result.put("commentCount", commentCount);
        result.put("total", likeCount + commentCount);
        return result;
    }

    /**
     * 实体 → 展示对象（补齐发布人、点赞、评论）
     */
    private MomentVO toVO(Moment moment, String viewerId) {
        MomentVO vo = new MomentVO();
        vo.setId(moment.getId());
        vo.setUserId(moment.getUserId());

        MomentUserVO owner = momentMapper.selectUserBrief(moment.getUserId());
        vo.setNickname(owner != null && StringUtils.isNotBlank(owner.getUsername())
                ? owner.getUsername() : "用户" + tail(moment.getUserId()));
        vo.setAvatar(owner == null ? null : owner.getAvatar());

        vo.setContent(moment.getContent() == null ? "" : moment.getContent());
        vo.setImages(splitImages(moment.getImages()));
        vo.setLocation(moment.getLocation() == null ? "" : moment.getLocation());
        vo.setCreateTime(moment.getCreateTime() == null ? "" : moment.getCreateTime().format(FMT));

        List<String> likeNames = new ArrayList<>();
        boolean liked = false;
        LambdaQueryWrapper<MomentLike> likeQuery = new LambdaQueryWrapper<>();
        likeQuery.eq(MomentLike::getMomentId, moment.getId()).orderByAsc(MomentLike::getCreateTime);
        List<MomentLike> likes = momentLikeMapper.selectList(likeQuery);
        for (MomentLike like : likes) {
            if (StringUtils.equals(viewerId, like.getUserId())) {
                liked = true;
            }
            MomentUserVO liker = momentMapper.selectUserBrief(like.getUserId());
            likeNames.add(liker != null && StringUtils.isNotBlank(liker.getUsername())
                    ? liker.getUsername() : "用户" + tail(like.getUserId()));
        }
        vo.setLikes(likeNames);
        vo.setLiked(liked);
        vo.setLikeCount(likeNames.size());

        List<MomentCommentVO> comments = new ArrayList<>();
        LambdaQueryWrapper<MomentComment> commentQuery = new LambdaQueryWrapper<>();
        commentQuery.eq(MomentComment::getMomentId, moment.getId())
                .eq(MomentComment::getDelFlag, 0)
                .orderByAsc(MomentComment::getCreateTime);
        List<MomentComment> rows = momentCommentMapper.selectList(commentQuery);
        for (MomentComment row : rows) {
            MomentCommentVO c = new MomentCommentVO();
            c.setId(row.getId());
            c.setUserId(row.getUserId());
            MomentUserVO commenter = momentMapper.selectUserBrief(row.getUserId());
            c.setNickname(commenter != null && StringUtils.isNotBlank(commenter.getUsername())
                    ? commenter.getUsername() : "用户" + tail(row.getUserId()));
            c.setText(row.getContent());
            c.setCreateTime(row.getCreateTime() == null ? "" : row.getCreateTime().format(FMT));
            comments.add(c);
        }
        vo.setComments(comments);
        vo.setCommentCount(comments.size());
        return vo;
    }

    private String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return "";
        }
        return String.join(IMAGE_SEPARATOR, images);
    }

    private List<String> splitImages(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : raw.split("\\|")) {
            if (StringUtils.isNotBlank(s)) {
                result.add(s.trim());
            }
        }
        return result;
    }

    private String tail(String id) {
        if (StringUtils.isBlank(id) || id.length() < 4) {
            return "";
        }
        return id.substring(id.length() - 4);
    }
}
