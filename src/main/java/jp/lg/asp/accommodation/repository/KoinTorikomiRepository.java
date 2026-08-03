package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.KoinTorikomi;
import jp.lg.asp.accommodation.entity.KoinTorikomiId;

@Repository
public interface KoinTorikomiRepository extends JpaRepository<KoinTorikomi, KoinTorikomiId> {
}