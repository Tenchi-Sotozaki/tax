package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_tokugimu")
@IdClass(TokugimuId.class)
@Getter @Setter
public class Tokugimu {

    @Id
    @Column(name = "jichitai_cd", length = 5)
    private String jichitaiCd;

    @Id
    @Column(name = "shitei_no", length = 8)
    private String shiteiNo;

    @Id
    @Column(name = "rno", precision = 3)
    private BigDecimal rno;

    @Column(name = "toroku_ymd", nullable = false)
    private LocalDate torokuYmd;

    @Column(name = "shinkoku_ymd", nullable = false)
    private LocalDate shinkokuYmd;

    @Column(name = "henko_ymd", nullable = false)
    private LocalDate henkoYmd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "jichitai_cd", referencedColumnName = "jichitai_cd", insertable = false, updatable = false),
        @JoinColumn(name = "atena_no", referencedColumnName = "atena_no", insertable = false, updatable = false)
    })
    private Atena atena;

    @Column(name = "atena_no", nullable = false, precision = 15)
    private BigDecimal atenaNo;

    @Column(name = "shisetsu_name", nullable = false, length = 200)
    private String shisetsuName;

    @Column(name = "shisetsu_name_kana", nullable = false, length = 200)
    private String shisetsuNameKana;

    @Column(name = "shisetsu_yubin_no", length = 10)
    private String shisetsuYubinNo;

    @Column(name = "shisetsu_jusho", length = 200)
    private String shisetsuJusho;

    @Column(name = "shisetsu_tel", length = 20)
    private String shisetsuTel;

    @Column(name = "yuka_menseki", precision = 9, scale = 2)
    private BigDecimal yukaMenseki;

    @Column(name = "chijo_kai", precision = 3)
    private BigDecimal chijoKai;

    @Column(name = "chika_kai", precision = 2)
    private BigDecimal chikaKai;

    @Column(name = "kyakushitsu_su", precision = 5)
    private BigDecimal kyakushitsuSu;

    @Column(name = "shuyo_su", precision = 7)
    private BigDecimal shuyoSu;

    @Column(name = "kyoka_name", nullable = false, length = 200)
    private String kyokaName;

    @Column(name = "kyoka_name_kana", nullable = false, length = 200)
    private String kyokaNameKana;

    @Column(name = "kyoka_yubin_no", length = 10)
    private String kyokaYubinNo;

    @Column(name = "kyoka_jusho", length = 200)
    private String kyokaJusho;

    @Column(name = "kyoka_tel", length = 20)
    private String kyokaTel;

    @Column(name = "kyoka_shu", length = 1)
    private String kyokaShu;

    @Column(name = "kyoka_no", length = 200)
    private String kyokaNo;

    @Column(name = "soufusaki_name", nullable = false, length = 200)
    private String soufusakiName;

    @Column(name = "soufusaki_name_kana", nullable = false, length = 200)
    private String soufusakiNameKana;

    @Column(name = "soufusaki_yubin_no", length = 10)
    private String soufusakiYubinNo;

    @Column(name = "soufusaki_jusho", length = 200)
    private String soufusakiJusho;

    @Column(name = "soufusaki_tel", length = 20)
    private String soufusakiTel;

    @Column(name = "biko", length = 400)
    private String biko;

    @Column(name = "eigyo_st_ymd", nullable = false)
    private LocalDate eigyoStYmd;

    @Column(name = "eigyo_ed_ymd")
    private LocalDate eigyoEdYmd;

    @Column(name = "kyushi_st_ymd")
    private LocalDate kyushiStYmd;

    @Column(name = "kyushi_ed_ymd")
    private LocalDate kyushiEdYmd;

    @Column(name = "kyuhaishi_riyu", length = 400)
    private String kyuhaishiRiyu;

    @Column(name = "eltax_umu", length = 1)
    private String eltaxUmu;

    @Column(name = "nokigen", length = 2)
    private String nokigen;

    @Column(name = "new_flg", nullable = false, length = 1)
    private String newFlg;

    @Column(name = "del_flg", nullable = false, length = 1)
    private String delFlg;

    @Column(name = "add_dt", nullable = false)
    private LocalDateTime addDt;

    @Column(name = "add_user", nullable = false, length = 20)
    private String addUser;

    @Column(name = "upd_dt", nullable = false)
    private LocalDateTime updDt;

    @Column(name = "upd_user", nullable = false, length = 20)
    private String updUser;

    @Column(name = "version", nullable = false, precision = 5)
    private BigDecimal version;

    /**
     * ステータスを判定する
     * 1：営業中 / 2：休止 / 3：廃止
     */
    public String getStatus() {
        LocalDate now = LocalDate.now();
        
        // 廃止判定
        if (eigyoEdYmd != null && eigyoEdYmd.isBefore(now)) {
            return "3";
        }
        
        // 休止判定
        if (kyushiStYmd != null && kyushiEdYmd != null) {
            if (!kyushiStYmd.isAfter(now) && !kyushiEdYmd.isBefore(now)) {
                return "2";
            }
        }
        
        // 営業中判定
        if (eigyoStYmd != null && !eigyoStYmd.isAfter(now)) {
            return "1";
        }
        
        return "1"; // デフォルトは営業中
    }
}