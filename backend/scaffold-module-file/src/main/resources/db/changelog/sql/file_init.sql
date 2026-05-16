-- sys_file：上传文件主表（一行 = 一个物理文件落盘记录）
CREATE TABLE IF NOT EXISTS sys_file (
  id                BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  bucket            VARCHAR(64)   NOT NULL DEFAULT 'common' COMMENT '业务桶（cms/image / form / common 等）',
  folder_id         BIGINT(20)    DEFAULT NULL COMMENT '所属文件夹 ID（NULL = 根）',
  name              VARCHAR(255)  NOT NULL COMMENT '文件展示名',
  original_name     VARCHAR(255)  NOT NULL COMMENT '上传时原始文件名',
  ext               VARCHAR(16)   DEFAULT NULL COMMENT '扩展名（小写、不含点）',
  mime              VARCHAR(128)  DEFAULT NULL COMMENT 'MIME 类型',
  size_bytes        BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '文件字节数',
  storage_path      VARCHAR(512)  NOT NULL COMMENT '物理存储路径（FileStorageService 返回的相对 URL）',
  category          VARCHAR(64)   DEFAULT NULL COMMENT '分类标签（用户自定义）',
  tags              VARCHAR(500)  DEFAULT NULL COMMENT '逗号分隔的标签列表',
  ref_count         INT(11)       NOT NULL DEFAULT 0 COMMENT '跨模块引用计数（>0 时禁删）',
  del_flag          CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0=正常 / 2=软删（30 天后由 quartz 物理清除）',
  delete_time       DATETIME      DEFAULT NULL COMMENT '软删时间（用于 30 天定时清磁盘）',
  create_by         VARCHAR(64)   DEFAULT '' COMMENT '上传者 username',
  create_by_name    VARCHAR(64)   DEFAULT NULL COMMENT '上传者昵称（冗余）',
  create_time       DATETIME      DEFAULT NULL COMMENT '上传时间',
  update_by         VARCHAR(64)   DEFAULT '' COMMENT '更新者',
  update_time       DATETIME      DEFAULT NULL COMMENT '更新时间',
  remark            VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_bucket_create (bucket, create_time),
  KEY idx_folder (folder_id),
  KEY idx_creator_time (create_by, create_time),
  KEY idx_del_flag_time (del_flag, delete_time),
  KEY idx_ext (ext),
  KEY idx_category (category)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文件中心 - 上传文件主表';

-- sys_file_folder：文件夹（每个用户独立的简单两层 path 树）
CREATE TABLE IF NOT EXISTS sys_file_folder (
  id            BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  owner         VARCHAR(64)   NOT NULL COMMENT '所有者 username',
  parent_id     BIGINT(20)    NOT NULL DEFAULT 0 COMMENT '父级 ID（0 = 根）',
  name          VARCHAR(128)  NOT NULL COMMENT '文件夹名',
  path          VARCHAR(512)  NOT NULL COMMENT '从根到本级的路径（/research/papers）',
  del_flag      CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0=正常 / 2=软删',
  create_by     VARCHAR(64)   DEFAULT '' COMMENT '创建者',
  create_time   DATETIME      DEFAULT NULL COMMENT '创建时间',
  update_by     VARCHAR(64)   DEFAULT '' COMMENT '更新者',
  update_time   DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_owner_path (owner, path),
  KEY idx_owner_parent (owner, parent_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文件中心 - 文件夹';

-- sys_file_share：分享链接（带过期 / 一次性）
CREATE TABLE IF NOT EXISTS sys_file_share (
  id            BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  file_id       BIGINT(20)    NOT NULL COMMENT '关联 sys_file.id',
  token         VARCHAR(64)   NOT NULL COMMENT '随机 token（URL 友好）',
  expire_at     DATETIME      DEFAULT NULL COMMENT '过期时间（NULL = 永久）',
  one_time      CHAR(1)       NOT NULL DEFAULT '0' COMMENT '1=一次性（用过即销毁）',
  visits        INT(11)       NOT NULL DEFAULT 0 COMMENT '已访问次数',
  password_hash VARCHAR(120)  DEFAULT NULL COMMENT '可选访问密码（BCrypt）',
  status        CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0=有效 / 1=已停用 / 2=已用尽',
  create_by     VARCHAR(64)   DEFAULT '' COMMENT '分享者 username',
  create_time   DATETIME      DEFAULT NULL COMMENT '创建时间',
  update_time   DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_token (token),
  KEY idx_file (file_id),
  KEY idx_status_expire (status, expire_at)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文件中心 - 分享链接';

-- sys_file_ref：跨模块引用（CMS 文章 / Form 提交 / 业务表单 引用同一个文件时累加）
CREATE TABLE IF NOT EXISTS sys_file_ref (
  id            BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  file_id       BIGINT(20)    NOT NULL COMMENT '引用的 sys_file.id',
  ref_module    VARCHAR(32)   NOT NULL COMMENT '引用方模块（cms / form / wf 等）',
  ref_type      VARCHAR(64)   NOT NULL COMMENT '引用方业务类型（article / submission 等）',
  ref_id        VARCHAR(64)   NOT NULL COMMENT '引用方业务记录 ID',
  create_by     VARCHAR(64)   DEFAULT '' COMMENT '建立引用的用户',
  create_time   DATETIME      DEFAULT NULL COMMENT '建立引用时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_file_ref (file_id, ref_module, ref_type, ref_id),
  KEY idx_ref_lookup (ref_module, ref_type, ref_id),
  KEY idx_file (file_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文件中心 - 跨模块引用计数明细';
