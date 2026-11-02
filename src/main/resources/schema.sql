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

CREATE TABLE IF NOT EXISTS billing_cards (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id          BIGINT       NOT NULL,
    customer_key         VARCHAR(64)  NULL,
    pending_customer_key VARCHAR(64)  NULL,
    billing_key          VARCHAR(200) NULL,
    card_summary         VARCHAR(100) NULL,
    registered_at        DATETIME     NULL,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_billing_cards_facility (facility_id),
    UNIQUE KEY uk_billing_cards_pending (pending_customer_key)
);

-- 공공데이터로 들여온 시설 ↔ 원본 기관코드 (중복 방지·출처 추적)
CREATE TABLE IF NOT EXISTS public_facility_sources (
    facility_id BIGINT      NOT NULL,
    source      VARCHAR(20) NOT NULL,
    external_id VARCHAR(30) NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_public_facility_sources_external (source, external_id),
    UNIQUE KEY uk_public_facility_sources_facility (facility_id)
);

-- 광고 시설 추가 정보: 도면도, 방 종류, 방별 거래 유형 가격
CREATE TABLE IF NOT EXISTS facility_floor_plans (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id BIGINT       NOT NULL,
    file_url    VARCHAR(500) NOT NULL,
    file_type   VARCHAR(10)  NOT NULL,
    label       VARCHAR(30)  NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_facility_floor_plans_facility (facility_id),
    CONSTRAINT fk_facility_floor_plans_facility FOREIGN KEY (facility_id) REFERENCES facilities (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS facility_rooms (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id BIGINT        NOT NULL,
    room_type   VARCHAR(50)   NOT NULL,
    area_sqm    DECIMAL(6, 2) NULL,
    KEY idx_facility_rooms_facility (facility_id),
    CONSTRAINT fk_facility_rooms_facility FOREIGN KEY (facility_id) REFERENCES facilities (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS facility_room_prices (
    room_id   BIGINT      NOT NULL,
    deal_type VARCHAR(10) NOT NULL,
    deposit   BIGINT      NULL,
    amount    BIGINT      NULL,
    UNIQUE KEY uk_facility_room_prices (room_id, deal_type),
    CONSTRAINT fk_facility_room_prices_room FOREIGN KEY (room_id) REFERENCES facility_rooms (id) ON DELETE CASCADE
);

-- 소셜 로그인: 제공자 계정 ↔ 사용자 연결, 일회용 표(state, 로그인 코드. 해시만 저장)
CREATE TABLE IF NOT EXISTS user_social_accounts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(191) NOT NULL,
    user_id          BIGINT       NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_social_accounts (provider, provider_user_id),
    CONSTRAINT fk_user_social_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_tickets (
    token_hash   CHAR(64)     NOT NULL PRIMARY KEY,
    kind         VARCHAR(10)  NOT NULL,
    provider     VARCHAR(20)  NOT NULL,
    redirect_uri VARCHAR(255) NOT NULL,
    user_id      BIGINT       NULL,
    expires_at   DATETIME     NOT NULL,
    KEY idx_oauth_tickets_expires (expires_at)
);
