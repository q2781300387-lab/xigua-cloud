package com.xigua.api.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.QueryNodeLogPageDTO;
import com.xigua.domain.entity.Node;
import com.xigua.domain.entity.NodeLog;
import com.xigua.domain.vo.admin.NodeLogVO;

import java.util.List;

/**
 * @ClassName NodeLogService
 * @Description 节点日志服务接口
 * @Author Claude
 * @Date 2026/03/18
 */
public interface NodeLogService extends IService<NodeLog> {

    /**
     * 分页查询节点日志
     * @param dto 查询参数
     * @return BasePageVO<NodeLogVO>
     */
    BasePageVO<NodeLogVO> pageNodeLogs(QueryNodeLogPageDTO dto);

    /**
     * 根据ID查询节点日志详情
     * @param id 日志ID
     * @return NodeLogVO
     */
    NodeLogVO getNodeLogById(String id);

    /**
     * 记录节点上线
     * @param node 节点信息
     * @return Boolean
     */
    Boolean recordNodeOnline(Node node);

    /**
     * 记录节点下线
     * @param node 节点信息
     * @return Boolean
     */
    Boolean recordNodeOffline(Node node);
}
