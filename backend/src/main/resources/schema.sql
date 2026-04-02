CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL DEFAULT '15138114047',
    nickname VARCHAR(30) DEFAULT NULL,
    avatar_path VARCHAR(255) DEFAULT NULL,
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

CREATE TABLE IF NOT EXISTS repair_order_flow_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_id BIGINT NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_repair_order_flow_image_flow (flow_id),
    CONSTRAINT fk_repair_order_flow_image_flow FOREIGN KEY (flow_id) REFERENCES repair_order_flow(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS service_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS convenience_service (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) DEFAULT NULL,
    cover_image_path VARCHAR(255) DEFAULT NULL,
    service_status VARCHAR(20) NOT NULL DEFAULT 'RESERVABLE',
    audit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    audit_reason VARCHAR(255) DEFAULT NULL,
    reviewed_by BIGINT DEFAULT NULL,
    reviewed_at DATETIME DEFAULT NULL,
    max_capacity INT NOT NULL DEFAULT 50,
    current_booked INT NOT NULL DEFAULT 0,
    avg_score DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    score_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_convenience_service_provider (provider_id),
    INDEX idx_convenience_service_audit_status (audit_status),
    INDEX idx_convenience_service_service_status (service_status),
    INDEX idx_convenience_service_category (category_code),
    CONSTRAINT fk_convenience_service_provider FOREIGN KEY (provider_id) REFERENCES sys_user(id),
    CONSTRAINT fk_convenience_service_reviewer FOREIGN KEY (reviewed_by) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS service_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_service_image_service_id (service_id),
    CONSTRAINT fk_service_image_service FOREIGN KEY (service_id) REFERENCES convenience_service(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS service_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    from_audit_status VARCHAR(20) NOT NULL,
    to_audit_status VARCHAR(20) NOT NULL,
    action VARCHAR(20) NOT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    reviewer_id BIGINT NOT NULL,
    reviewer_name VARCHAR(50) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_service_audit_log_service_id (service_id),
    CONSTRAINT fk_service_audit_log_service FOREIGN KEY (service_id) REFERENCES convenience_service(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS service_booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_service_booking_service_id (service_id),
    INDEX idx_service_booking_user_id (user_id),
    INDEX idx_service_booking_status (status),
    CONSTRAINT fk_service_booking_service FOREIGN KEY (service_id) REFERENCES convenience_service(id) ON DELETE CASCADE,
    CONSTRAINT fk_service_booking_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS service_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    content VARCHAR(500) DEFAULT NULL,
    reviewer_name VARCHAR(50) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_service_review_unique (service_id, user_id),
    INDEX idx_service_review_service_id (service_id),
    CONSTRAINT fk_service_review_service FOREIGN KEY (service_id) REFERENCES convenience_service(id) ON DELETE CASCADE,
    CONSTRAINT fk_service_review_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS electricity_payment_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    merchant_order_no VARCHAR(64) NOT NULL UNIQUE,
    out_trade_no VARCHAR(64) NOT NULL UNIQUE,
    trade_no VARCHAR(64) DEFAULT NULL,
    ebpp_alipay_order_no VARCHAR(64) DEFAULT NULL,
    ebpp_order_status VARCHAR(32) DEFAULT NULL,
    charge_inst VARCHAR(64) NOT NULL,
    bill_key VARCHAR(64) NOT NULL,
    owner_name VARCHAR(50) DEFAULT NULL,
    order_type VARCHAR(20) NOT NULL DEFAULT 'JF',
    sub_order_type VARCHAR(20) NOT NULL DEFAULT 'ELEC',
    pay_amount DECIMAL(10,2) NOT NULL,
    service_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    qr_code VARCHAR(1024) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    raw_message VARCHAR(500) DEFAULT NULL,
    paid_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_electricity_order_user (user_id),
    INDEX idx_electricity_order_status (status),
    CONSTRAINT fk_electricity_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS social_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_social_post_user (user_id),
    INDEX idx_social_post_created_at (created_at),
    CONSTRAINT fk_social_post_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS social_post_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_social_post_image_post (post_id),
    CONSTRAINT fk_social_post_image_post FOREIGN KEY (post_id) REFERENCES social_post(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS social_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    reply_to_user_id BIGINT DEFAULT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_social_comment_post (post_id),
    INDEX idx_social_comment_parent (parent_id),
    INDEX idx_social_comment_user (user_id),
    CONSTRAINT fk_social_comment_post FOREIGN KEY (post_id) REFERENCES social_post(id) ON DELETE CASCADE,
    CONSTRAINT fk_social_comment_parent FOREIGN KEY (parent_id) REFERENCES social_comment(id) ON DELETE CASCADE,
    CONSTRAINT fk_social_comment_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_social_comment_reply_user FOREIGN KEY (reply_to_user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS social_comment_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_social_comment_image_comment (comment_id),
    CONSTRAINT fk_social_comment_image_comment FOREIGN KEY (comment_id) REFERENCES social_comment(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS friend_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_friend_pair (user_id, friend_id),
    INDEX idx_friend_relation_user (user_id),
    CONSTRAINT fk_friend_relation_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_friend_relation_friend FOREIGN KEY (friend_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS friend_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message VARCHAR(255) DEFAULT NULL,
    handled_by BIGINT DEFAULT NULL,
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_friend_request_pending (requester_id, target_user_id, status),
    INDEX idx_friend_request_target_status (target_user_id, status),
    INDEX idx_friend_request_requester_status (requester_id, status),
    CONSTRAINT fk_friend_request_requester FOREIGN KEY (requester_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_friend_request_target FOREIGN KEY (target_user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_friend_request_handler FOREIGN KEY (handled_by) REFERENCES sys_user(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS chat_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    owner_id BIGINT NOT NULL,
    announcement VARCHAR(500) DEFAULT NULL,
    announcement_version BIGINT NOT NULL DEFAULT 0,
    announcement_updated_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_chat_group_owner (owner_id),
    CONSTRAINT fk_chat_group_owner FOREIGN KEY (owner_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS chat_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    muted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_group_member (group_id, user_id),
    INDEX idx_chat_group_member_group (group_id),
    INDEX idx_chat_group_member_user (user_id),
    CONSTRAINT fk_chat_group_member_group FOREIGN KEY (group_id) REFERENCES chat_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_group_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_group_announcement_ack (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    announcement_version BIGINT NOT NULL,
    acked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_group_announcement_ack (group_id, user_id, announcement_version),
    INDEX idx_chat_group_announcement_ack_group (group_id),
    INDEX idx_chat_group_announcement_ack_user (user_id),
    CONSTRAINT fk_chat_group_announcement_ack_group FOREIGN KEY (group_id) REFERENCES chat_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_group_announcement_ack_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_type VARCHAR(20) NOT NULL,
    receiver_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    image_paths VARCHAR(2000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_message_receiver (receiver_type, receiver_id, created_at),
    INDEX idx_chat_message_sender (sender_id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user(id)
);
