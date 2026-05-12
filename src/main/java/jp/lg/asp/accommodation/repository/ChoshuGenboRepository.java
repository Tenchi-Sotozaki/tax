package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ChoshuGenbo;
import jp.lg.asp.accommodation.entity.ChoshuGenboId;

@Repository
public interface ChoshuGenboRepository extends JpaRepository<ChoshuGenbo, ChoshuGenboId> {
    // 基本的なCRUD操作は JpaRepository が提供してくれるぜ
}