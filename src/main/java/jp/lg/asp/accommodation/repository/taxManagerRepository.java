package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.taxManager;
import jp.lg.asp.accommodation.entity.taxManagerId; // ★これを追加！

@Repository
public interface taxManagerRepository extends JpaRepository<taxManager, taxManagerId> {
}