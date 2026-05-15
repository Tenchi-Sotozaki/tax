package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.FukaUchiId;

@Repository
public interface FukaUchiRepository extends JpaRepository<FukaUchi, FukaUchiId> {

    /**
     * 親テーブルのキー情報を元に、税区分ごとの内訳リストを取得する
     */
    List<FukaUchi> findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
            String jichitaiCd, String shiteiNo, Integer rno, String nendo, Integer kibetsu);

    /**
     * 💡 修正：命名規則による一括削除（Derived Delete Query）
     * メソッド名がそのまま削除条件になるから、@Query は不要だぜ。
     * 削除処理には @Transactional（トランザクション管理）が必要になる。
     */
    @Transactional
    void deleteByJichitaiCdAndShiteiNoAndVersionAndNendoAndKibetsu(
            String jichitaiCd, String shiteiNo, Integer version, String nendo, Integer kibetsu);
}