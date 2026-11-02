-- ============================================================
-- 二手交易平台 · 业务库初始化脚本
-- 库：market  字符集：utf8mb4
-- 用法：mysql -uroot -p market < db/init.sql
-- 说明：可重复执行（IF NOT EXISTS + INSERT IGNORE）
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 用户表：买卖同账号，role 区分 USER / ADMIN
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL                COMMENT '登录名',
    password    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 加密后的密码',
    nickname    VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    avatar      VARCHAR(500) DEFAULT NULL            COMMENT '头像 URL',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER / ADMIN',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 正常 / 0 封禁',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 类目表：二级类目树，parent_id = 0 表示一级
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS category (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT      NOT NULL DEFAULT 0      COMMENT '父类目 ID，0 为一级',
    name        VARCHAR(50) NOT NULL                COMMENT '类目名',
    sort        INT         NOT NULL DEFAULT 0      COMMENT '排序，越小越前',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '类目表';

-- ------------------------------------------------------------
-- 3. 商品表：ai_category_id 与 category_id 分列，便于对比 AI 与人工
--    version 用于下单乐观锁，防超卖
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS item (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    seller_id        BIGINT        NOT NULL                COMMENT '卖家 ID',
    title            VARCHAR(200)  NOT NULL                COMMENT '标题',
    description      TEXT          DEFAULT NULL            COMMENT '描述',
    category_id      BIGINT        DEFAULT NULL            COMMENT '用户选择的类目',
    ai_category_id   BIGINT        DEFAULT NULL            COMMENT 'AI 识别的类目',
    price            DECIMAL(10,2) NOT NULL                COMMENT '价格',
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_AUDIT/ON_SALE/OFF_SALE/SOLD/REJECTED',
    ai_risk          VARCHAR(20)   DEFAULT NULL            COMMENT '机审风险等级 LOW/MEDIUM/HIGH',
    cover_url        VARCHAR(500)  DEFAULT NULL            COMMENT '封面图 URL',
    version          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status_category_time (status, category_id, create_time),
    KEY idx_seller (seller_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品表';

-- ------------------------------------------------------------
-- 4. 商品图片表：一个商品最多 5 张
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS item_image (
    id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id BIGINT       NOT NULL                COMMENT '商品 ID',
    url     VARCHAR(500) NOT NULL                COMMENT '原图 OSS Key / URL',
    sort    INT          NOT NULL DEFAULT 0      COMMENT '顺序，0 为封面',
    PRIMARY KEY (id),
    KEY idx_item (item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品图片表';

-- ------------------------------------------------------------
-- 5. 审核记录表：机审与人工分开落库
--    machine_result 与 result 的差集 = 规则缺口与误杀分析的数据源
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id          BIGINT       NOT NULL                COMMENT '商品 ID',
    type             VARCHAR(20)  NOT NULL                COMMENT 'MACHINE / MANUAL',
    machine_result   VARCHAR(20)  DEFAULT NULL            COMMENT '机审结论 PASS / REJECT',
    result           VARCHAR(20)  DEFAULT NULL            COMMENT '最终结论 PASS / REJECT',
    risk             VARCHAR(20)  DEFAULT NULL            COMMENT '风险等级',
    score            DECIMAL(4,2) DEFAULT NULL            COMMENT '风险分 0~1',
    reason           VARCHAR(500) DEFAULT NULL            COMMENT '驳回理由 / 说明',
    matched_rule_ids VARCHAR(255) DEFAULT NULL            COMMENT '命中的规则 ID，逗号分隔',
    rule_ver         VARCHAR(20)  DEFAULT NULL            COMMENT '规则库版本',
    operator_id      BIGINT       DEFAULT NULL            COMMENT '人工操作人，机审为 NULL',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    PRIMARY KEY (id),
    KEY idx_item (item_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审核记录表';

-- ------------------------------------------------------------
-- 6. 订单表：idem_key 唯一约束保证幂等
--    防超卖 = item.version 乐观锁 + 商品状态流转
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trade_order (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no    VARCHAR(32)   NOT NULL                COMMENT '订单号',
    item_id     BIGINT        NOT NULL                COMMENT '商品 ID',
    buyer_id    BIGINT        NOT NULL                COMMENT '买家 ID',
    seller_id   BIGINT        NOT NULL                COMMENT '卖家 ID',
    amount      DECIMAL(10,2) NOT NULL                COMMENT '成交金额',
    status      VARCHAR(20)   NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/SHIPPED/COMPLETED/CANCELLED',
    idem_key    VARCHAR(64)   NOT NULL                COMMENT '幂等键，前端提交时生成',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    pay_time    DATETIME      DEFAULT NULL            COMMENT '付款时间',
    finish_time DATETIME      DEFAULT NULL            COMMENT '完成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_idem_key (idem_key),
    KEY idx_item (item_id),
    KEY idx_buyer (buyer_id),
    KEY idx_seller (seller_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';

-- ------------------------------------------------------------
-- 7. 举报表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS report (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id       BIGINT       NOT NULL                COMMENT '被举报商品',
    reporter_id   BIGINT       NOT NULL                COMMENT '举报人',
    reason        VARCHAR(500) NOT NULL                COMMENT '举报理由',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / HANDLED / IGNORED',
    handle_result VARCHAR(500) DEFAULT NULL            COMMENT '处置结果',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    PRIMARY KEY (id),
    KEY idx_item (item_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '举报表';

-- ------------------------------------------------------------
-- 8. 图片下载审计表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS download_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id     BIGINT       NOT NULL                COMMENT '商品 ID',
    image_id    BIGINT       NOT NULL                COMMENT '图片 ID',
    user_id     BIGINT       DEFAULT NULL            COMMENT '下载人，游客为 NULL',
    ip          VARCHAR(64)  DEFAULT NULL            COMMENT 'IP',
    ua          VARCHAR(500) DEFAULT NULL            COMMENT 'User-Agent',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
    PRIMARY KEY (id),
    KEY idx_item (item_id),
    KEY idx_user_time (user_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图片下载审计表';

-- ------------------------------------------------------------
-- 9. AI 调用日志表：指标来源（采纳率、耗时、拒答率）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_call_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id     BIGINT      DEFAULT NULL            COMMENT '关联商品，找货时为 NULL',
    type        VARCHAR(20) NOT NULL                COMMENT 'MODERATE/CATEGORY/PRICE/DESCRIBE/QA/GUIDE',
    cost_ms     INT         DEFAULT NULL            COMMENT '耗时毫秒',
    tokens      INT         DEFAULT NULL            COMMENT '消耗 token',
    grounded    TINYINT     DEFAULT NULL            COMMENT '问答是否有依据 1/0',
    accepted    TINYINT     DEFAULT NULL            COMMENT '用户是否采纳 1/0',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_type (type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 调用日志表';

-- ------------------------------------------------------------
-- 初始化数据：二级类目
-- ------------------------------------------------------------
INSERT IGNORE INTO category (id, parent_id, name, sort) VALUES
(1, 0, '数码', 10),
(2, 1, '手机', 11),
(3, 1, '平板', 12),
(4, 1, '笔记本', 13),
(5, 1, '相机', 14),
(6, 1, '耳机音响', 15),
(10, 0, '家居', 20),
(11, 10, '家具', 21),
(12, 10, '厨具', 22),
(13, 10, '家用电器', 23),
(20, 0, '服饰', 30),
(21, 20, '服装', 31),
(22, 20, '鞋靴', 32),
(23, 20, '箱包', 33),
(30, 0, '图书', 40),
(31, 30, '教材教辅', 41),
(32, 30, '小说文学', 42),
(33, 30, '漫画绘本', 43),
(40, 0, '运动户外', 50),
(41, 40, '健身器材', 51),
(42, 40, '球类', 52),
(43, 40, '骑行装备', 53),
(50, 0, '其他', 99);