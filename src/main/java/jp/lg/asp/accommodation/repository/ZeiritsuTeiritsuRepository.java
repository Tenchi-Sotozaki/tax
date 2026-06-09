package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
// ※DB構造に合わせたComposite Keyクラス（ZeiritsuTeiritsuId等）のimportが含まれている前提とします
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsuId;

/*
 * 税率定率詳細マスタのデータアクセス用リポジトリ
 */
@Repository
public interface ZeiritsuTeiritsuRepository extends JpaRepository<ZeiritsuTeiritsu, ZeiritsuTeiritsuId> {

    /*
     * 自治体コード、連番、削除フラグを条件に、定率連番の昇順で定率詳細リストを取得する
     * （※もともと定義されていたSpring Data JPAの自動生成メソッド）
     */
    List<ZeiritsuTeiritsu> findByJichitaiCdAndSeqAndDelFlgOrderByTeiritsuSeqAsc(String jichitaiCd, BigDecimal seq, String delFlg);

    /*
     * Service層の既存呼び出し（引数3つ）に適合させるためのメソッド。
     * m_zeiritsu（税率管理マスタ）と結合し、指定した対象区分、適用開始年月、適用終了年月
     * に一致する有効（del_flg='0'）な定率詳細リストを取得する。
     */
    @Query(value =
        "SELECT t.* FROM m_zeiritsu_teiritsu t " +
        "INNER JOIN m_zeiritsu m " +
        "  ON t.jichitai_cd = m.jichitai_cd AND t.seq = m.seq " +
        "WHERE m.taisho_kbn = :taishoKbn " +
        "  AND m.tekiyo_st_ym = :tekiyoStYm " +
        // 終了年月はNULLの可能性もあるため、安全に比較する
        "  AND (m.tekiyo_ed_ym = :tekiyoEdYm OR (m.tekiyo_ed_ym IS NULL AND :tekiyoEdYm IS NULL)) " +
        "  AND t.del_flg = '0' AND m.del_flg = '0'",
        nativeQuery = true)
    List<ZeiritsuTeiritsu> findActiveByTaishoKbnAndTekiyoYm(
        @Param("taishoKbn") String taishoKbn,
        @Param("tekiyoStYm") String tekiyoStYm,
        @Param("tekiyoEdYm") String tekiyoEdYm
    );
}