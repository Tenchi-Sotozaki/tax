package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaId;

@Repository
public interface FukaRepository extends JpaRepository<Fuka, FukaId> {
    
    // 基本の検索（全件表示用）
    List<Fuka> findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
            String jichitaiCd, String shiteiNo, String nendo);

    // 申告済み絞り込み：shinkokuYmd が NULL ではない
    List<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndShinkokuYmdIsNotNullOrderByKibetsuAsc(
            String jichitaiCd, String shiteiNo, String nendo);

    // 未申告絞り込み：shinkokuYmd が NULL である
    List<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndShinkokuYmdIsNullOrderByKibetsuAsc(
            String jichitaiCd, String shiteiNo, String nendo);
    
    Optional<Fuka> findByJichitaiCdAndShiteiNoAndKibetsu(String jichitaiCd, String shiteiNo, Integer kibetsu);
    
    /**
     * 指定番号と対象年月をキーに、最新の履歴番号（RNO）のデータを取得する
     * 💡 Derived Query（派生クエリ：メソッド名からSpring Data JPAが自動でSQLを生成する仕組み）
     */
    Optional<Fuka> findFirstByJichitaiCdAndShiteiNoAndTaishoYmOrderByRnoDesc(
            String jichitaiCd, String shiteiNo, String taishoYm);
    
    /**
     * 指定されたキー（自治体・指定番号・年度・期別）の中で最新の（RNOが最大の）レコードを1件取得する
     * 💡 findFirst...OrderByRnoDesc という命名により、自動的に MAX(RNO) の行が選ばれるぜ。
     */
    Optional<Fuka> findFirstByJichitaiCdAndShiteiNoAndNendoAndKibetsuOrderByRnoDesc(
            String jichitaiCd, String shiteiNo, String nendo, Integer kibetsu);
}