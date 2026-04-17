package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class CollectorSearchForm {

    /** No.1 \u6307\u5b9a\u756a\u53f7 (t_tokugimu.shitei_no) */
    private String shiteiNo;

    /** No.2 \u6c0f\u540d/\u540d\u79f0 (m_atena.name) */
    private String name;

    /** No.3 \u65bd\u8a2d\u540d\u79f0 (t_tokugimu.shisetsu_name) */
    private String shisetsuName;

    /** No.4 \u55b6\u696d\u7a2e\u5225 (t_tokugimu.kyoka_shu): 1=\u30db\u30c6\u30eb/2=\u65c5\u9928/3=\u7c21\u6613\u5bbf\u6240/4=\u6c11\u6cca/999=\u3059\u3079\u3066 */
    private String kyokaShu = "999";

    /** No.5 \u5408\u7b97\u5bfe\u8c61 (t_gassan_uchi.shitei_no, gassan_shitei_no): 1=\u975e\u5bfe\u8c61/2=\u5bfe\u8c61/999=\u3059\u3079\u3066 */
    private String gasanTaisho = "999";

    /**
     * No.6 \u30b9\u30c6\u30fc\u30bf\u30b9 (t_tokugimu.eigyo_st_ymd, eigyo_ed_ymd, kyushi_st_ymd, kyushi_ed_ymd\u306e\u767b\u9332\u72b6\u6cc1\u304b\u3089\u5224\u5b9a)
     * 1=\u55b6\u696d\u4e2d/2=\u4f11\u6b62/3=\u5ec3\u6b62/4=\u3059\u3079\u3066
     */
    private String status = "4";

    /** No.7 \u500b\u4eba\u756a\u53f7 (m_atena.kojin_no) */
    private String kojinNo;

    /** No.8 \u6cd5\u4eba\u756a\u53f7 (m_atena.hojin_no) */
    private String hojinNo;
}
