package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.KoinTorikomi;
import jp.lg.asp.accommodation.repository.KoinTorikomiRepository;
import jp.lg.asp.accommodation.service.KoinTorikomiService;

@Service
public class KoinTorikomiServiceImpl implements KoinTorikomiService {

	@Autowired
	private KoinTorikomiRepository koinTorikomiRepository;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Override
	public List<KoinTorikomi> getImportHistory() {
		return koinTorikomiRepository.findAll();
	}

	@Override
	public void importReportFile(MultipartFile file, String jichitaiCd, String userId) {
	    String sql = "INSERT INTO t_koin_torikomi (jichitai_cd, seq, file_name, torikomi_dt, torikomi_user, add_dt, add_user, upd_dt, upd_user, version) " +
	                 "VALUES (:jichitaiCd, :seq, :fileName, :torikomiDt, :torikomiUser, :addDt, :addUser, :updDt, :updUser, :version) " +
	                 "ON CONFLICT (jichitai_cd, seq) DO UPDATE SET " +
	                 "file_name = EXCLUDED.file_name, " +
	                 "torikomi_dt = EXCLUDED.torikomi_dt, " +
	                 "upd_dt = EXCLUDED.upd_dt, " +
	                 "version = t_koin_torikomi.version + 1";

	    MapSqlParameterSource params = new MapSqlParameterSource()
	        .addValue("jichitaiCd", jichitaiCd)
	        .addValue("seq", 1L)
	        .addValue("fileName", file.getOriginalFilename())
	        .addValue("torikomiDt", LocalDateTime.now())
	        .addValue("torikomiUser", userId)
	        .addValue("addDt", LocalDateTime.now())
	        .addValue("addUser", userId)
	        .addValue("updDt", LocalDateTime.now())
	        .addValue("updUser", userId)
	        .addValue("version", 1);

	    namedParameterJdbcTemplate.update(sql, params);
	}
}