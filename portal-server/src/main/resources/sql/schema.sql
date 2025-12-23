CREATE TABLE IF NOT EXISTS portal_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    real_name VARCHAR(64) DEFAULT NULL,
    mobile VARCHAR(32) DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    disable_reason VARCHAR(255) DEFAULT NULL,
    disable_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_portal_user_username (username),
    KEY idx_portal_user_mobile (mobile),
    KEY idx_portal_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS portal_user_auth_state (
    user_id BIGINT PRIMARY KEY,
    auth_version BIGINT NOT NULL DEFAULT 1,
    profile_version BIGINT NOT NULL DEFAULT 1,
    last_pwd_change_time DATETIME DEFAULT NULL,
    last_profile_update_time DATETIME DEFAULT NULL,
    last_disable_time DATETIME DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS app_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_code VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_role_code (app_code, role_code),
    KEY idx_app_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS app_menu_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_code VARCHAR(64) NOT NULL,
    menu_code VARCHAR(64) NOT NULL,
    menu_name VARCHAR(128) NOT NULL,
    menu_path VARCHAR(255) DEFAULT NULL,
    menu_type VARCHAR(32) DEFAULT NULL,
    parent_id BIGINT DEFAULT NULL,
    permission VARCHAR(128) DEFAULT NULL,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_menu_code (app_code, menu_code),
    KEY idx_app_menu_parent (parent_id),
    KEY idx_app_menu_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS app_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_user_role (user_id, role_id),
    KEY idx_app_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS app_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_role_menu (role_id, menu_id),
    KEY idx_app_role_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS portal_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,
    username VARCHAR(64) DEFAULT NULL,
    action VARCHAR(128) NOT NULL,
    resource VARCHAR(255) DEFAULT NULL,
    detail TEXT DEFAULT NULL,
    ip VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_portal_audit_user (user_id),
    KEY idx_portal_audit_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO portal_user (id, username, real_name, mobile, email, status, create_time, update_time)
VALUES (1, 'admin', '管理员', '13800000000', 'admin@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (1001, 'user', '普通用户', '13900000000', 'user@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
