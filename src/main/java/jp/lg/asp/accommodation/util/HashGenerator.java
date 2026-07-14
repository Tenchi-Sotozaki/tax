package jp.lg.asp.accommodation.util;

public class HashGenerator {
    public static void main(String[] args) {
        System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
            .encode("GEpassword"));  // ← 決めた初期パスワードに置き換え
    }
}

//ハッシュ値の生成方法
//EclipseのパッケージエクスプローラーでHashGenerator.javaを右クリック→「実行」→「Javaアプリケーション」を選択します。INSERT INTO m_user (


//INSERT INTO m_user (jichitai_cd, id, name, name_kana, busho, role_id,
//password, initial_password_flg,
//add_dt, add_user, upd_dt, upd_user, version
//)
//VALUES (
//'01202',                 -- 自治体コード（環境に合わせる）
//'admin',                 -- デフォルトユーザーID
//'システム管理者',
//'しすてむかんりしゃ',
//'システム管理課',
//1,                       -- デフォルト権限ロール（role_id=1）
//'$2a$10$FcnRPCuzPbfatsVK/4ZaYePXPIlxryDAevYUBBp/A34ZmNKlIX3eO',
//'1',                     -- 初期パスワードのまま＝初回ログイン時に変更を強制
//now(), 'SYSTEM', now(), 'SYSTEM', 0
//)
//ON CONFLICT (jichitai_cd, id) DO NOTHING;