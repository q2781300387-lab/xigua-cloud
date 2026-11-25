-- 数据库表结构

-- 数据库名称：xigua
-- 数据库编码：utf8mb4
-- 数据库排序规则：utf8mb4_0900_ai_ci

-- 用户表
-- auto-generated definition
create table xg_user
(
    id          bigint            not null comment '主键id'
        primary key,
    username    varchar(30)       null comment '用户名',
    password    varchar(255)      null comment '密码',
    email       varchar(30)       null comment '邮箱',
    phone       varchar(20)       null comment '手机号',
    avatar      varchar(300)      null comment '头像',
    sex         tinyint           null comment '性别（1；男；2：女）',
    region      varchar(200)      null comment '地区',
    signature   varchar(200)      null comment '个性签名',
    is_admin tinyint default 0 null comment '是否管理员（0：否；1：是）',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    update_by   varchar(20)       null comment '修改人',
    create_time datetime          null comment '创建时间',
    update_time datetime          null comment '修改时间',
    create_by   varchar(20)       null comment '创建人'
)
    comment '用户表';

-- 好友关系表
-- auto-generated definition
create table xg_friend_relation
(
    id          bigint            not null comment '主键id'
        primary key,
    user_id     bigint            null comment '自己',
    friend_id   bigint            null comment '对方(好友)',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '好友关系(双向关系)';

-- 好友请求表
-- auto-generated definition
create table xg_friend_request
(
    id          bigint            not null comment '主键id'
        primary key,
    sender_id   bigint            null comment '发起人',
    receiver_id bigint            null comment '接收人',
    flow_status tinyint           null comment '流程状态（0：待处理；1：同意；2：拒绝）',
    apply_msg   varchar(300)      null comment '验证消息',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '好友申请';

-- 聊天消息表
-- auto-generated definition
create table xg_chat_message
(
    id          bigint            not null comment '主键id'
        primary key,
    sender_id   bigint            null comment '发送人',
    receiver_id bigint            null comment '接收人',
    message     text              null comment '消息内容',
    chat_type   tinyint           not null comment '聊天类型（1：单聊；2：群聊）',
    message_type tinyint default 1 not null comment '消息类型（1文本 2图片 3语音 4视频 5文件）',
    lat         varchar(30)       null comment '纬度',
    lng         varchar(30)       null comment '经度',
    ip          varchar(30)       null comment 'ip地址',
    withdrawn   tinyint default 0 null comment '是否撤回 （0：未撤回；1：已撤回）',
    reply_to_id bigint            null comment '引用的消息id',
    reply_content varchar(1000)   null comment '引用消息内容快照',
    at_users    varchar(2000)     null comment '@提及的用户id列表（JSON数组字符串）',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '消息表';

-- 群组表
-- auto-generated definition
create table xg_group
(
    id             bigint            not null comment '主键id'
        primary key,
    owner_id       bigint            null comment '群主id',
    group_name     varchar(300)      null comment '群名称',
    group_avatar   varchar(300)      null comment '群头像',
    description    text              null comment '群描述',
    current_member int               null comment '当前成员数',
    del_flag       tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by      bigint            null comment '创建人',
    create_time    datetime          null comment '创建时间',
    update_by      bigint            null comment '修改人',
    update_time    datetime          null comment '修改时间'
)
    comment '群组表';

-- 群组成员表
-- auto-generated definition
create table xg_group_member
(
    id          bigint            not null comment '主键id'
        primary key,
    group_id    bigint            null comment '群组id',
    user_id     bigint            null comment '用户id',
    nickname    varchar(300)      null comment '群内昵称',
    status      tinyint default 1 null comment '状态（0：已退出；1：正常）',
    role        tinyint           null comment '群内角色（1：群主；2：群管理；3：普通成员）',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '群成员表';

-- 消息已读表
-- auto-generated definition
create table xg_message_read
(
    id          bigint            not null comment '主键id'
        primary key,
    sender_id   bigint            null comment '发送人id',
    receiver_id bigint            null comment '接收人id',
    message_id  bigint            null comment '消息id',
    is_read     tinyint           not null comment '是否已读（0：未读；1：已读）',
    read_time   datetime          null comment '已读时间',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '消息已读表';

-- ai机器人表
-- auto-generated definition
create table xg_bot
(
    id          bigint            not null comment '主键id'
        primary key,
    user_id     bigint            null comment '用户id',
    name        varchar(200)      null comment '名称',
    avatar      varchar(200)      null comment '头像',
    description varchar(300)      null comment '描述',
    prompt      text              null comment '提示词',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment 'ai机器人';

-- auto-generated definition
create table xg_login_log
(
    id           bigint            not null comment '主键id'
        primary key,
    user_id      varchar(30)       null comment '用户id',
    login_type   tinyint           null comment '登录方式（0：登出；1：密码）',
    login_ip     varchar(50)       null comment '登录ip',
    login_region varchar(150)      null comment '登录区域',
    user_agent   varchar(512)      null comment 'ua',
    platform     tinyint           null comment '平台（1：客户端web；2：管理端web；3：app；4：mini）',
    login_status tinyint           null comment '登录状态（登陆状态：1：成功；0：失败）',
    login_msg    varchar(512)      null comment '登录消息',
    create_by    bigint            null comment '创建人',
    create_time  datetime          null comment '创建时间',
    del_flag     tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）'
)
    comment '登录登出日志';

create table sys_role
(
    id          bigint            not null comment '主键id'
        primary key,
    role_name   varchar(100)      not null comment '角色名称',
    role_key    varchar(100)      not null comment '角色key',
    role_sort   int               null comment '顺序',
    status      tinyint           not null comment '状态（0：禁用；1：启用）',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '角色';

create table sys_menu
(
    id          bigint            not null comment '主键id'
        primary key,
    menu_name   varchar(100)      not null comment '菜单名称',
    parent_id   bigint  default 0 not null comment '父菜单id',
    menu_sort   int               null comment '排序',
    path        varchar(200)      null comment '路由路径',
    component   varchar(100)      null comment '组件',
    menu_type   tinyint           null comment '菜单类型（1：目录；2：菜单；3：按钮）',
    status      tinyint           not null comment '状态（0：禁用；1：启用）',
    perms       varchar(100)      not null comment '权限标识',
    icon        varchar(100)      not null comment '图标',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '菜单';

create table sys_role_menu
(
    id          bigint            not null comment '主键id'
        primary key,
    role_id     bigint            not null comment '角色id',
    menu_id     bigint            not null comment '菜单id',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '角色菜单';

create table sys_user_role
(
    id          bigint            not null comment '主键id'
        primary key,
    user_id     bigint            not null comment '用户id',
    role_id     bigint            not null comment '角色id',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '创建人',
    update_time datetime          null comment '修改时间'
)
    comment '用户角色';


create table xg_user_resource
(
    id          bigint            not null comment '资源id'
        primary key,
    user_id     bigint            not null comment '所属用户',
    url         varchar(500)      not null comment '资源访问URL',
    file_type   varchar(50)       not null comment '文件类型',
    size        bigint            not null comment '资源大小（字节）',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '用户资源表';


-- auto-generated definition
create table xg_user_resource_quota
(
    id             bigint            not null comment '主键id'
        primary key,
    user_id        bigint            not null comment '用户id',
    used_size      bigint  default 0 not null comment '已使用总容量（字节）',
    used_count     int     default 0 not null comment '已使用资源数量',
    max_total_size bigint            not null comment '最大允许总容量（字节）',
    max_file_size  bigint            not null comment '单个资源最大大小（字节）',
    del_flag       tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by      bigint            null comment '创建人',
    create_time    datetime          null comment '创建时间',
    update_by      bigint            null comment '修改人',
    update_time    datetime          null comment '修改时间'
)
    comment '用户资源配额统计表';

create table xg_node
(
    id          bigint            not null comment '主键id'
        primary key,
    node_name   varchar(100)      null comment '节点名称',
    node_code   varchar(100)      null comment '节点编码',
    inner_ip    varchar(50)       null comment '内网ip',
    outer_ip    varchar(50)       null comment '外网ip',
    region      varchar(100)      null comment '地区',
    remark      text              null comment '备注',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '节点实例';

create table xg_node_log
(
    id          bigint            not null comment '主键id'
        primary key,
    node_id     bigint            null comment '节点id',
    start_time  datetime          null comment '启动时间',
    end_time    datetime          null comment '下线时间',
    status      tinyint default 1 null comment '状态（0：下线；1：在线）',
    del_flag    tinyint default 0 null comment '是否删除 （0：未删除；1：已删除）',
    create_by   bigint            null comment '创建人',
    create_time datetime          null comment '创建时间',
    update_by   bigint            null comment '修改人',
    update_time datetime          null comment '修改时间'
)
    comment '节点日志';

-- 群文件中心
CREATE TABLE IF NOT EXISTS `xg_group_file` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `group_id` BIGINT NOT NULL COMMENT '所属群id',
  `uploader_id` BIGINT NOT NULL COMMENT '上传人id',
  `uploader_name` VARCHAR(64) DEFAULT NULL COMMENT '上传人昵称（冗余，便于列表展示）',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `file_url` VARCHAR(2048) NOT NULL COMMENT '文件访问地址',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `mime_type` VARCHAR(128) DEFAULT NULL COMMENT 'MIME 类型',
  `file_ext` VARCHAR(32) DEFAULT NULL COMMENT '扩展名',
  `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_file_group` (`group_id`, `del_flag`, `create_time`),
  UNIQUE KEY `uk_group_file_url` (`group_id`, `file_url`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群文件';
