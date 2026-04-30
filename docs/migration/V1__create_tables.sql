------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_tokugimu (
  jichitai_cd char(5) NOT NULL,
  shitei_no char(8) NOT NULL,
  rno numeric(3) NOT NULL,
  toroku_ymd date NOT NULL,
  shinkoku_ymd date NOT NULL,
  henko_ymd date NOT NULL,
  atena_no numeric(15) NOT NULL,
  shisetsu_name varchar(200) NOT NULL,
  shisetsu_name_kana varchar(200) NOT NULL,
  shisetsu_yubin_no varchar(10),
  shisetsu_jusho varchar(200),
  shisetsu_tel varchar(20),
  yuka_menseki numeric(9, 2),
  chijo_kai numeric(3),
  chika_kai numeric(2),
  kyakushitsu_su numeric(5),
  shuyo_su numeric(7),
  kyoka_name varchar(200) NOT NULL,
  kyoka_name_kana varchar(200) NOT NULL,
  kyoka_yubin_no varchar(10),
  kyoka_jusho varchar(200),
  kyoka_tel varchar(20),
  kyoka_shu char(1),
  kyoka_no varchar(200),
  soufusaki_name varchar(200) NOT NULL,
  soufusaki_name_kana varchar(200) NOT NULL,
  soufusaki_yubin_no varchar(10),
  soufusaki_jusho varchar(200),
  soufusaki_tel varchar(20),
  biko varchar(400),
  eigyo_st_ymd date NOT NULL,
  eigyo_ed_ymd date,
  kyushi_st_ymd date,
  kyushi_ed_ymd date,
  kyuhaishi_riyu varchar(400),
  eltax_umu char(1),
  nokigen numeric(3),
  new_flg char(1) NOT NULL,
  del_flg char(1) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_tokugimu_pkey PRIMARY KEY (jichitai_cd, shitei_no, rno)
);
COMMENT ON TABLE t_tokugimu IS '特別徴収義務者情報';
COMMENT ON COLUMN t_tokugimu.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_tokugimu.shitei_no IS '指定番号';
COMMENT ON COLUMN t_tokugimu.rno IS '履歴番号';
COMMENT ON COLUMN t_tokugimu.toroku_ymd IS '登録年月日';
COMMENT ON COLUMN t_tokugimu.shinkoku_ymd IS '申告年月日';
COMMENT ON COLUMN t_tokugimu.henko_ymd IS '変更年月日';
COMMENT ON COLUMN t_tokugimu.atena_no IS '宛名番号';
COMMENT ON COLUMN t_tokugimu.shisetsu_name IS '施設名称';
COMMENT ON COLUMN t_tokugimu.shisetsu_name_kana IS '施設名称かな';
COMMENT ON COLUMN t_tokugimu.shisetsu_yubin_no IS '施設郵便番号';
COMMENT ON COLUMN t_tokugimu.shisetsu_jusho IS '施設所在地';
COMMENT ON COLUMN t_tokugimu.shisetsu_tel IS '施設電話番号';
COMMENT ON COLUMN t_tokugimu.yuka_menseki IS '施設床面積';
COMMENT ON COLUMN t_tokugimu.chijo_kai IS '施設地上階数';
COMMENT ON COLUMN t_tokugimu.chika_kai IS '施設地下階数';
COMMENT ON COLUMN t_tokugimu.kyakushitsu_su IS '施設客室数';
COMMENT ON COLUMN t_tokugimu.shuyo_su IS '施設収容人員数';
COMMENT ON COLUMN t_tokugimu.kyoka_name IS '営業許可氏名';
COMMENT ON COLUMN t_tokugimu.kyoka_name_kana IS '営業許可氏名かな';
COMMENT ON COLUMN t_tokugimu.kyoka_yubin_no IS '営業許可郵便番号';
COMMENT ON COLUMN t_tokugimu.kyoka_jusho IS '営業許可住所';
COMMENT ON COLUMN t_tokugimu.kyoka_tel IS '営業許可電話番号';
COMMENT ON COLUMN t_tokugimu.kyoka_shu IS '営業許可種別';
COMMENT ON COLUMN t_tokugimu.kyoka_no IS '営業許可等番号';
COMMENT ON COLUMN t_tokugimu.soufusaki_name IS '送付先氏名';
COMMENT ON COLUMN t_tokugimu.soufusaki_name_kana IS '送付先氏名かな';
COMMENT ON COLUMN t_tokugimu.soufusaki_yubin_no IS '送付先郵便番号';
COMMENT ON COLUMN t_tokugimu.soufusaki_jusho IS '送付先住所';
COMMENT ON COLUMN t_tokugimu.soufusaki_tel IS '送付先電話番号';
COMMENT ON COLUMN t_tokugimu.biko IS '申請備考';
COMMENT ON COLUMN t_tokugimu.eigyo_st_ymd IS '営業開始年月日';
COMMENT ON COLUMN t_tokugimu.eigyo_ed_ymd IS '営業終了年月日';
COMMENT ON COLUMN t_tokugimu.kyushi_st_ymd IS '休止開始年月日';
COMMENT ON COLUMN t_tokugimu.kyushi_ed_ymd IS '休止終了年月日';
COMMENT ON COLUMN t_tokugimu.kyuhaishi_riyu IS '休廃止理由';
COMMENT ON COLUMN t_tokugimu.eltax_umu IS 'eLTAX利用有無';
COMMENT ON COLUMN t_tokugimu.nokigen IS '納税周期選択';
COMMENT ON COLUMN t_tokugimu.new_flg IS '最新フラグ';
COMMENT ON COLUMN t_tokugimu.del_flg IS '削除フラグ';
COMMENT ON COLUMN t_tokugimu.add_dt IS '作成日時';
COMMENT ON COLUMN t_tokugimu.add_user IS '作成者';
COMMENT ON COLUMN t_tokugimu.upd_dt IS '更新日時';
COMMENT ON COLUMN t_tokugimu.upd_user IS '更新者';
COMMENT ON COLUMN t_tokugimu.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_shoyusha (
  jichitai_cd char(5) NOT NULL,
  shitei_no char(8) NOT NULL,
  rno numeric(3) NOT NULL,
  idx numeric(3) NOT NULL,
  shoyusha_name varchar(200) NOT NULL,
  shoyusha_name_kana varchar(200) NOT NULL,
  shoyusha_yubin_no varchar(10),
  shoyusha_jusho varchar(200),
  shoyusha_tel varchar(20),
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_shoyusha_pkey PRIMARY KEY (jichitai_cd, shitei_no, rno, idx)
);
COMMENT ON TABLE t_shoyusha IS '宿泊施設所有者情報';
COMMENT ON COLUMN t_shoyusha.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_shoyusha.shitei_no IS '指定番号';
COMMENT ON COLUMN t_shoyusha.rno IS '履歴番号';
COMMENT ON COLUMN t_shoyusha.idx IS '同一施設所有者連番';
COMMENT ON COLUMN t_shoyusha.shoyusha_name IS '施設所有者名称';
COMMENT ON COLUMN t_shoyusha.shoyusha_name_kana IS '施設所有者名称かな';
COMMENT ON COLUMN t_shoyusha.shoyusha_yubin_no IS '施設所有者郵便番号';
COMMENT ON COLUMN t_shoyusha.shoyusha_jusho IS '施設所有者住所';
COMMENT ON COLUMN t_shoyusha.shoyusha_tel IS '施設所有者電話番号';
COMMENT ON COLUMN t_shoyusha.add_dt IS '作成日時';
COMMENT ON COLUMN t_shoyusha.add_user IS '作成者';
COMMENT ON COLUMN t_shoyusha.upd_dt IS '更新日時';
COMMENT ON COLUMN t_shoyusha.upd_user IS '更新者';
COMMENT ON COLUMN t_shoyusha.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_nokan (
  jichitai_cd char(5) NOT NULL,
  shitei_no char(8) NOT NULL,
  rno numeric(3) NOT NULL,
  menjo_kbn char(1) NOT NULL,
  toroku_ymd date NOT NULL,
  shinkoku_ymd date NOT NULL,
  atena_no char(15),
  name varchar(200),
  name_kana varchar(200),
  yubin_no varchar(10),
  jusho varchar(200),
  tel varchar(20),
  menjo_riyu varchar(400),
  new_flg char(1) NOT NULL,
  del_flg char(1) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_nokan_pkey PRIMARY KEY (jichitai_cd, shitei_no, rno)
);
COMMENT ON TABLE t_nokan IS '納税管理人情報';
COMMENT ON COLUMN t_nokan.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_nokan.shitei_no IS '指定番号';
COMMENT ON COLUMN t_nokan.rno IS '履歴番号';
COMMENT ON COLUMN t_nokan.menjo_kbn IS '選任免除区分';
COMMENT ON COLUMN t_nokan.toroku_ymd IS '登録年月日';
COMMENT ON COLUMN t_nokan.shinkoku_ymd IS '申告年月日';
COMMENT ON COLUMN t_nokan.atena_no IS '納税管理人宛名番号';
COMMENT ON COLUMN t_nokan.name IS '納税管理人名称';
COMMENT ON COLUMN t_nokan.name_kana IS '納税管理人名称かな';
COMMENT ON COLUMN t_nokan.yubin_no IS '納税管理人郵便番号';
COMMENT ON COLUMN t_nokan.jusho IS '納税管理人住所';
COMMENT ON COLUMN t_nokan.tel IS '納税管理人電話番号';
COMMENT ON COLUMN t_nokan.menjo_riyu IS '専任免除理由';
COMMENT ON COLUMN t_nokan.new_flg IS '最新フラグ';
COMMENT ON COLUMN t_nokan.del_flg IS '削除フラグ';
COMMENT ON COLUMN t_nokan.add_dt IS '作成日時';
COMMENT ON COLUMN t_nokan.add_user IS '作成者';
COMMENT ON COLUMN t_nokan.upd_dt IS '更新日時';
COMMENT ON COLUMN t_nokan.upd_user IS '更新者';
COMMENT ON COLUMN t_nokan.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_gassan (
  jichitai_cd char(5) NOT NULL,
  gassan_shitei_no char(8) NOT NULL,
  rno numeric(3) NOT NULL,
  toroku_ymd date NOT NULL,
  shinkoku_ymd date NOT NULL,
  atena_no numeric(15) NOT NULL,
  tekiyo_st_ymd date NOT NULL,
  tekiyo_ed_ymd date,
  new_flg char(1) NOT NULL,
  del_flg char(1) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_gassan_pkey PRIMARY KEY (jichitai_cd, gassan_shitei_no, rno)
);
COMMENT ON TABLE t_gassan IS '合算申告納入情報';
COMMENT ON COLUMN t_gassan.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_gassan.gassan_shitei_no IS '合算指定番号';
COMMENT ON COLUMN t_gassan.rno IS '履歴番号';
COMMENT ON COLUMN t_gassan.toroku_ymd IS '登録年月日';
COMMENT ON COLUMN t_gassan.shinkoku_ymd IS '申告年月日';
COMMENT ON COLUMN t_gassan.atena_no IS '宛名番号';
COMMENT ON COLUMN t_gassan.tekiyo_st_ymd IS '適用開始年月日';
COMMENT ON COLUMN t_gassan.tekiyo_ed_ymd IS '適用終了年月日';
COMMENT ON COLUMN t_gassan.new_flg IS '最新フラグ';
COMMENT ON COLUMN t_gassan.del_flg IS '削除フラグ';
COMMENT ON COLUMN t_gassan.add_dt IS '作成日時';
COMMENT ON COLUMN t_gassan.add_user IS '作成者';
COMMENT ON COLUMN t_gassan.upd_dt IS '更新日時';
COMMENT ON COLUMN t_gassan.upd_user IS '更新者';
COMMENT ON COLUMN t_gassan.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_gassan_uchi (
  jichitai_cd char(5) NOT NULL,
  gassan_shitei_no char(8) NOT NULL,
  rno numeric(3) NOT NULL,
  shitei_no char(8) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_gassan_uchi_pkey PRIMARY KEY (jichitai_cd, gassan_shitei_no, rno, shitei_no)
);
COMMENT ON TABLE t_gassan_uchi IS '合算申告納入内訳情報';
COMMENT ON COLUMN t_gassan_uchi.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_gassan_uchi.gassan_shitei_no IS '合算指定番号';
COMMENT ON COLUMN t_gassan_uchi.rno IS '履歴番号';
COMMENT ON COLUMN t_gassan_uchi.shitei_no IS '指定番号';
COMMENT ON COLUMN t_gassan_uchi.add_dt IS '作成日時';
COMMENT ON COLUMN t_gassan_uchi.add_user IS '作成者';
COMMENT ON COLUMN t_gassan_uchi.upd_dt IS '更新日時';
COMMENT ON COLUMN t_gassan_uchi.upd_user IS '更新者';
COMMENT ON COLUMN t_gassan_uchi.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_fuka (
  jichitai_cd char(5) NOT NULL,
  shitei_no char(8) NOT NULL,
  rno numeric(3) NOT NULL,
  nendo char(4) NOT NULL,
  kibetsu numeric(2) NOT NULL,
  toroku_ymd date NOT NULL,
  shinkoku_ymd date NOT NULL,
  taisho_ym char(6) NOT NULL,
  fuka_kbn char(1) NOT NULL,
  henko_kbn char(1) NOT NULL,
  henko_riyu varchar(400),
  kazei_hakusu numeric(9),
  kazei_ryokin numeric(14),
  zeigaku numeric(14),
  menjo_hakusu numeric(9),
  menjo_ryokin numeric(14),
  total_hakusu numeric(10) NOT NULL,
  total_zeigaku numeric(14) NOT NULL,
  city_zeigaku numeric(14) NOT NULL,
  ken_zeigaku numeric(14) NOT NULL,
  kasan_kbn char(1),
  kasan_ritsu numeric(5, 2),
  kasan_gaku numeric(13),
  nokigen date,
  new_flg char(1) NOT NULL,
  del_flg char(1) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_fuka_pkey PRIMARY KEY (jichitai_cd, shitei_no, rno, nendo, kibetsu)
);
COMMENT ON TABLE t_fuka IS '賦課情報';
COMMENT ON COLUMN t_fuka.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_fuka.shitei_no IS '指定番号';
COMMENT ON COLUMN t_fuka.rno IS '履歴番号';
COMMENT ON COLUMN t_fuka.nendo IS '賦課年度';
COMMENT ON COLUMN t_fuka.kibetsu IS '期別';
COMMENT ON COLUMN t_fuka.toroku_ymd IS '登録年月日';
COMMENT ON COLUMN t_fuka.shinkoku_ymd IS '申告年月日';
COMMENT ON COLUMN t_fuka.taisho_ym IS '対象年月';
COMMENT ON COLUMN t_fuka.fuka_kbn IS '賦課方式';
COMMENT ON COLUMN t_fuka.henko_kbn IS '変更区分';
COMMENT ON COLUMN t_fuka.henko_riyu IS '変更理由';
COMMENT ON COLUMN t_fuka.kazei_hakusu IS '課税対象宿泊数';
COMMENT ON COLUMN t_fuka.kazei_ryokin IS '課税対象宿泊料金';
COMMENT ON COLUMN t_fuka.zeigaku IS '課税対象税額';
COMMENT ON COLUMN t_fuka.menjo_hakusu IS '課税免除宿泊数';
COMMENT ON COLUMN t_fuka.menjo_ryokin IS '課税免除宿泊料金';
COMMENT ON COLUMN t_fuka.total_hakusu IS '合計宿泊数';
COMMENT ON COLUMN t_fuka.total_zeigaku IS '合計税額';
COMMENT ON COLUMN t_fuka.city_zeigaku IS '市区町村税額';
COMMENT ON COLUMN t_fuka.ken_zeigaku IS '都道府県税額';
COMMENT ON COLUMN t_fuka.kasan_kbn IS '加算金額区分';
COMMENT ON COLUMN t_fuka.kasan_ritsu IS '加算割合';
COMMENT ON COLUMN t_fuka.kasan_gaku IS '加算金額';
COMMENT ON COLUMN t_fuka.nokigen IS '納期限';
COMMENT ON COLUMN t_fuka.new_flg IS '最新フラグ';
COMMENT ON COLUMN t_fuka.del_flg IS '削除フラグ';
COMMENT ON COLUMN t_fuka.add_dt IS '作成日時';
COMMENT ON COLUMN t_fuka.add_user IS '作成者';
COMMENT ON COLUMN t_fuka.upd_dt IS '更新日時';
COMMENT ON COLUMN t_fuka.upd_user IS '更新者';
COMMENT ON COLUMN t_fuka.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_fuka_uchi (
  jichitai_cd char(5) NOT NULL,
  shitei_no char(8) NOT NULL,
  rno numeric(3) NOT NULL,
  nendo char(4) NOT NULL,
  kibetsu numeric(2) NOT NULL,
  kazei_kbn numeric(2) NOT NULL,
  zeiritsu_seq numeric(8) NOT NULL,
  fuka_kbn char(1) NOT NULL,
  ryokin_sogaku numeric(13),
  hakusu numeric(8),
  ryokin numeric(13),
  zei_ritsu numeric(12, 2) NOT NULL,
  zeigaku numeric(13) NOT NULL,
  city_zeigaku numeric(13) NOT NULL,
  ken_zeigaku numeric(13) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_fuka_uchi_pkey PRIMARY KEY (jichitai_cd, shitei_no, rno, nendo, kibetsu, kazei_kbn)
);
COMMENT ON TABLE t_fuka_uchi IS '賦課内訳情報';
COMMENT ON COLUMN t_fuka_uchi.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_fuka_uchi.shitei_no IS '指定番号';
COMMENT ON COLUMN t_fuka_uchi.rno IS '履歴番号';
COMMENT ON COLUMN t_fuka_uchi.nendo IS '賦課年度';
COMMENT ON COLUMN t_fuka_uchi.kibetsu IS '期別';
COMMENT ON COLUMN t_fuka_uchi.kazei_kbn IS '課税区分';
COMMENT ON COLUMN t_fuka_uchi.zeiritsu_seq IS '税率管理番号';
COMMENT ON COLUMN t_fuka_uchi.fuka_kbn IS '賦課方式';
COMMENT ON COLUMN t_fuka_uchi.ryokin_sogaku IS '宿泊料金総額';
COMMENT ON COLUMN t_fuka_uchi.hakusu IS '宿泊数';
COMMENT ON COLUMN t_fuka_uchi.ryokin IS '宿泊料金';
COMMENT ON COLUMN t_fuka_uchi.zei_ritsu IS '税率';
COMMENT ON COLUMN t_fuka_uchi.zeigaku IS '税額';
COMMENT ON COLUMN t_fuka_uchi.city_zeigaku IS '市区町村税額';
COMMENT ON COLUMN t_fuka_uchi.ken_zeigaku IS '都道府県税額';
COMMENT ON COLUMN t_fuka_uchi.add_dt IS '作成日時';
COMMENT ON COLUMN t_fuka_uchi.add_user IS '作成者';
COMMENT ON COLUMN t_fuka_uchi.upd_dt IS '更新日時';
COMMENT ON COLUMN t_fuka_uchi.upd_user IS '更新者';
COMMENT ON COLUMN t_fuka_uchi.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_choshu_genbo (
  jichitai_cd char(5) NOT NULL,
  shitei_no char(8) NOT NULL,
  rno numeric(3) NOT NULL,
  nendo char(4) NOT NULL,
  kibetsu numeric(2) NOT NULL,
  sogaku1_sum numeric(14),
  hakusu1_sum numeric(9),
  ryokin1_sum numeric(14),
  zei_ritsu1 numeric(12, 2),
  zeigaku1_sum numeric(14),
  sogaku2_sum numeric(14),
  hakusu2_sum numeric(9),
  ryokin2_sum numeric(14),
  zei_ritsu2 numeric(12, 2),
  zeigaku2_sum numeric(14),
  sogaku3_sum numeric(14),
  hakusu3_sum numeric(9),
  ryokin3_sum numeric(14),
  zei_ritsu3 numeric(12, 2),
  zeigaku3_sum numeric(14),
  sogaku4_sum numeric(14),
  hakusu4_sum numeric(9),
  ryokin4_sum numeric(14),
  zei_ritsu4 numeric(12, 2),
  zeigaku4_sum numeric(14),
  sogaku5_sum numeric(14),
  hakusu5_sum numeric(9),
  ryokin5_sum numeric(14),
  zei_ritsu5 numeric(12, 2),
  zeigaku5_sum numeric(14),
  sogaku6_sum numeric(14),
  hakusu6_sum numeric(9),
  ryokin6_sum numeric(14),
  zei_ritsu6 numeric(12, 2),
  zeigaku6_sum numeric(14),
  sogaku7_sum numeric(14),
  hakusu7_sum numeric(9),
  ryokin7_sum numeric(14),
  zei_ritsu7 numeric(12, 2),
  zeigaku7_sum numeric(14),
  sogaku8_sum numeric(14),
  hakusu8_sum numeric(9),
  ryokin8_sum numeric(14),
  zei_ritsu8 numeric(12, 2),
  zeigaku8_sum numeric(14),
  sogaku9_sum numeric(14),
  hakusu9_sum numeric(9),
  ryokin9_sum numeric(14),
  zei_ritsu9 numeric(12, 2),
  zeigaku9_sum numeric(14),
  sogaku10_sum numeric(14),
  hakusu10_sum numeric(9),
  ryokin10_sum numeric(14),
  zei_ritsu10 numeric(12, 2),
  zeigaku10_sum numeric(14),
  menjo_hakusu_sum numeric(9),
  total_zeigaku_sum numeric(14),
  uchi_idx_1 numeric(8),
  uchi_idx_2 numeric(8),
  uchi_idx_3 numeric(8),
  uchi_idx_4 numeric(8),
  uchi_idx_5 numeric(8),
  uchi_idx_6 numeric(8),
  uchi_idx_7 numeric(8),
  uchi_idx_8 numeric(8),
  uchi_idx_9 numeric(8),
  uchi_idx_10 numeric(8),
  uchi_idx_11 numeric(8),
  uchi_idx_12 numeric(8),
  uchi_idx_13 numeric(8),
  uchi_idx_14 numeric(8),
  uchi_idx_15 numeric(8),
  uchi_idx_16 numeric(8),
  uchi_idx_17 numeric(8),
  uchi_idx_18 numeric(8),
  uchi_idx_19 numeric(8),
  uchi_idx_20 numeric(8),
  uchi_idx_21 numeric(8),
  uchi_idx_22 numeric(8),
  uchi_idx_23 numeric(8),
  uchi_idx_24 numeric(8),
  uchi_idx_25 numeric(8),
  uchi_idx_26 numeric(8),
  uchi_idx_27 numeric(8),
  uchi_idx_28 numeric(8),
  uchi_idx_29 numeric(8),
  uchi_idx_30 numeric(8),
  uchi_idx_31 numeric(8),
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_choshu_genbo_pkey PRIMARY KEY (jichitai_cd, shitei_no, rno, nendo, kibetsu)
);
COMMENT ON TABLE t_choshu_genbo IS '徴収原簿';
COMMENT ON COLUMN t_choshu_genbo.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_choshu_genbo.shitei_no IS '指定番号';
COMMENT ON COLUMN t_choshu_genbo.rno IS '履歴番号';
COMMENT ON COLUMN t_choshu_genbo.nendo IS '賦課年度';
COMMENT ON COLUMN t_choshu_genbo.kibetsu IS '期別';
COMMENT ON COLUMN t_choshu_genbo.sogaku1_sum IS '合計宿泊料金総額1';
COMMENT ON COLUMN t_choshu_genbo.hakusu1_sum IS '合計宿泊数1';
COMMENT ON COLUMN t_choshu_genbo.ryokin1_sum IS '合計宿泊料金1';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu1 IS '税率1';
COMMENT ON COLUMN t_choshu_genbo.zeigaku1_sum IS '合計税額1';
COMMENT ON COLUMN t_choshu_genbo.sogaku2_sum IS '合計宿泊料金総額2';
COMMENT ON COLUMN t_choshu_genbo.hakusu2_sum IS '合計宿泊数2';
COMMENT ON COLUMN t_choshu_genbo.ryokin2_sum IS '合計宿泊料金2';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu2 IS '税率2';
COMMENT ON COLUMN t_choshu_genbo.zeigaku2_sum IS '合計税額2';
COMMENT ON COLUMN t_choshu_genbo.sogaku3_sum IS '合計宿泊料金総額3';
COMMENT ON COLUMN t_choshu_genbo.hakusu3_sum IS '合計宿泊数3';
COMMENT ON COLUMN t_choshu_genbo.ryokin3_sum IS '合計宿泊料金3';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu3 IS '税率3';
COMMENT ON COLUMN t_choshu_genbo.zeigaku3_sum IS '合計税額3';
COMMENT ON COLUMN t_choshu_genbo.sogaku4_sum IS '合計宿泊料金総額4';
COMMENT ON COLUMN t_choshu_genbo.hakusu4_sum IS '合計宿泊数4';
COMMENT ON COLUMN t_choshu_genbo.ryokin4_sum IS '合計宿泊料金4';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu4 IS '税率4';
COMMENT ON COLUMN t_choshu_genbo.zeigaku4_sum IS '合計税額4';
COMMENT ON COLUMN t_choshu_genbo.sogaku5_sum IS '合計宿泊料金総額5';
COMMENT ON COLUMN t_choshu_genbo.hakusu5_sum IS '合計宿泊数5';
COMMENT ON COLUMN t_choshu_genbo.ryokin5_sum IS '合計宿泊料金5';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu5 IS '税率5';
COMMENT ON COLUMN t_choshu_genbo.zeigaku5_sum IS '合計税額5';
COMMENT ON COLUMN t_choshu_genbo.sogaku6_sum IS '合計宿泊料金総額6';
COMMENT ON COLUMN t_choshu_genbo.hakusu6_sum IS '合計宿泊数6';
COMMENT ON COLUMN t_choshu_genbo.ryokin6_sum IS '合計宿泊料金6';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu6 IS '税率6';
COMMENT ON COLUMN t_choshu_genbo.zeigaku6_sum IS '合計税額6';
COMMENT ON COLUMN t_choshu_genbo.sogaku7_sum IS '合計宿泊料金総額7';
COMMENT ON COLUMN t_choshu_genbo.hakusu7_sum IS '合計宿泊数7';
COMMENT ON COLUMN t_choshu_genbo.ryokin7_sum IS '合計宿泊料金7';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu7 IS '税率7';
COMMENT ON COLUMN t_choshu_genbo.zeigaku7_sum IS '合計税額7';
COMMENT ON COLUMN t_choshu_genbo.sogaku8_sum IS '合計宿泊料金総額8';
COMMENT ON COLUMN t_choshu_genbo.hakusu8_sum IS '合計宿泊数8';
COMMENT ON COLUMN t_choshu_genbo.ryokin8_sum IS '合計宿泊料金8';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu8 IS '税率8';
COMMENT ON COLUMN t_choshu_genbo.zeigaku8_sum IS '合計税額8';
COMMENT ON COLUMN t_choshu_genbo.sogaku9_sum IS '合計宿泊料金総額9';
COMMENT ON COLUMN t_choshu_genbo.hakusu9_sum IS '合計宿泊数9';
COMMENT ON COLUMN t_choshu_genbo.ryokin9_sum IS '合計宿泊料金9';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu9 IS '税率9';
COMMENT ON COLUMN t_choshu_genbo.zeigaku9_sum IS '合計税額9';
COMMENT ON COLUMN t_choshu_genbo.sogaku10_sum IS '合計宿泊料金総額10';
COMMENT ON COLUMN t_choshu_genbo.hakusu10_sum IS '合計宿泊数10';
COMMENT ON COLUMN t_choshu_genbo.ryokin10_sum IS '合計宿泊料金10';
COMMENT ON COLUMN t_choshu_genbo.zei_ritsu10 IS '税率10';
COMMENT ON COLUMN t_choshu_genbo.zeigaku10_sum IS '合計税額10';
COMMENT ON COLUMN t_choshu_genbo.menjo_hakusu_sum IS '合計免除泊数';
COMMENT ON COLUMN t_choshu_genbo.total_zeigaku_sum IS '総合計税額';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_1 IS '徴収原簿内訳識別子1';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_2 IS '徴収原簿内訳識別子2';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_3 IS '徴収原簿内訳識別子3';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_4 IS '徴収原簿内訳識別子4';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_5 IS '徴収原簿内訳識別子5';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_6 IS '徴収原簿内訳識別子6';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_7 IS '徴収原簿内訳識別子7';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_8 IS '徴収原簿内訳識別子8';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_9 IS '徴収原簿内訳識別子9';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_10 IS '徴収原簿内訳識別子10';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_11 IS '徴収原簿内訳識別子11';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_12 IS '徴収原簿内訳識別子12';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_13 IS '徴収原簿内訳識別子13';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_14 IS '徴収原簿内訳識別子14';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_15 IS '徴収原簿内訳識別子15';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_16 IS '徴収原簿内訳識別子16';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_17 IS '徴収原簿内訳識別子17';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_18 IS '徴収原簿内訳識別子18';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_19 IS '徴収原簿内訳識別子19';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_20 IS '徴収原簿内訳識別子20';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_21 IS '徴収原簿内訳識別子21';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_22 IS '徴収原簿内訳識別子22';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_23 IS '徴収原簿内訳識別子23';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_24 IS '徴収原簿内訳識別子24';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_25 IS '徴収原簿内訳識別子25';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_26 IS '徴収原簿内訳識別子26';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_27 IS '徴収原簿内訳識別子27';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_28 IS '徴収原簿内訳識別子28';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_29 IS '徴収原簿内訳識別子29';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_30 IS '徴収原簿内訳識別子30';
COMMENT ON COLUMN t_choshu_genbo.uchi_idx_31 IS '徴収原簿内訳識別子31';
COMMENT ON COLUMN t_choshu_genbo.add_dt IS '作成日時';
COMMENT ON COLUMN t_choshu_genbo.add_user IS '作成者';
COMMENT ON COLUMN t_choshu_genbo.upd_dt IS '更新日時';
COMMENT ON COLUMN t_choshu_genbo.upd_user IS '更新者';
COMMENT ON COLUMN t_choshu_genbo.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_choshu_genbo_uchi (
  jichitai_cd char(5) NOT NULL,
  uchi_idx numeric(8) NOT NULL,
  sogaku1 numeric(13),
  hakusu1 numeric(8),
  ryokin1 numeric(13),
  sogaku2 numeric(13),
  hakusu2 numeric(8),
  ryokin2 numeric(13),
  sogaku3 numeric(13),
  hakusu3 numeric(8),
  ryokin3 numeric(13),
  sogaku4 numeric(13),
  hakusu4 numeric(8),
  ryokin4 numeric(13),
  sogaku5 numeric(13),
  hakusu5 numeric(8),
  ryokin5 numeric(13),
  sogaku6 numeric(13),
  hakusu6 numeric(8),
  ryokin6 numeric(13),
  sogaku7 numeric(13),
  hakusu7 numeric(8),
  ryokin7 numeric(13),
  sogaku8 numeric(13),
  hakusu8 numeric(8),
  ryokin8 numeric(13),
  sogaku9 numeric(13),
  hakusu9 numeric(8),
  ryokin9 numeric(13),
  sogaku10 numeric(13),
  hakusu10 numeric(8),
  ryokin10 numeric(13),
  menjo_hakusu numeric(8),
  zeigaku numeric(13),
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_choshu_genbo_uchi_pkey PRIMARY KEY (jichitai_cd, uchi_idx)
);
COMMENT ON TABLE t_choshu_genbo_uchi IS '徴収原簿内訳';
COMMENT ON COLUMN t_choshu_genbo_uchi.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_choshu_genbo_uchi.uchi_idx IS '徴収原簿内訳識別子';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku1 IS '宿泊料金総額1';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu1 IS '宿泊数1';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin1 IS '宿泊料金1';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku2 IS '宿泊料金総額2';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu2 IS '宿泊数2';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin2 IS '宿泊料金2';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku3 IS '宿泊料金総額3';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu3 IS '宿泊数3';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin3 IS '宿泊料金3';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku4 IS '宿泊料金総額4';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu4 IS '宿泊数4';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin4 IS '宿泊料金4';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku5 IS '宿泊料金総額5';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu5 IS '宿泊数5';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin5 IS '宿泊料金5';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku6 IS '宿泊料金総額6';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu6 IS '宿泊数6';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin6 IS '宿泊料金6';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku7 IS '宿泊料金総額7';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu7 IS '宿泊数7';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin7 IS '宿泊料金7';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku8 IS '宿泊料金総額8';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu8 IS '宿泊数8';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin8 IS '宿泊料金8';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku9 IS '宿泊料金総額9';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu9 IS '宿泊数9';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin9 IS '宿泊料金9';
COMMENT ON COLUMN t_choshu_genbo_uchi.sogaku10 IS '宿泊料金総額10';
COMMENT ON COLUMN t_choshu_genbo_uchi.hakusu10 IS '宿泊数10';
COMMENT ON COLUMN t_choshu_genbo_uchi.ryokin10 IS '宿泊料金10';
COMMENT ON COLUMN t_choshu_genbo_uchi.menjo_hakusu IS '免除泊数';
COMMENT ON COLUMN t_choshu_genbo_uchi.zeigaku IS '税額';
COMMENT ON COLUMN t_choshu_genbo_uchi.add_dt IS '作成日時';
COMMENT ON COLUMN t_choshu_genbo_uchi.add_user IS '作成者';
COMMENT ON COLUMN t_choshu_genbo_uchi.upd_dt IS '更新日時';
COMMENT ON COLUMN t_choshu_genbo_uchi.upd_user IS '更新者';
COMMENT ON COLUMN t_choshu_genbo_uchi.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_shoreikin (
  jichitai_cd char(5) NOT NULL,
  shitei_no char(8) NOT NULL,
  nendo char(4) NOT NULL,
  kofu_zeigaku numeric(14) NOT NULL,
  kofu_ritsu numeric(5, 2) NOT NULL,
  kofu_gaku numeric(13) NOT NULL,
  kofu_ymd date,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_shoreikin_pkey PRIMARY KEY (jichitai_cd, shitei_no, nendo)
);
COMMENT ON TABLE t_shoreikin IS '奨励金交付情報';
COMMENT ON COLUMN t_shoreikin.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_shoreikin.shitei_no IS '指定番号';
COMMENT ON COLUMN t_shoreikin.nendo IS '奨励金年度';
COMMENT ON COLUMN t_shoreikin.kofu_zeigaku IS '納入税額';
COMMENT ON COLUMN t_shoreikin.kofu_ritsu IS '交付率';
COMMENT ON COLUMN t_shoreikin.kofu_gaku IS '交付額';
COMMENT ON COLUMN t_shoreikin.kofu_ymd IS '交付年月日';
COMMENT ON COLUMN t_shoreikin.add_dt IS '作成日時';
COMMENT ON COLUMN t_shoreikin.add_user IS '作成者';
COMMENT ON COLUMN t_shoreikin.upd_dt IS '更新日時';
COMMENT ON COLUMN t_shoreikin.upd_user IS '更新者';
COMMENT ON COLUMN t_shoreikin.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_furikomi_koza (
  jichitai_cd char(5) NOT NULL,
  shitei_no char(8) NOT NULL,
  bank_cd char(4) NOT NULL,
  bank_name varchar(30) NOT NULL,
  branch_cd char(3) NOT NULL,
  branch_name varchar(30) NOT NULL,
  shumoku char(1) NOT NULL,
  koza_no char(8) NOT NULL,
  meigi varchar(30) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_furikomi_koza_pkey PRIMARY KEY (jichitai_cd, shitei_no)
);
COMMENT ON TABLE t_furikomi_koza IS '振込口座情報';
COMMENT ON COLUMN t_furikomi_koza.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_furikomi_koza.shitei_no IS '指定番号';
COMMENT ON COLUMN t_furikomi_koza.bank_cd IS '金融機関コード';
COMMENT ON COLUMN t_furikomi_koza.bank_name IS '金融機関名';
COMMENT ON COLUMN t_furikomi_koza.branch_cd IS '支店コード';
COMMENT ON COLUMN t_furikomi_koza.branch_name IS '支店名';
COMMENT ON COLUMN t_furikomi_koza.shumoku IS '預金種目';
COMMENT ON COLUMN t_furikomi_koza.koza_no IS '口座番号';
COMMENT ON COLUMN t_furikomi_koza.meigi IS '口座名義';
COMMENT ON COLUMN t_furikomi_koza.add_dt IS '作成日時';
COMMENT ON COLUMN t_furikomi_koza.add_user IS '作成者';
COMMENT ON COLUMN t_furikomi_koza.upd_dt IS '更新日時';
COMMENT ON COLUMN t_furikomi_koza.upd_user IS '更新者';
COMMENT ON COLUMN t_furikomi_koza.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_eltax_renkei (
  jichitai_cd char(5) NOT NULL,
  seq numeric(8) NOT NULL,
  file_name varchar(256) NOT NULL,
  shubetsu char(2),
  shori_dt timestamp,
  shori_kekka char(1),
  log bytea,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT t_eltax_renkei_pkey PRIMARY KEY (jichitai_cd, seq)
);
COMMENT ON TABLE t_eltax_renkei IS 'eLTAX連携管理';
COMMENT ON COLUMN t_eltax_renkei.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN t_eltax_renkei.seq IS '管理番号';
COMMENT ON COLUMN t_eltax_renkei.file_name IS 'ファイル名';
COMMENT ON COLUMN t_eltax_renkei.shubetsu IS 'ファイル種別';
COMMENT ON COLUMN t_eltax_renkei.shori_dt IS '処理日時';
COMMENT ON COLUMN t_eltax_renkei.shori_kekka IS '処理結果';
COMMENT ON COLUMN t_eltax_renkei.log IS 'ログ';
COMMENT ON COLUMN t_eltax_renkei.add_dt IS '作成日時';
COMMENT ON COLUMN t_eltax_renkei.add_user IS '作成者';
COMMENT ON COLUMN t_eltax_renkei.upd_dt IS '更新日時';
COMMENT ON COLUMN t_eltax_renkei.upd_user IS '更新者';
COMMENT ON COLUMN t_eltax_renkei.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS m_nozei_shuki (
  jichitai_cd char(5) NOT NULL,
  seq numeric(3) NOT NULL,
  shuki numeric(2) NOT NULL,
  del_flg char(1) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT m_nozei_shuki_pkey PRIMARY KEY (jichitai_cd, seq)
);
COMMENT ON TABLE m_nozei_shuki IS '納税周期マスタ';
COMMENT ON COLUMN m_nozei_shuki.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN m_nozei_shuki.seq IS '管理番号';
COMMENT ON COLUMN m_nozei_shuki.shuki IS '納税周期';
COMMENT ON COLUMN m_nozei_shuki.del_flg IS '削除フラグ';
COMMENT ON COLUMN m_nozei_shuki.add_dt IS '作成日時';
COMMENT ON COLUMN m_nozei_shuki.add_user IS '作成者';
COMMENT ON COLUMN m_nozei_shuki.upd_dt IS '更新日時';
COMMENT ON COLUMN m_nozei_shuki.upd_user IS '更新者';
COMMENT ON COLUMN m_nozei_shuki.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS m_zeiritsu (
  jichitai_cd char(5) NOT NULL,
  seq numeric(5) NOT NULL,
  taisho_kbn char(1) NOT NULL,
  tekiyo_st_ym char(6) NOT NULL,
  tekiyo_ed_ym char(6) NOT NULL,
  fuka_kbn char(1) NOT NULL,
  del_flg char(1) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT m_zeiritsu_pkey PRIMARY KEY (jichitai_cd, seq)
);
COMMENT ON TABLE m_zeiritsu IS '税率管理マスタ';
COMMENT ON COLUMN m_zeiritsu.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN m_zeiritsu.seq IS '税率管理番号';
COMMENT ON COLUMN m_zeiritsu.taisho_kbn IS '対象区分';
COMMENT ON COLUMN m_zeiritsu.tekiyo_st_ym IS '適用開始年月';
COMMENT ON COLUMN m_zeiritsu.tekiyo_ed_ym IS '適用終了年月';
COMMENT ON COLUMN m_zeiritsu.fuka_kbn IS '賦課方式';
COMMENT ON COLUMN m_zeiritsu.del_flg IS '削除フラグ';
COMMENT ON COLUMN m_zeiritsu.add_dt IS '作成日時';
COMMENT ON COLUMN m_zeiritsu.add_user IS '作成者';
COMMENT ON COLUMN m_zeiritsu.upd_dt IS '更新日時';
COMMENT ON COLUMN m_zeiritsu.upd_user IS '更新者';
COMMENT ON COLUMN m_zeiritsu.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS m_zeiritsu_teigaku (
  jichitai_cd char(5) NOT NULL,
  seq numeric(5) NOT NULL,
  teigaku_seq numeric(8) NOT NULL,
  ryokin_st numeric(13) NOT NULL,
  ryokin_ed numeric(13),
  zeigaku numeric(13) NOT NULL,
  del_flg char(1) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT m_zeiritsu_teigaku_pkey PRIMARY KEY (jichitai_cd, seq, teigaku_seq)
);
COMMENT ON TABLE m_zeiritsu_teigaku IS '税率定額詳細マスタ';
COMMENT ON COLUMN m_zeiritsu_teigaku.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN m_zeiritsu_teigaku.seq IS '税率管理番号';
COMMENT ON COLUMN m_zeiritsu_teigaku.teigaku_seq IS '定額管理番号';
COMMENT ON COLUMN m_zeiritsu_teigaku.ryokin_st IS '宿泊料金範囲開始';
COMMENT ON COLUMN m_zeiritsu_teigaku.ryokin_ed IS '宿泊料金範囲終了';
COMMENT ON COLUMN m_zeiritsu_teigaku.zeigaku IS '宿泊税額';
COMMENT ON COLUMN m_zeiritsu_teigaku.del_flg IS '削除フラグ';
COMMENT ON COLUMN m_zeiritsu_teigaku.add_dt IS '作成日時';
COMMENT ON COLUMN m_zeiritsu_teigaku.add_user IS '作成者';
COMMENT ON COLUMN m_zeiritsu_teigaku.upd_dt IS '更新日時';
COMMENT ON COLUMN m_zeiritsu_teigaku.upd_user IS '更新者';
COMMENT ON COLUMN m_zeiritsu_teigaku.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS m_zeiritsu_teiritsu (
  jichitai_cd char(5) NOT NULL,
  seq numeric(5) NOT NULL,
  teiritsu_seq numeric(8) NOT NULL,
  zei_ritsu numeric(3, 2) NOT NULL,
  del_flg char(1) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT m_zeiritsu_teiritsu_pkey PRIMARY KEY (jichitai_cd, seq, teiritsu_seq)
);
COMMENT ON TABLE m_zeiritsu_teiritsu IS '税率定率詳細マスタ';
COMMENT ON COLUMN m_zeiritsu_teiritsu.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN m_zeiritsu_teiritsu.seq IS '税率管理番号';
COMMENT ON COLUMN m_zeiritsu_teiritsu.teiritsu_seq IS '定率管理番号';
COMMENT ON COLUMN m_zeiritsu_teiritsu.zei_ritsu IS '宿泊税率';
COMMENT ON COLUMN m_zeiritsu_teiritsu.del_flg IS '削除フラグ';
COMMENT ON COLUMN m_zeiritsu_teiritsu.add_dt IS '作成日時';
COMMENT ON COLUMN m_zeiritsu_teiritsu.add_user IS '作成者';
COMMENT ON COLUMN m_zeiritsu_teiritsu.upd_dt IS '更新日時';
COMMENT ON COLUMN m_zeiritsu_teiritsu.upd_user IS '更新者';
COMMENT ON COLUMN m_zeiritsu_teiritsu.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS m_user (
  jichitai_cd char(5) NOT NULL,
  id varchar(100) NOT NULL,
  password char(64) NOT NULL,
  name varchar(200) NOT NULL,
  name_kana varchar(200) NOT NULL,
  busho varchar(200) NOT NULL,
  role_id numeric(5) NOT NULL,
  add_dt timestamp NOT NULL,
  add_user varchar(20) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT m_user_pkey PRIMARY KEY (jichitai_cd, id)
);
COMMENT ON TABLE m_user IS 'ユーザ管理マスタ';
COMMENT ON COLUMN m_user.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN m_user.id IS 'ユーザＩＤ';
COMMENT ON COLUMN m_user.password IS 'パスワード';
COMMENT ON COLUMN m_user.name IS '氏名';
COMMENT ON COLUMN m_user.name_kana IS '氏名かな';
COMMENT ON COLUMN m_user.busho IS '部署';
COMMENT ON COLUMN m_user.role_id IS '権限ロールＩＤ';
COMMENT ON COLUMN m_user.add_dt IS '作成日時';
COMMENT ON COLUMN m_user.add_user IS '作成者';
COMMENT ON COLUMN m_user.upd_dt IS '更新日時';
COMMENT ON COLUMN m_user.upd_user IS '更新者';
COMMENT ON COLUMN m_user.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS m_role (
  jichitai_cd char(5) NOT NULL,
  role_id numeric(5) NOT NULL,
  name varchar(200) NOT NULL,
  add_user varchar(200) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT m_role_pkey PRIMARY KEY (jichitai_cd, role_id)
);
COMMENT ON TABLE m_role IS '権限管理マスタ';
COMMENT ON COLUMN m_role.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN m_role.role_id IS '権限ロールＩＤ';
COMMENT ON COLUMN m_role.name IS '権限名称';
COMMENT ON COLUMN m_role.add_user IS '作成者';
COMMENT ON COLUMN m_role.upd_dt IS '更新日時';
COMMENT ON COLUMN m_role.upd_user IS '更新者';
COMMENT ON COLUMN m_role.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS m_role_dtl (
  jichitai_cd char(5) NOT NULL,
  role_id numeric(5) NOT NULL,
  screen_id char(10) NOT NULL,
  permission char(1) NOT NULL,
  add_user varchar(200) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT m_role_dtl_pkey PRIMARY KEY (jichitai_cd, role_id, screen_id)
);
COMMENT ON TABLE m_role_dtl IS '権限詳細マスタ';
COMMENT ON COLUMN m_role_dtl.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN m_role_dtl.role_id IS '権限ロールＩＤ';
COMMENT ON COLUMN m_role_dtl.screen_id IS '画面ＩＤ';
COMMENT ON COLUMN m_role_dtl.permission IS '権限';
COMMENT ON COLUMN m_role_dtl.add_user IS '作成者';
COMMENT ON COLUMN m_role_dtl.upd_dt IS '更新日時';
COMMENT ON COLUMN m_role_dtl.upd_user IS '更新者';
COMMENT ON COLUMN m_role_dtl.version IS 'バージョン';

------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS m_screen (
  jichitai_cd char(5) NOT NULL,
  screen_id char(10) NOT NULL,
  screen_name varchar(100) NOT NULL,
  add_user varchar(200) NOT NULL,
  upd_dt timestamp NOT NULL,
  upd_user varchar(20) NOT NULL,
  version numeric(5) NOT NULL,
  CONSTRAINT m_screen_pkey PRIMARY KEY (jichitai_cd, screen_id)
);
COMMENT ON TABLE m_screen IS '画面管理マスタ';
COMMENT ON COLUMN m_screen.jichitai_cd IS '自治体コード';
COMMENT ON COLUMN m_screen.screen_id IS '画面ＩＤ';
COMMENT ON COLUMN m_screen.screen_name IS '画面名称';
COMMENT ON COLUMN m_screen.add_user IS '作成者';
COMMENT ON COLUMN m_screen.upd_dt IS '更新日時';
COMMENT ON COLUMN m_screen.upd_user IS '更新者';
COMMENT ON COLUMN m_screen.version IS 'バージョン';

