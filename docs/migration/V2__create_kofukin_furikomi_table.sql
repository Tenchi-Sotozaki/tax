------------------------------------------------------------------------
-- 交付金振込情報テーブル作成
------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_kofukin_furikomi (
  jichitai_cd char(5) NOT NULL,
  shitei_no char(8) NOT NULL,
  taisho_ym char(6) NOT NULL,
  rno numeric(3) NOT NULL,
  toroku_ymd date,
  furikomi_ymd date,
  furikomi_gaku numeric(14),
  shiharai_gaku numeric(14),
  tesuryo numeric(10),
  furikomi_kbn char(1),
  furikomi_status char(1),
  ginko_cd char(4),
  ginko_name varchar(100),
  shiten_cd char(3),
  shiten_name varchar(100),
  yokin_shubetsu char(1),
  koza_no char(8),
  koza_meigi varchar(100),
  biko varchar(400),
  new_flg char(1) NOT NULL DEFAULT '1',
  del_flg char(1) NOT NULL DEFAULT '0',
  add_dt timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  add_user varchar(20) NOT NULL DEFAULT 'system',
  upd_dt timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  upd_user varchar(20) NOT NULL DEFAULT 'system',
  version integer NOT NULL DEFAULT 1,
  CONSTRAINT t_kofukin_furikomi_pkey PRIMARY KEY (jichitai_cd, shitei_no, taisho_ym, rno)
);

COMMENT ON TABLE t_kofukin_furikomi IS '交付金振込情報';
COMMENT ON COLUMN t_kofukin_furikomi.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_kofukin_furikomi.shitei_no IS '指定番号';
COMMENT ON COLUMN t_kofukin_furikomi.taisho_ym IS '対象年月';
COMMENT ON COLUMN t_kofukin_furikomi.rno IS '履歴番号';
COMMENT ON COLUMN t_kofukin_furikomi.toroku_ymd IS '登録年月日';
COMMENT ON COLUMN t_kofukin_furikomi.furikomi_ymd IS '振込年月日';
COMMENT ON COLUMN t_kofukin_furikomi.furikomi_gaku IS '振込金額';
COMMENT ON COLUMN t_kofukin_furikomi.shiharai_gaku IS '支払金額';
COMMENT ON COLUMN t_kofukin_furikomi.tesuryo IS '手数料';
COMMENT ON COLUMN t_kofukin_furikomi.furikomi_kbn IS '振込区分';
COMMENT ON COLUMN t_kofukin_furikomi.furikomi_status IS '振込状況';
COMMENT ON COLUMN t_kofukin_furikomi.ginko_cd IS '銀行コード';
COMMENT ON COLUMN t_kofukin_furikomi.ginko_name IS '銀行名';
COMMENT ON COLUMN t_kofukin_furikomi.shiten_cd IS '支店コード';
COMMENT ON COLUMN t_kofukin_furikomi.shiten_name IS '支店名';
COMMENT ON COLUMN t_kofukin_furikomi.yokin_shubetsu IS '預金種別';
COMMENT ON COLUMN t_kofukin_furikomi.koza_no IS '口座番号';
COMMENT ON COLUMN t_kofukin_furikomi.koza_meigi IS '口座名義';
COMMENT ON COLUMN t_kofukin_furikomi.biko IS '備考';
COMMENT ON COLUMN t_kofukin_furikomi.new_flg IS '最新フラグ';
COMMENT ON COLUMN t_kofukin_furikomi.del_flg IS '削除フラグ';
COMMENT ON COLUMN t_kofukin_furikomi.add_dt IS '作成日時';
COMMENT ON COLUMN t_kofukin_furikomi.add_user IS '作成者';
COMMENT ON COLUMN t_kofukin_furikomi.upd_dt IS '更新日時';
COMMENT ON COLUMN t_kofukin_furikomi.upd_user IS '更新者';
COMMENT ON COLUMN t_kofukin_furikomi.version IS 'バージョン';