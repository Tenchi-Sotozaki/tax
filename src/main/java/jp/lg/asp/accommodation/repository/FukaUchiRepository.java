package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.FukaUchiId;

@Repository
public interface FukaUchiRepository extends JpaRepository<FukaUchi, FukaUchiId> {

    /**
     * 親テーブルのキー情報を元に、税区分ごとの内訳リストを取得する
     */
    List<FukaUchi> findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
            String jichitaiCd, String shiteiNo, Integer rno, String nendo, Integer kibetsu);
    
    void deleteByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
            String jichitaiCd, String shiteiNo, Integer rno, String nendo, Integer kibetsu);
}