package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class CollectorSearchForm {

    /** \u6307\u5b9a\u756a\u53f7 (shitei_no) */
    private String shiteiNo;

    /** \u6c0f\u540d/\u540d\u79f0 (name) */
    private String name;

    /** \u65bd\u8a2d\u540d\u79f0 (shisetsu_name) */
    private String shisetsuName;

    /** \u55b6\u696d\u7a2e\u5225 (kyoka_shu): 1=\u30db\u30c6\u30eb, 2=\u65c5\u9928, 3=\u7c21\u6613\u5bbf\u6240, 4=\u6c11\u6cca, 999=\u3059\u3079\u3066 */
    private String kyokaShu;

    /** \u5408\u7b97\u5bfe\u8c61: 1=\u975e\u5bfe\u8c61, 2=\u5bfe\u8c61, 999=\u3059\u3079\u3066 */
    private String gasanTaisho;

    /** \u30b9\u30c6\u30fc\u30bf\u30b9: 1=\u55b6\u696d\u4e2d, 2=\u4f11\u6b62, 3=\u5ec3\u6b62, 4=\u3059\u3079\u3066 */
    private String status;

    /** \u500b\u4eba\u756a\u53f7 (kojin_no) */
    private String kojinNo;

    /** \u6cd5\u4eba\u756a\u53f7 (hojin_no) */
    private String hojinNo;
}
