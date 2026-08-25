package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 更正・決定通知書 帳票DTO
 * フィールド名はJRXMLの&lt;field name&gt;と完全一致させること
 */
@Data
public class KoseiKetteiTsuchiReportsDto {

    // ── 宛名・施設情報 ──────────────────────────────
    private String shitei_no;
    private String yubin_no;
    private String jusho;
    private String name;
    private String shisetsu_yubin_no;
    private String shisetsu_jusho;
    private String shisetsu_name;
    private String henko_riyu;

    // ── 通知日 ─────────────────────────────────────
    private String tsuchi_Ymd;

    // ── 期別1（b1）───────────────────────────────────
    private String b1_nen;
    private String b1_tsuki;

    // 宿泊者数（定額用）
    private String b1_hakusu1;
    private String b1_hakusu2;
    private String b1_hakusu3;
    private String b1_hakusu4;
    private String b1_hakusu5;
    private String b1_hakusu_sum;

    private String b1_sogaku1;
    private String b1_sogaku2;
    private String b1_sogaku3;
    private String b1_sogaku4;
    private String b1_sogaku5;
    private String b1_sogaku_sum;

    private String b1_ryokin1;
    private String b1_ryokin2;
    private String b1_ryokin3;
    private String b1_ryokin4;
    private String b1_ryokin5;
    private String b1_ryokin_sum;

    private String b1_zeigaku1;
    private String b1_zeigaku2;
    private String b1_zeigaku3;
    private String b1_zeigaku4;
    private String b1_zeigaku5;
    private String b1_zeigaku_sum;

    // 既納の税額（定率・旧レイアウト用）
    private String b1_kino_zeigaku1;
    private String b1_kino_zeigaku2;
    private String b1_kino_zeigaku3;
    private String b1_kino_zeigaku4;
    private String b1_kino_zeigaku5;
    private String b1_kino_zeigaku_sum;

    private String b1_sashihiki1;
    private String b1_sashihiki2;
    private String b1_sashihiki3;
    private String b1_sashihiki4;
    private String b1_sashihiki5;
    private String b1_sashihiki_sum;
    
    // b1 税率
    private String b1_zei_ritsu1;
    private String b1_zei_ritsu2;
    private String b1_zei_ritsu3;
    private String b1_zei_ritsu4;
    private String b1_zei_ritsu5;

    // ── 期別2（b2）───────────────────────────────────
    private String b2_nen;
    private String b2_tsuki;

    // 宿泊者数（b2）
    private String b2_hakusu1;
    private String b2_hakusu2;
    private String b2_hakusu3;
    private String b2_hakusu4;
    private String b2_hakusu5;
    private String b2_hakusu_sum;

    private String b2_sogaku1;
    private String b2_sogaku2;
    private String b2_sogaku3;
    private String b2_sogaku4;
    private String b2_sogaku5;
    private String b2_sogaku_sum;
    private String b2_ryokin1;
    private String b2_ryokin2;
    private String b2_ryokin3;
    private String b2_ryokin4;
    private String b2_ryokin5;
    private String b2_ryokin_sum;
    private String b2_zeigaku1;
    private String b2_zeigaku2;
    private String b2_zeigaku3;
    private String b2_zeigaku4;
    private String b2_zeigaku5;
    private String b2_zeigaku_sum;

    // 既納の税額（b2）
    private String b2_kino_zeigaku1;
    private String b2_kino_zeigaku2;
    private String b2_kino_zeigaku3;
    private String b2_kino_zeigaku4;
    private String b2_kino_zeigaku5;
    private String b2_kino_zeigaku_sum;

    private String b2_sashihiki1;
    private String b2_sashihiki2;
    private String b2_sashihiki3;
    private String b2_sashihiki4;
    private String b2_sashihiki5;
    private String b2_sashihiki_sum;
    
    // b2用税率
    private String b2_zei_ritsu1;
    private String b2_zei_ritsu2;
    private String b2_zei_ritsu3;
    private String b2_zei_ritsu4;
    private String b2_zei_ritsu5;

    // ── 期別3（b3）───────────────────────────────────
    private String b3_nen;
    private String b3_tsuki;

    // 宿泊者数（b3）
    private String b3_hakusu1;
    private String b3_hakusu2;
    private String b3_hakusu3;
    private String b3_hakusu4;
    private String b3_hakusu5;
    private String b3_hakusu_sum;

    private String b3_sogaku1;
    private String b3_sogaku2;
    private String b3_sogaku3;
    private String b3_sogaku4;
    private String b3_sogaku5;
    private String b3_sogaku_sum;
    private String b3_ryokin1;
    private String b3_ryokin2;
    private String b3_ryokin3;
    private String b3_ryokin4;
    private String b3_ryokin5;
    private String b3_ryokin_sum;
    private String b3_zeigaku1;
    private String b3_zeigaku2;
    private String b3_zeigaku3;
    private String b3_zeigaku4;
    private String b3_zeigaku5;
    private String b3_zeigaku_sum;

    // 既納の税額（b3）
    private String b3_kino_zeigaku1;
    private String b3_kino_zeigaku2;
    private String b3_kino_zeigaku3;
    private String b3_kino_zeigaku4;
    private String b3_kino_zeigaku5;
    private String b3_kino_zeigaku_sum;

    private String b3_sashihiki1;
    private String b3_sashihiki2;
    private String b3_sashihiki3;
    private String b3_sashihiki4;
    private String b3_sashihiki5;
    private String b3_sashihiki_sum;
    
    // b3用税率
    private String b3_zei_ritsu1;
    private String b3_zei_ritsu2;
    private String b3_zei_ritsu3;
    private String b3_zei_ritsu4;
    private String b3_zei_ritsu5;

    // ── 納入・加算金 ─────────────────────────────────
    private String nofu_zeigaku;
    private String kasan_ritsu1;
    private String kasan_gaku1;
    private String kasan_ritsu2;
    private String kasan_gaku2;
    private String kasan_ritsu3;
    private String kasan_gaku3;
    private String nofu_kigen_nen;
    private String nofu_kigen_tsuki;
    private String nofu_kigen_hi;

    // ── fukaKbn（JRXML切り替え用。帳票フィールドではない）──
    private String fukaKbn;
    
    private String kasan_kbn1;
    private String kasan_kbn2;
    private String kasan_kbn3;
    
    
    private String todoufuken;
    
    private String henko_kbn;
    
    private String kbn_name1;
    private String kbn_name2;
    private String kbn_name3;
    private String kbn_name4;
    private String kbn_name5;
    
    // ── 帳票フィールド ────────────────
    private byte[] koin;
}
