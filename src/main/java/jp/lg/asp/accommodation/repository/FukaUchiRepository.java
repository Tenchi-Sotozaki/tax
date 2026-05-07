package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.FukaUchiId;

/**
 * 賦課内訳テーブルのリポジトリ
 */
@Repository
public interface FukaUchiRepository extends JpaRepository<FukaUchi, FukaUchiId> {

}