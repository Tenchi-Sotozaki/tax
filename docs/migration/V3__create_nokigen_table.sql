------------------------------------------------------------------------
-- 納入期限マスタ作成
------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS m_nokigen (
  jichitai_cd char(5) NOT NULL,
  nendo char(4) NOT NULL,
  nokigen_1st char(8) NOT NULL,
  nokigen_2nd char(8) NOT NULL,
  nokigen_3rd char(8) NOT NULL,
  nokigen_4th char(8) NOT NULL,
  nokigen_5th char(8) NOT NULL,
  nokigen_6th char(8) NOT NULL,
  nokigen_7th char(8) NOT NULL,
  nokigen_8th char(8) NOT NULL,
  nokigen_9th char(8) NOT NULL,
  nokigen_10th char(8) NOT NULL,
  nokigen_11th char(8) NOT NULL,
  nokigen_12th char(8) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version integer NOT NULL,
  CONSTRAINT m_nokigen_pkey PRIMARY KEY (jichitai_cd, nendo)
);
COMMENT ON TABLE m_nokigen IS '納入期限マスタ';
COMMENT ON COLUMN m_nokigen.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN m_nokigen.nendo IS '年度';
COMMENT ON COLUMN m_nokigen.nokigen_1st IS '1期納期';
COMMENT ON COLUMN m_nokigen.nokigen_2nd IS '2期納期';
COMMENT ON COLUMN m_nokigen.nokigen_3rd IS '3期納期';
COMMENT ON COLUMN m_nokigen.nokigen_4th IS '4期納期';
COMMENT ON COLUMN m_nokigen.nokigen_5th IS '5期納期';
COMMENT ON COLUMN m_nokigen.nokigen_6th IS '6期納期';
COMMENT ON COLUMN m_nokigen.nokigen_7th IS '7期納期';
COMMENT ON COLUMN m_nokigen.nokigen_8th IS '8期納期';
COMMENT ON COLUMN m_nokigen.nokigen_9th IS '9期納期';
COMMENT ON COLUMN m_nokigen.nokigen_10th IS '10期納期';
COMMENT ON COLUMN m_nokigen.nokigen_11th IS '11期納期';
COMMENT ON COLUMN m_nokigen.nokigen_12th IS '12期納期';
COMMENT ON COLUMN m_nokigen.add_dt IS '作成日時';
COMMENT ON COLUMN m_nokigen.add_user IS '作成者';
COMMENT ON COLUMN m_nokigen.upd_dt IS '更新日時';
COMMENT ON COLUMN m_nokigen.upd_user IS '更新者';
COMMENT ON COLUMN m_nokigen.version IS 'バージョン';
