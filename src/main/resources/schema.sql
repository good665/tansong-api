-- Applied on every startup (spring.sql.init.mode=always). Keep every statement idempotent.

CREATE TABLE IF NOT EXISTS subscriptions (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id        BIGINT       NOT NULL,
    plan               VARCHAR(20)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    current_period_end DATETIME     NOT NULL,
    toss_customer_key  VARCHAR(100) NULL,
    toss_billing_key   VARCHAR(200) NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_subscriptions_facility (facility_id),
    KEY idx_subscriptions_renewal (status, current_period_end)
);

CREATE TABLE IF NOT EXISTS subscription_payments (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id       BIGINT       NOT NULL,
    amount            INT          NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    pg_provider       VARCHAR(30)  NOT NULL,
    pg_transaction_id VARCHAR(100) NULL,
    failure_reason    VARCHAR(255) NULL,
    covered_until     DATETIME     NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_subscription_payments_facility (facility_id, id)
);

CREATE TABLE IF NOT EXISTS business_verifications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id     BIGINT       NOT NULL,
    representative_name VARCHAR(50)  NOT NULL,
    business_start_date DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    nts_status_code     VARCHAR(10)  NULL,
    message             VARCHAR(255) NULL,
    checked_at          DATETIME     NOT NULL,
    UNIQUE KEY uk_business_verifications_org (organization_id)
);
