package com.xigua.common.docker.client;

import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Volume;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @ClassName DockerClientService
 * @Description Docker 客户端服务，提供容器、镜像等操作
 * @Author Claude Code
 * @Date 2026/5/8
 */
@Slf4j
@Component
public class DockerClient {

    @Autowired
    private com.github.dockerjava.api.DockerClient dockerClient;

    // ==================== 容器操作 ====================

    /**
     * 列出所有容器
     *
     * @param showAll 是否显示所有容器（包括已停止的）
     * @return 容器列表
     */
    public List<Container> listContainers(boolean showAll) {
        try {
            return dockerClient.listContainersCmd()
                    .withShowAll(showAll)
                    .exec();
        } catch (Exception e) {
            log.error("列出容器失败", e);
            throw new RuntimeException("列出容器失败", e);
        }
    }

    /**
     * 列出所有运行中的容器
     *
     * @return 运行中的容器列表
     */
    public List<Container> listRunningContainers() {
        return listContainers(false);
    }

    /**
     * 根据容器 ID 或名称查询容器详情
     *
     * @param containerIdOrName 容器 ID 或名称
     * @return 容器详情
     */
    public InspectContainerResponse inspectContainer(String containerIdOrName) {
        try {
            return dockerClient.inspectContainerCmd(containerIdOrName).exec();
        } catch (Exception e) {
            log.error("查询容器详情失败: {}", containerIdOrName, e);
            throw new RuntimeException("查询容器详情失败: " + containerIdOrName, e);
        }
    }

    /**
     * 启动容器
     *
     * @param containerId 容器 ID
     */
    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            log.info("容器启动成功: {}", containerId);
        } catch (Exception e) {
            log.error("启动容器失败: {}", containerId, e);
            throw new RuntimeException("启动容器失败: " + containerId, e);
        }
    }

    /**
     * 停止容器
     *
     * @param containerId 容器 ID
     */
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            log.info("容器停止成功: {}", containerId);
        } catch (Exception e) {
            log.error("停止容器失败: {}", containerId, e);
            throw new RuntimeException("停止容器失败: " + containerId, e);
        }
    }

    /**
     * 重启容器
     *
     * @param containerId 容器 ID
     */
    public void restartContainer(String containerId) {
        try {
            dockerClient.restartContainerCmd(containerId).exec();
            log.info("容器重启成功: {}", containerId);
        } catch (Exception e) {
            log.error("重启容器失败: {}", containerId, e);
            throw new RuntimeException("重启容器失败: " + containerId, e);
        }
    }

    /**
     * 删除容器
     *
     * @param containerId 容器 ID
     * @param force       是否强制删除
     */
    public void removeContainer(String containerId, boolean force) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(force)
                    .exec();
            log.info("容器删除成功: {}", containerId);
        } catch (Exception e) {
            log.error("删除容器失败: {}", containerId, e);
            throw new RuntimeException("删除容器失败: " + containerId, e);
        }
    }

    /**
     * 创建容器
     *
     * @param createContainerCmd 创建容器命令
     * @return 容器创建响应
     */
    public CreateContainerResponse createContainer(CreateContainerCmd createContainerCmd) {
        try {
            CreateContainerResponse response = createContainerCmd.exec();
            log.info("容器创建成功: {}", response.getId());
            return response;
        } catch (Exception e) {
            log.error("创建容器失败", e);
            throw new RuntimeException("创建容器失败", e);
        }
    }

    // ==================== 镜像操作 ====================

    /**
     * 列出所有镜像
     *
     * @return 镜像列表
     */
    public List<Image> listImages() {
        try {
            return dockerClient.listImagesCmd()
                    .withShowAll(true)
                    .exec();
        } catch (Exception e) {
            log.error("列出镜像失败", e);
            throw new RuntimeException("列出镜像失败", e);
        }
    }

    /**
     * 根据镜像 ID 或名称查询镜像详情
     *
     * @param imageIdOrName 镜像 ID 或名称
     * @return 镜像详情
     */
    public InspectImageResponse inspectImage(String imageIdOrName) {
        try {
            return dockerClient.inspectImageCmd(imageIdOrName).exec();
        } catch (Exception e) {
            log.error("查询镜像详情失败: {}", imageIdOrName, e);
            throw new RuntimeException("查询镜像详情失败: " + imageIdOrName, e);
        }
    }

    /**
     * 拉取镜像
     *
     * @param repository 镜像仓库名称
     * @param tag        标签
     */
    public void pullImage(String repository, String tag) {
        try {
            dockerClient.pullImageCmd(repository)
                    .withTag(tag)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
            log.info("镜像拉取成功: {}:{}", repository, tag);
        } catch (Exception e) {
            log.error("拉取镜像失败: {}:{}", repository, tag, e);
            throw new RuntimeException("拉取镜像失败: " + repository + ":" + tag, e);
        }
    }

    /**
     * 删除镜像
     *
     * @param imageId 镜像 ID
     * @param force   是否强制删除
     */
    public void removeImage(String imageId, boolean force) {
        try {
            dockerClient.removeImageCmd(imageId)
                    .withForce(force)
                    .exec();
            log.info("镜像删除成功: {}", imageId);
        } catch (Exception e) {
            log.error("删除镜像失败: {}", imageId, e);
            throw new RuntimeException("删除镜像失败: " + imageId, e);
        }
    }

    /**
     * 标记镜像
     *
     * @param imageId    镜像 ID
     * @param repository 新仓库名称
     * @param tag        新标签
     */
    public void tagImage(String imageId, String repository, String tag) {
        try {
            dockerClient.tagImageCmd(imageId, repository, tag).exec();
            log.info("镜像标记成功: {} -> {}:{}", imageId, repository, tag);
        } catch (Exception e) {
            log.error("标记镜像失败: {}", imageId, e);
            throw new RuntimeException("标记镜像失败: " + imageId, e);
        }
    }

    // ==================== 网络操作 ====================

    /**
     * 列出所有网络
     *
     * @return 网络列表
     */
    public List<Network> listNetworks() {
        try {
            return dockerClient.listNetworksCmd().exec();
        } catch (Exception e) {
            log.error("列出网络失败", e);
            throw new RuntimeException("列出网络失败", e);
        }
    }

    /**
     * 根据网络 ID 查询网络详情
     *
     * @param networkId 网络 ID
     * @return 网络详情
     */
    public Network inspectNetwork(String networkId) {
        try {
            return dockerClient.inspectNetworkCmd().withNetworkId(networkId).exec();
        } catch (Exception e) {
            log.error("查询网络详情失败: {}", networkId, e);
            throw new RuntimeException("查询网络详情失败: " + networkId, e);
        }
    }

    /**
     * 创建网络
     *
     * @param name 网络名称
     * @return 网络创建响应
     */
    public CreateNetworkResponse createNetwork(String name) {
        try {
            CreateNetworkResponse response = dockerClient.createNetworkCmd()
                    .withName(name)
                    .exec();
            log.info("网络创建成功: {}", name);
            return response;
        } catch (Exception e) {
            log.error("创建网络失败: {}", name, e);
            throw new RuntimeException("创建网络失败: " + name, e);
        }
    }

    /**
     * 删除网络
     *
     * @param networkId 网络 ID
     */
    public void removeNetwork(String networkId) {
        try {
            dockerClient.removeNetworkCmd(networkId).exec();
            log.info("网络删除成功: {}", networkId);
        } catch (Exception e) {
            log.error("删除网络失败: {}", networkId, e);
            throw new RuntimeException("删除网络失败: " + networkId, e);
        }
    }

    // ==================== 卷操作 ====================

    /**
     * 列出所有卷
     *
     * @return 卷列表
     */
    public List<InspectVolumeResponse> listVolumes() {
        try {
            ListVolumesResponse response = dockerClient.listVolumesCmd().exec();
            return response.getVolumes();
        } catch (Exception e) {
            log.error("列出卷失败", e);
            throw new RuntimeException("列出卷失败", e);
        }
    }

    /**
     * 根据卷名称查询卷详情
     *
     * @param volumeName 卷名称
     * @return 卷详情
     */
    public InspectVolumeResponse inspectVolume(String volumeName) {
        try {
            return dockerClient.inspectVolumeCmd(volumeName).exec();
        } catch (Exception e) {
            log.error("查询卷详情失败: {}", volumeName, e);
            throw new RuntimeException("查询卷详情失败: " + volumeName, e);
        }
    }

    /**
     * 创建卷
     *
     * @param name 卷名称
     * @return 卷创建响应
     */
    public CreateVolumeResponse createVolume(String name) {
        try {
            CreateVolumeResponse response = dockerClient.createVolumeCmd()
                    .withName(name)
                    .exec();
            log.info("卷创建成功: {}", name);
            return response;
        } catch (Exception e) {
            log.error("创建卷失败: {}", name, e);
            throw new RuntimeException("创建卷失败: " + name, e);
        }
    }

    /**
     * 删除卷
     *
     * @param volumeName 卷名称
     */
    public void removeVolume(String volumeName) {
        try {
            dockerClient.removeVolumeCmd(volumeName).exec();
            log.info("卷删除成功: {}", volumeName);
        } catch (Exception e) {
            log.error("删除卷失败: {}", volumeName, e);
            throw new RuntimeException("删除卷失败: " + volumeName, e);
        }
    }

    // ==================== Docker 信息 ====================

    /**
     * 获取 Docker 系统信息
     *
     * @return Docker 系统信息
     */
    public Info getDockerInfo() {
        try {
            return dockerClient.infoCmd().exec();
        } catch (Exception e) {
            log.error("获取 Docker 信息失败", e);
            throw new RuntimeException("获取 Docker 信息失败", e);
        }
    }

    public Long getContainerRunningSec(String containerId) {
        // 执行 inspect 命令获取容器详情
        InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId)
                .exec();

        InspectContainerResponse.ContainerState state = inspect.getState();

        // 检查容器是否在运行中
        if (!state.getRunning()) {
            log.info("容器未运行");
            return 0L;
        }

        String startedAtStr = state.getStartedAt();

        // 解析 ISO 8601 格式的时间戳
        Instant startedAt = DateTimeFormatter.ISO_INSTANT.parse(startedAtStr, Instant::from);
        Instant now = Instant.now();

        // 返回运行秒数（long类型）
        return Duration.between(startedAt, now).getSeconds();
    }

    /**
     * Ping Docker 守护进程
     *
     * @return 连接是否正常
     */
    public boolean ping() {
        try {
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            log.error("Ping Docker 失败", e);
            return false;
        }
    }

    /**
     * 获取原始 DockerClient 实例
     *
     * @return DockerClient
     */
    public com.github.dockerjava.api.DockerClient getDockerClient() {
        return dockerClient;
    }
}
