-- CMS 模块初始化：栏目 / 文章 / 标签 / 文章-标签 关联
-- changeset cms-20260507-init
-- 注意：MySQL 5.7+ 自带 ngram 中文分词器；FULLTEXT 索引需要显式 WITH PARSER ngram，
-- 否则中文按空格切词，效果等同于按字 LIKE。如果数据库不是 MySQL（或版本过低），
-- 可以用 `ALTER TABLE cms_article DROP INDEX ft_cms_article_title;` 把 FULLTEXT 拆掉，
-- 后台搜索退化成 LIKE。

CREATE TABLE cms_channel (
    id              BIGINT          NOT NULL AUTO_INCREMENT          COMMENT '主键',
    parent_id       BIGINT          NOT NULL DEFAULT 0                COMMENT '父栏目 id；0=根',
    code            VARCHAR(64)     NOT NULL                          COMMENT '栏目编码（唯一，用于公开 API URL）',
    name            VARCHAR(120)    NOT NULL                          COMMENT '栏目名称',
    order_num       INT             NOT NULL DEFAULT 0                COMMENT '同级排序',
    status          CHAR(1)         NOT NULL DEFAULT '0'              COMMENT '状态：0=启用 1=停用',
    keywords        VARCHAR(255)    DEFAULT ''                        COMMENT 'SEO 关键词，逗号分隔',
    description     VARCHAR(500)    DEFAULT ''                        COMMENT 'SEO 描述',
    template        VARCHAR(120)    DEFAULT ''                        COMMENT '前端门户模板名（保留字段，门户用）',
    create_by       VARCHAR(64)     DEFAULT ''                        COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
    update_by       VARCHAR(64)     DEFAULT ''                        COMMENT '更新人',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag        CHAR(1)         NOT NULL DEFAULT '0'              COMMENT '0=正常 2=软删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cms_channel_code (code),
    KEY idx_cms_channel_parent (parent_id, order_num)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'CMS 栏目（树）';

CREATE TABLE cms_article (
    id              BIGINT          NOT NULL AUTO_INCREMENT          COMMENT '主键',
    channel_id      BIGINT          NOT NULL                          COMMENT '所属栏目 id',
    title           VARCHAR(255)    NOT NULL                          COMMENT '标题',
    slug            VARCHAR(160)    NOT NULL                          COMMENT 'URL slug（唯一，对外 API 用）',
    summary         VARCHAR(500)    DEFAULT ''                        COMMENT '摘要',
    cover_url       VARCHAR(500)    DEFAULT ''                        COMMENT '封面图 URL',
    content_html    LONGTEXT                                          COMMENT '正文 HTML（wangEditor 输出）',
    source          VARCHAR(120)    DEFAULT ''                        COMMENT '来源',
    author          VARCHAR(120)    DEFAULT ''                        COMMENT '作者',
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'          COMMENT '状态：DRAFT/PENDING/PUBLISHED/UNPUBLISHED',
    meta_title      VARCHAR(255)    DEFAULT ''                        COMMENT 'SEO meta title',
    meta_description VARCHAR(500)   DEFAULT ''                        COMMENT 'SEO meta description',
    meta_keywords   VARCHAR(255)    DEFAULT ''                        COMMENT 'SEO meta keywords',
    canonical_url   VARCHAR(500)    DEFAULT ''                        COMMENT 'SEO canonical URL',
    published_at    DATETIME        DEFAULT NULL                      COMMENT '首次发布时间（PUBLISHED 时写入；下线再上线不重置）',
    view_count      BIGINT          NOT NULL DEFAULT 0                COMMENT '阅读量',
    sort_order      INT             NOT NULL DEFAULT 0                COMMENT '同栏目排序，越大越靠前',
    process_instance_id VARCHAR(64) DEFAULT NULL                      COMMENT '关联的 workflow 流程实例 id（M-4 cms-workflow 桥写入；NULL=未走 workflow）',
    create_by       VARCHAR(64)     DEFAULT ''                        COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
    update_by       VARCHAR(64)     DEFAULT ''                        COMMENT '更新人',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag        CHAR(1)         NOT NULL DEFAULT '0'              COMMENT '0=正常 2=软删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cms_article_slug (slug),
    KEY idx_cms_article_list (channel_id, status, del_flag, published_at, sort_order),
    FULLTEXT KEY ft_cms_article_search (title, summary, content_html) WITH PARSER ngram
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'CMS 文章';

CREATE TABLE cms_tag (
    id              BIGINT          NOT NULL AUTO_INCREMENT          COMMENT '主键',
    name            VARCHAR(64)     NOT NULL                          COMMENT '标签名称',
    color           VARCHAR(20)     NOT NULL DEFAULT ''               COMMENT '展示色 hex',
    create_by       VARCHAR(64)     DEFAULT ''                        COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cms_tag_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'CMS 标签字典';

CREATE TABLE cms_article_tag (
    article_id      BIGINT          NOT NULL                          COMMENT '文章 id',
    tag_id          BIGINT          NOT NULL                          COMMENT '标签 id',
    PRIMARY KEY (article_id, tag_id),
    KEY idx_cms_article_tag_tag (tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'CMS 文章-标签 关联';
