-- m_kofu_ritsu テーブル変更
-- 新カラム追加
ALTER TABLE m_kofu_ritsu ADD COLUMN IF NOT EXISTS sanshutsu text;
ALTER TABLE m_kofu_ritsu ADD COLUMN IF NOT EXISTS kbn text;
ALTER TABLE m_kofu_ritsu ADD COLUMN IF NOT EXISTS saiteigaku numeric(15, 2);
ALTER TABLE m_kofu_ritsu ADD COLUMN IF NOT EXISTS tekiyo_st_nendo integer;

-- 旧カラム削除
ALTER TABLE m_kofu_ritsu DROP COLUMN IF EXISTS tekiyo_st_ymd;
ALTER TABLE m_kofu_ritsu DROP COLUMN IF EXISTS tekiyo_ed_ymd;
