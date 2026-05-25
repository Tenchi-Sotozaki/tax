package jp.lg.asp.accommodation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.FurikomiKozaId;

@Repository
public interface FurikomiKozaRepository extends JpaRepository<FurikomiKoza, FurikomiKozaId> {

	Optional<FurikomiKoza> findByJichitaiCdAndShiteiNo(String jichitaiCd, String shiteiNo);
}