package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.AccommodationFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccommodationFacilityRepository extends JpaRepository<AccommodationFacility, String> {
    List<AccommodationFacility> findByCollector_CollectorId(String collectorId);
}
