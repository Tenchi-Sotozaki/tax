-- 特別徴収義務者マスタ
CREATE TABLE special_collector (
    collector_id      VARCHAR(20)     NOT NULL,
    collector_name    VARCHAR(200)    NOT NULL,
    address           VARCHAR(500),
    phone_number      VARCHAR(20),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_special_collector PRIMARY KEY (collector_id)
);

-- 宿泊施設マスタ
CREATE TABLE accommodation_facility (
    facility_id       VARCHAR(20)     NOT NULL,
    collector_id      VARCHAR(20)     NOT NULL,
    facility_name     VARCHAR(200)    NOT NULL,
    address           VARCHAR(500),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_accommodation_facility PRIMARY KEY (facility_id),
    CONSTRAINT fk_facility_collector FOREIGN KEY (collector_id)
        REFERENCES special_collector(collector_id)
);

-- 税区分マスタ（①〜③）
CREATE TABLE tax_category (
    tax_category_code VARCHAR(2)      NOT NULL,
    category_name     VARCHAR(100)    NOT NULL,
    tax_amount        NUMERIC(10, 0)  NOT NULL,
    CONSTRAINT pk_tax_category PRIMARY KEY (tax_category_code)
);

-- 宿泊税納入申告ヘッダ
CREATE TABLE accommodation_tax_declaration (
    declaration_id        BIGSERIAL       NOT NULL,
    collector_id          VARCHAR(20)     NOT NULL,
    facility_id           VARCHAR(20)     NOT NULL,
    payment_year_month    VARCHAR(6)      NOT NULL,
    total_nights          INTEGER         NOT NULL DEFAULT 0,
    exempt_nights         INTEGER         NOT NULL DEFAULT 0,
    total_payment_amount  NUMERIC(15, 0)  NOT NULL DEFAULT 0,
    status                VARCHAR(10)     NOT NULL DEFAULT 'DRAFT',
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_declaration PRIMARY KEY (declaration_id),
    CONSTRAINT fk_declaration_collector FOREIGN KEY (collector_id)
        REFERENCES special_collector(collector_id),
    CONSTRAINT fk_declaration_facility FOREIGN KEY (facility_id)
        REFERENCES accommodation_facility(facility_id),
    CONSTRAINT uq_declaration UNIQUE (collector_id, facility_id, payment_year_month)
);

-- 宿泊税納入申告明細（税区分ごと）
CREATE TABLE accommodation_tax_declaration_detail (
    detail_id             BIGSERIAL       NOT NULL,
    declaration_id        BIGINT          NOT NULL,
    tax_category_code     VARCHAR(2)      NOT NULL,
    taxable_nights        INTEGER         NOT NULL DEFAULT 0,
    tax_amount_per_night  NUMERIC(10, 0)  NOT NULL,
    subtotal_amount       NUMERIC(15, 0)  NOT NULL DEFAULT 0,
    CONSTRAINT pk_declaration_detail PRIMARY KEY (detail_id),
    CONSTRAINT fk_detail_declaration FOREIGN KEY (declaration_id)
        REFERENCES accommodation_tax_declaration(declaration_id),
    CONSTRAINT fk_detail_tax_category FOREIGN KEY (tax_category_code)
        REFERENCES tax_category(tax_category_code)
);

-- 初期税区分データ
INSERT INTO tax_category (tax_category_code, category_name, tax_amount) VALUES
    ('01', '税区分①（〜7,000円未満）',         200),
    ('02', '税区分②（7,000円〜15,000円未満）', 500),
    ('03', '税区分③（15,000円以上）',          1000);
