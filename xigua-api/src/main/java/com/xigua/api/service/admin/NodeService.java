package com.xigua.api.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xigua.common.mybatis.base.BasePageVO;
import com.xigua.domain.dto.QueryNodePageDTO;
import com.xigua.domain.dto.SaveNodeDTO;
import com.xigua.domain.entity.Node;
import com.xigua.domain.vo.admin.NodeVO;

import java.util.List;

/**
 * @ClassName NodeService
 * @Description 节点服务接口
 * @Author wangjinfei
 * @Date 2026/2/1 13:42
 */
public interface NodeService extends IService<Node> {

    /**
     * 分页查询节点
     * @param dto 查询参数
     * @return BasePageVO<NodeVO>
     */
    BasePageVO<NodeVO> pageNodes(QueryNodePageDTO dto);

    /**
     * 根据ID查询节点详情
     * @param id 节点ID
     * @return NodeVO
     */
    NodeVO getNodeById(String id);

    /**
     * 保存节点（新增或更新）
     * @param dto 节点信息
     * @return Boolean
     */
    Boolean saveNode(SaveNodeDTO dto);

    /**
     * 批量删除节点
     * @param ids 节点ID列表
     * @return Boolean
     */
    Boolean deleteNodes(List<String> ids);

    /**
     * 刷新节点系统信息
     * @param id 节点ID
     */
    void refreshNodeSystem(String id);

    /**
     * 获取节点统计信息
     * @return NodeStatisticsVO
     */
    com.xigua.domain.vo.admin.NodeStatisticsVO getNodeStatistics();

    /**
     * 根据内网IP查询节点
     * @param innerIp 内网IP
     * @return Node
     */
    Node getByInnerIp(String innerIp);

    /**
     * 获取节点详情（包含启动时间等运行时信息）
     * @param id 节点ID
     * @return NodeDetailVO
     */
    com.xigua.domain.vo.admin.NodeDetailVO getNodeDetail(String id);

    /**
     * 获取节点运行指标（连接数、TPS、CPU、内存等）
     * @param id 节点ID
     * @return NodeMetricsVO
     */
    com.xigua.domain.vo.admin.NodeMetricsVO getNodeMetrics(String id);

    /**
     * 分页查询节点在线用户
     * @param dto 查询参数
     * @return BasePageVO<NodeOnlineUserVO>
     */
    com.xigua.common.mybatis.base.BasePageVO<com.xigua.domain.vo.admin.NodeOnlineUserVO> pageNodeOnlineUsers(com.xigua.domain.dto.QueryNodeOnlineUserDTO dto);

    /**
     * 节点上线
     * @param nodeIp 节点内网ip
     * @return Boolean
     */
    Boolean online(String nodeIp);

    /**
     * 节点下线
     * @param nodeIp 节点内网ip
     * @return Boolean
     */
    Boolean offline(String nodeIp);
}
