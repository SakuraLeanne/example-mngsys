CREATE TABLE IF NOT EXISTS portal_user (
    id VARCHAR(64) NOT NULL COMMENT 'uuid或雪花ID',
    username VARCHAR(64) NOT NULL COMMENT '登录名，默认同手机号',
    mobile VARCHAR(20) NOT NULL COMMENT '手机号，唯一',
    mobile_verified INT NOT NULL DEFAULT 0 COMMENT '手机号是否已验证：0-否，1-是',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    email_verified INT NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证：0-否，1-是',
    password VARCHAR(255) NOT NULL COMMENT '密码哈希串（PasswordEncoder输出）',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常，2-冻结',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    nick_name VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    gender VARCHAR(16) DEFAULT NULL COMMENT '性别',
    birthday DATE DEFAULT NULL COMMENT '出生日期',
    company_name VARCHAR(128) DEFAULT NULL COMMENT '公司',
    department VARCHAR(128) DEFAULT NULL COMMENT '部门',
    position VARCHAR(128) DEFAULT NULL COMMENT '职位',
    tenant_id VARCHAR(64) DEFAULT NULL COMMENT '所属园区/租户',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人ID',
    update_by VARCHAR(64) DEFAULT NULL COMMENT '修改人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_portal_user_mobile (mobile),
    UNIQUE KEY uk_portal_user_username (username),
    KEY idx_portal_user_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户用户表';

CREATE TABLE IF NOT EXISTS portal_user_auth_state (
    user_id VARCHAR(64) PRIMARY KEY COMMENT '用户ID',
    auth_version BIGINT NOT NULL DEFAULT 1 COMMENT '认证信息版本号',
    profile_version BIGINT NOT NULL DEFAULT 1 COMMENT '档案信息版本号',
    last_pwd_change_time DATETIME DEFAULT NULL COMMENT '最近密码变更时间',
    last_profile_update_time DATETIME DEFAULT NULL COMMENT '最近档案更新',
    last_disable_time DATETIME DEFAULT NULL COMMENT '最近禁用时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户用户认证状态表';

CREATE TABLE IF NOT EXISTS app_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    app_code VARCHAR(64) NOT NULL COMMENT '应用编码',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(128) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_app_role_code (app_code, role_code),
    KEY idx_app_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用角色表';

CREATE TABLE IF NOT EXISTS app_menu_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    app_code VARCHAR(64) NOT NULL COMMENT '应用编码',
    menu_code VARCHAR(64) NOT NULL COMMENT '菜单/资源编码',
    menu_name VARCHAR(128) NOT NULL COMMENT '菜单/资源名称',
    menu_path VARCHAR(255) DEFAULT NULL COMMENT '前端路由/资源路径',
    menu_type VARCHAR(32) DEFAULT NULL COMMENT '类型：menu/button/link等',
    menu_module VARCHAR(64) DEFAULT NULL COMMENT '菜单模块',
    parent_id BIGINT DEFAULT NULL COMMENT '父菜单ID',
    permission VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号（升序）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_app_menu_code (app_code, menu_code),
    KEY idx_app_menu_parent (parent_id),
    KEY idx_app_menu_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用菜单及资源表';

CREATE TABLE IF NOT EXISTS app_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_app_user_role (user_id, role_id),
    KEY idx_app_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS app_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单/资源ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_app_role_menu (role_id, menu_id),
    KEY idx_app_role_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单/资源关联表';

CREATE TABLE IF NOT EXISTS portal_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    user_id VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    action VARCHAR(128) NOT NULL COMMENT '操作名称',
    resource VARCHAR(255) DEFAULT NULL COMMENT '操作资源',
    detail TEXT DEFAULT NULL COMMENT '详细信息',
    ip VARCHAR(64) DEFAULT NULL COMMENT '来源IP',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_portal_audit_user (user_id),
    KEY idx_portal_audit_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户审计日志表';

INSERT INTO portal_user (id, username, mobile, mobile_verified, email, email_verified, password, status, real_name, nick_name, create_time, update_time)
VALUES ('u-admin-0001', 'admin', '13800000000', 1, 'admin@example.com', 1, '{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5Cw5IV/pY5PaaC2l5x4pnW5sA8vz', 1, '管理员', '管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('u-user-0002', 'user', '13900000000', 1, 'user@example.com', 0, '{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5Cw5IV/pY5PaaC2l5x4pnW5sA8vz', 1, '普通用户', '普通用户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
