CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL DEFAULT '15138114047',
    nickname VARCHAR(30) DEFAULT NULL,
    role VARCHAR(20) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS repair_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    handler_id BIGINT DEFAULT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    contact_phone VARCHAR(20) DEFAULT NULL,
    status VARCHAR(40) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_repair_order_user_id (user_id),
    INDEX idx_repair_order_handler_id (handler_id),
    INDEX idx_repair_order_status (status),
    CONSTRAINT fk_repair_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_repair_order_handler FOREIGN KEY (handler_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS repair_order_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_repair_order_image_order_id (order_id),
    CONSTRAINT fk_repair_order_image_order FOREIGN KEY (order_id) REFERENCES repair_order(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS repair_order_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(40) DEFAULT NULL,
    to_status VARCHAR(40) NOT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    operator_id BIGINT DEFAULT NULL,
    operator_name VARCHAR(50) DEFAULT NULL,
    operator_role VARCHAR(20) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_repair_order_flow_order_id (order_id),
    CONSTRAINT fk_repair_order_flow_order FOREIGN KEY (order_id) REFERENCES repair_order(id) ON DELETE CASCADE
);
