-- サンプルデータ挿入（テスト用）

-- 宛名マスタのサンプルデータ
INSERT INTO m_atena (jichitai_cd, atena_no, kbn, kojin_no, hojin_no, name, name_kana, yubin_no, jusho, tel1, tel2, add_user, upd_dt, upd_user, version) VALUES
('01234', 1001, '2', null, '1234567890123', 'グランドホテル東京', 'ぐらんどほてるとうきょう', '160-0023', '東京都新宿区西新宿1-1-1', '03-1234-5678', null, 'system', CURRENT_TIMESTAMP, 'system', 1),
('01234', 1002, '1', '123456789012', null, '温泉旅館やまと', 'おんせんりょかんやまと', '100-0001', '東京都千代田区千代田1-1', '03-2345-6789', null, 'system', CURRENT_TIMESTAMP, 'system', 1),
('01234', 1003, '2', null, '3456789012345', 'シティイン新宿', 'してぃいんしんじゅく', '160-0022', '東京都新宿区新宿3-1-1', '03-3456-7890', null, 'system', CURRENT_TIMESTAMP, 'system', 1);

-- 特別徴収義務者テーブルのサンプルデータ
INSERT INTO t_tokugimu (jichitai_cd, shitei_no, rno, toroku_ymd, shinkoku_ymd, henko_ymd, atena_no, shisetsu_name, shisetsu_name_kana, shisetsu_yubin_no, shisetsu_jusho, shisetsu_tel, yuka_menseki, chijo_kai, chika_kai, kyakushitsu_su, shuyo_su, kyoka_name, kyoka_name_kana, kyoka_yubin_no, kyoka_jusho, kyoka_tel, kyoka_shu, kyoka_no, soufusaki_name, soufusaki_name_kana, soufusaki_yubin_no, soufusaki_jusho, soufusaki_tel, biko, eigyo_st_ymd, eigyo_ed_ymd, kyushi_st_ymd, kyushi_ed_ymd, kyuhaishi_riyu, eltax_umu, nokigen, new_flg, del_flg, add_dt, add_user, upd_dt, upd_user, version) VALUES
('01234', 'T0010001', 1, '2024-01-01', '2024-01-01', '2024-01-01', 1001, 'グランドホテル東京本館', 'ぐらんどほてるとうきょうほんかん', '160-0023', '東京都新宿区西新宿1-1-1', '03-1234-5678', 5000.00, 10, 2, 100, 200, 'グランドホテル東京', 'ぐらんどほてるとうきょう', '160-0023', '東京都新宿区西新宿1-1-1', '03-1234-5678', '1', 'HOTEL-001', 'グランドホテル東京', 'ぐらんどほてるとうきょう', '160-0023', '東京都新宿区西新宿1-1-1', '03-1234-5678', null, '2024-01-01', null, null, null, null, '1', '01', '1', '0', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 1),
('01234', 'R0020001', 1, '2024-01-01', '2024-01-01', '2024-01-01', 1002, 'やまと本館', 'やまとほんかん', '100-0001', '東京都千代田区千代田1-1', '03-2345-6789', 3000.00, 5, 1, 50, 100, '温泉旅館やまと', 'おんせんりょかんやまと', '100-0001', '東京都千代田区千代田1-1', '03-2345-6789', '2', 'RYOKAN-001', '温泉旅館やまと', 'おんせんりょかんやまと', '100-0001', '東京都千代田区千代田1-1', '03-2345-6789', null, '2024-01-01', null, null, null, null, '0', '02', '1', '0', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 1),
('01234', 'S0030001', 1, '2024-01-01', '2024-01-01', '2024-01-01', 1003, 'シティイン新宿', 'してぃいんしんじゅく', '160-0022', '東京都新宿区新宿3-1-1', '03-3456-7890', 2000.00, 8, 1, 80, 150, 'シティイン新宿', 'してぃいんしんじゅく', '160-0022', '東京都新宿区新宿3-1-1', '03-3456-7890', '3', 'SIMPLE-001', 'シティイン新宿', 'してぃいんしんじゅく', '160-0022', '東京都新宿区新宿3-1-1', '03-3456-7890', null, '2024-01-01', null, '2024-06-01', '2024-08-31', '改装工事のため', '1', '03', '1', '0', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 1);

-- 合算申告内訳のサンプルデータ
INSERT INTO t_gassan_uchi (jichitai_cd, gassan_shitei_no, rno, shitei_no, add_dt, add_user, upd_dt, upd_user, version) VALUES
('01234', 'G0010001', 1, 'T0010001', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 1),
('01234', 'G0010001', 1, 'S0030001', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 1);

-- 所有者情報のサンプルデータ
INSERT INTO t_shoyusha (jichitai_cd, shitei_no, rno, idx, shoyusha_name, shoyusha_name_kana, shoyusha_yubin_no, shoyusha_jusho, shoyusha_tel, add_dt, add_user, upd_dt, upd_user, version) VALUES
('01234', 'T0010001', 1, 1, 'グランドホテル東京株式会社', 'ぐらんどほてるとうきょうかぶしきがいしゃ', '160-0023', '東京都新宿区西新宿1-1-1', '03-1234-5678', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 1),
('01234', 'R0020001', 1, 1, '山田太郎', 'やまだたろう', '100-0001', '東京都千代田区千代田1-1', '03-2345-6789', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 1);

-- 納税管理人のサンプルデータ
INSERT INTO t_nokan (jichitai_cd, shitei_no, rno, menjo_kbn, toroku_ymd, shinkoku_ymd, atena_no, name, name_kana, yubin_no, jusho, tel, menjo_riyu, new_flg, del_flg, add_dt, add_user, upd_dt, upd_user, version) VALUES
('01234', 'T0010001', 1, '0', '2024-01-01', '2024-01-01', '1001001', '税理士田中', 'ぜいりしたなか', '160-0023', '東京都新宿区西新宿2-1-1', '03-1111-2222', null, '1', '0', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 1);