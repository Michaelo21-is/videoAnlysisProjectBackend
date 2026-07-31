-- =====================================================================
-- purchase_db - baseline schema (owned by purchaseService)
--
-- Entities covered:
--   com.moj.purchaseservice.Entity.OrderCredit           -> order_credit
--   com.moj.purchaseservice.Entity.OrderAnalyzeContents  -> order_analyze_content
--   com.moj.purchaseservice.Entity.SubscriptionHistory   -> subscription_history
--
-- user_id is a plain uuid column, not a foreign key: the users table lives
-- in auth_db and the two databases stay separate.
--
-- Every sequence uses INCREMENT BY 1 to match allocationSize = 1 on the
-- corresponding @SequenceGenerator.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Sequences
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS order_credit_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS order_analyze_content_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS subscription_history_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    NO CYCLE;

-- ---------------------------------------------------------------------
-- order_credit (credit top-up orders)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_credit
(
    id           bigint                      NOT NULL,
    credit       bigint                      NOT NULL,
    price_in_usd numeric(38, 2)              NOT NULL,
    status       varchar(255)                NOT NULL,
    purchased_at timestamp(6) with time zone,
    user_id      uuid                        NOT NULL,
    CONSTRAINT pk_order_credit PRIMARY KEY (id),
    CONSTRAINT ck_order_credit_status CHECK (status IN (
        'PENDING',
        'PAYMENT_FAILED',
        'SERVER_FAILED',
        'FAILED_TO_ADD_CREDIT',
        'PURCHASED',
        'SUCCEED'
    ))
);

-- ---------------------------------------------------------------------
-- order_analyze_content (content-analysis orders)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_analyze_content
(
    id             bigint                      NOT NULL,
    user_id        uuid                        NOT NULL,
    sum_of_content varchar(255)                NOT NULL,
    content_type   varchar(255)                NOT NULL,
    credit_cost    bigint                      NOT NULL,
    purchased_at   timestamp(6) with time zone,
    status         varchar(255)                NOT NULL,
    CONSTRAINT pk_order_analyze_content PRIMARY KEY (id),
    CONSTRAINT ck_order_analyze_content_sum_of_content CHECK (sum_of_content IN ('THREE', 'FIVE', 'EIGHT')),
    CONSTRAINT ck_order_analyze_content_content_type CHECK (content_type IN ('TEXT', 'IMAGE', 'VIDEO')),
    CONSTRAINT ck_order_analyze_content_status CHECK (status IN (
        'PENDING',
        'PAYMENT_FAILED',
        'SERVER_FAILED',
        'PURCHASED',
        'PICKED_UP_VIDEOS',
        'ANALYZE_VIDEOS_COMPLETED',
        'COMPLETED'
    ))
);

-- ---------------------------------------------------------------------
-- subscription_history
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_history
(
    id                bigint       NOT NULL,
    user_id           uuid,
    subscription_type varchar(255) NOT NULL,
    purchased_at      timestamp(6) with time zone,
    CONSTRAINT pk_subscription_history PRIMARY KEY (id),
    CONSTRAINT ck_subscription_history_type CHECK (subscription_type IN ('BASE', 'PRO', 'AGENCY'))
);

-- ---------------------------------------------------------------------
-- Indexes
--
-- Every read path in PurchaseService / PaddleWebHookService filters by
-- user_id, usually together with status, so index those.
-- ---------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_order_credit_user_id
    ON order_credit (user_id);

CREATE INDEX IF NOT EXISTS idx_order_credit_user_id_status
    ON order_credit (user_id, status);

CREATE INDEX IF NOT EXISTS idx_order_analyze_content_user_id
    ON order_analyze_content (user_id);

CREATE INDEX IF NOT EXISTS idx_order_analyze_content_user_id_status
    ON order_analyze_content (user_id, status);

CREATE INDEX IF NOT EXISTS idx_subscription_history_user_id
    ON subscription_history (user_id);
