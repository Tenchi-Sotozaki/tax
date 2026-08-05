package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.entity.ScreenId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, ScreenId> {
    
    List<Screen> findByJichitaiCdOrderByScreenId(String jichitaiCd);

    /** 表示順に並べて取得する（区分の並び順も表示順に従う） */
    List<Screen> findByJichitaiCdOrderByDspOdrAsc(String jichitaiCd);
}