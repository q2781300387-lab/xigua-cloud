-- 全局配置与后台操作审计表
-- 数据库：xigua

CREATE TABLE IF NOT EXISTS `xg_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `config_key`   VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_name`  VARCHAR(100) NOT NULL COMMENT '配置名称',
    `config_value` TEXT         NULL COMMENT '配置值',
    `config_group` VARCHAR(50)  NOT NULL DEFAULT '通用' COMMENT '配置分组',
    `value_type`   VARCHAR(20)  NOT NULL DEFAULT 'string' COMMENT '值类型：string/number/boolean/json',
    `description`  VARCHAR(500) NULL COMMENT '配置说明',
    `editable`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否允许修改（0：否；1：是）',
    `create_by`    VARCHAR(50)  NULL COMMENT '创建人',
    `create_time`  DATETIME     NULL COMMENT '创建时间',
    `update_by`    VARCHAR(50)  NULL COMMENT '修改人',
    `update_time`  DATETIME     NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_group` (`config_group`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '全局配置表';

CREATE TABLE IF NOT EXISTS `xg_oper_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `admin_id`    VARCHAR(64)  NULL COMMENT '操作人ID',
    `admin_name`  VARCHAR(50)  NULL COMMENT '操作人名称',
    `module`      VARCHAR(50)  NULL COMMENT '业务模块',
    `action`      VARCHAR(100) NULL COMMENT '操作动作',
    `request_uri` VARCHAR(300) NULL COMMENT '请求路径',
    `method`      VARCHAR(10)  NULL COMMENT '请求方法',
    `params`      TEXT         NULL COMMENT '请求参数（敏感字段已脱敏）',
    `ip`          VARCHAR(64)  NULL COMMENT '来源IP',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '结果（1：成功；0：失败）',
    `error_msg`   VARCHAR(1000) NULL COMMENT '失败原因',
    `cost_ms`     BIGINT       NULL COMMENT '耗时（毫秒）',
    `create_time` DATETIME     NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_oper_log_time` (`create_time`),
    KEY `idx_oper_log_module` (`module`),
    KEY `idx_oper_log_admin` (`admin_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '后台操作审计日志表';

INSERT INTO `xg_config`
    (`config_key`, `config_name`, `config_value`, `config_group`, `value_type`, `description`, `editable`)
VALUES
    ('site.name', '站点名称', '西瓜IM', '站点', 'string', '显示在后台登录页、顶栏与浏览器标题中的产品名称', 1),
    ('site.description', '站点描述', '基于 Spring Cloud Alibaba 的即时通讯系统', '站点', 'string', '用于登录页与关于页的产品简介', 1),
    ('message.recall.window', '消息撤回时限', '120', '消息', 'number', '允许撤回消息的时间窗口，单位秒', 1),
    ('message.max.length', '单条消息最大长度', '5000', '消息', 'number', '单条文本消息允许的最大字符数', 1),
    ('upload.image.max.size', '图片上传上限', '10', '上传', 'number', '单张图片允许上传的最大体积，单位 MB', 1),
    ('upload.file.max.size', '文件上传上限', '100', '上传', 'number', '单个聊天文件允许上传的最大体积，单位 MB', 1),
    ('security.login.captcha', '登录验证码', 'true', '安全', 'boolean', '后台登录是否强制校验图形验证码', 1),
    ('security.session.timeout', '登录会话有效期', '7200', '安全', 'number', '后台登录令牌有效期，单位秒', 1),
    ('client.heartbeat.interval', '客户端心跳间隔', '30', '客户端', 'number', '客户端 WebSocket 心跳间隔，单位秒', 1),
    ('client.default.avatar', '默认用户头像', '/assets/user-placeholder.svg', '客户端', 'string', '用户未设置头像时使用的默认资源路径', 1)
ON DUPLICATE KEY UPDATE `config_key` = VALUES(`config_key`);
