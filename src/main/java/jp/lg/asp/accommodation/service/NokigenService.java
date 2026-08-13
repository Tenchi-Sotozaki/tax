package jp.lg.asp.accommodation.service;

import java.util.List;
import java.util.Map;

import jp.lg.asp.accommodation.entity.Nokigen;

public interface NokigenService {

    List<Nokigen> findAll();

    Nokigen findByNendo(String nendo);

    boolean existsByNendo(String nendo);

    Nokigen save(Nokigen nokigen);
    
    public Map<String, String> getPrevDataWithShift(Nokigen prev, String targetNendo, String shiftMode);
}
