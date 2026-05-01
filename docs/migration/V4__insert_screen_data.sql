-- 画面管理マスタ.csvに基づくm_screenテーブルへのダミーデータ挿入

-- 既存データをクリア（必要に応じて）
DELETE FROM m_screen;

-- 画面管理マスタ.csvのデータを挿入（実際のテーブル構造に合わせて）
INSERT INTO m_screen (jichitai_cd, screen_id, screen_name, add_user, upd_dt, upd_user, version) VALUES
('01202', '1', '特別徴収義務者管理台帳', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '2', '特別徴収義務者登録画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '3', '特別徴収義務者編集画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '4', '特別徴収義務者照会画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '5', 'ユーザー検索画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '6', 'ユーザー登録画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '7', 'ユーザー編集画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '8', 'ユーザー照会画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '9', '権限管理画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '10', '納税周期照会画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1),
('01202', '11', '納税周期設定画面', 'admin', CURRENT_TIMESTAMP, 'admin', 1);

-- サンプル権限データ（m_roleテーブルが存在する場合）
INSERT INTO m_role (jichitai_cd, name, add_user, upd_user) VALUES
('01202', '管理者', 'system', 'system'),
('01202', '一般ユーザー', 'system', 'system'),
('01202', '閲覧専用', 'system', 'system')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- サンプル権限詳細データ（m_role_dtlテーブルが存在する場合）
-- 管理者：全権限
INSERT INTO m_role_dtl (jichitai_cd, role_id, screen_id, permission, add_user, upd_user) VALUES
('01202', 1, '1', 2, 'system', 'system'),
('01202', 1, '2', 2, 'system', 'system'),
('01202', 1, '3', 2, 'system', 'system'),
('01202', 1, '4', 2, 'system', 'system'),
('01202', 1, '5', 2, 'system', 'system'),
('01202', 1, '6', 2, 'system', 'system'),
('01202', 1, '7', 2, 'system', 'system'),
('01202', 1, '8', 2, 'system', 'system'),
('01202', 1, '9', 2, 'system', 'system'),
('01202', 1, '10', 2, 'system', 'system'),
('01202', 1, '11', 2, 'system', 'system');

-- 一般ユーザー：基本機能のみ
INSERT INTO m_role_dtl (jichitai_cd, role_id, screen_id, permission, add_user, upd_user) VALUES
('01202', 2, '1', 2, 'system', 'system'),
('01202', 2, '2', 2, 'system', 'system'),
('01202', 2, '3', 2, 'system', 'system'),
('01202', 2, '4', 1, 'system', 'system');

-- 閲覧専用：参照のみ
INSERT INTO m_role_dtl (jichitai_cd, role_id, screen_id, permission, add_user, upd_user) VALUES
('01202', 3, '1', 1, 'system', 'system'),
('01202', 3, '4', 1, 'system', 'system');