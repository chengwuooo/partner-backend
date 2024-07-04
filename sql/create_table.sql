create
database if not exists partner;

use
partner;

-- 用户表
create table user
(
    userName     varchar(256) null comment '用户昵称',
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256) null comment '账号',
    userAvatar    varchar(1024) null comment '用户头像',
    gender       tinyint null comment '性别',
    userPassword varchar(512)       not null comment '密码',
    phone        varchar(128) null comment '电话',
    email        varchar(512) null comment '邮箱',
    userStatus   int      default 0 not null comment '状态 0 - 正常',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete     tinyint  default 0 not null comment '是否删除',
    userRole     int      default 0 not null comment '用户角色 0 - 普通用户 1 - 管理员',
    tags         varchar(1024) null comment '标签 json 列表'
) comment '用户';

-- 队伍表
create table team
(
    id            bigint auto_increment comment 'id'
        primary key,
    teamName      varchar(256)                       not null comment '队伍名称',
    teamAvatarUrl varchar(1024)                      null comment '队伍头像',
    description   varchar(1024)                      null comment '描述',
    maxNum        int      default 1                 not null comment '最大人数',
    expireTime    datetime                           null comment '过期时间',
    userId        bigint                             null comment '用户id（队长 id）',
    status        int      default 0                 not null comment '0 - 公开，1 - 私有，2 - 加密',
    password      varchar(512)                       null comment '密码',
    createTime    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete      tinyint  default 0                 not null comment '是否删除'
)
    comment '队伍' row_format = DYNAMIC;

-- 用户队伍关系
create table user_team
(
    id         bigint auto_increment comment 'id'
        primary key,
    userId     bigint comment '用户id',
    teamId     bigint comment '队伍id',
    joinTime   datetime null comment '加入时间',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint  default 0 not null comment '是否删除'
) comment '用户队伍关系';


-- 标签表（可以不创建，因为标签字段已经放到了用户表中）
create table tag
(
    id         bigint auto_increment comment 'id'
        primary key,
    tagName    varchar(256) null comment '标签名称',
    userId     bigint null comment '用户 id',
    parentId   bigint null comment '父标签 id',
    isParent   tinyint null comment '0 - 不是, 1 - 父标签',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint  default 0 not null comment '是否删除',
    constraint uniIdx_tagName
        unique (tagName)
) comment '标签';

create index idx_userId
    on tag (userId);


-- auto-generated definition
create table message
(
    id          bigint auto_increment comment '聊天记录id'
        primary key,
    fromId      bigint                                  not null comment '发送消息id',
    toId        bigint                                  null comment '接收消息id',
    text        varchar(512) collate utf8mb4_unicode_ci null,
    messageType tinyint                                 not null comment '聊天类型 1-私聊 2-群聊',
    date        datetime default CURRENT_TIMESTAMP      null comment '创建时间',
    isRead      int                                     null comment '是否被阅读',
    isDelete    int                                     null comment '是否被撤回'
)
    comment '聊天消息表';





