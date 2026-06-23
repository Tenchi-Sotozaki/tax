package jp.lg.asp.accommodation.service.impl;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.service.ReportsConfigService;

@Service
public class ReportsConfigServiceImpl implements ReportsConfigService {

	@Autowired
	private ReportsDefRepository reportsDefRepository;
	
	@Autowired
	private DataSource dataSource;

	@Override
	public List<ReportsDef> getImportHistory() {
		return reportsDefRepository.findAll();
	}

	@Override
	@Transactional
	public void importReportFile(MultipartFile file, String jichitaiCd, String userId) {
		try (Connection connection = dataSource.getConnection()) {
			LocalDateTime now = LocalDateTime.now();
			
			System.out.println("File info: name=" + file.getOriginalFilename() + ", size=" + file.getSize() + ", type=" + file.getContentType());
			
			String sql = "INSERT INTO m_reports_def (jichitai_cd, id, kbn, def_text, def_data, add_dt, add_user, upd_dt, upd_user, version) " +
						 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
						 "ON CONFLICT (jichitai_cd, id) DO UPDATE SET " +
						 "kbn = EXCLUDED.kbn, def_text = EXCLUDED.def_text, def_data = EXCLUDED.def_data, " +
						 "upd_dt = EXCLUDED.upd_dt, upd_user = EXCLUDED.upd_user, version = m_reports_def.version + 1";
			
			try (PreparedStatement stmt = connection.prepareStatement(sql)) {
				stmt.setString(1, jichitaiCd);
				stmt.setString(2, "RPT0000001");
				stmt.setString(3, "2"); // 2：バイナリ
				stmt.setString(4, ""); // 空文字を設定
				stmt.setBinaryStream(5, new ByteArrayInputStream(file.getBytes()), (int) file.getSize());
				stmt.setObject(6, now);
				stmt.setString(7, userId);
				stmt.setObject(8, now);
				stmt.setString(9, userId);
				stmt.setInt(10, 1);
				
				int result = stmt.executeUpdate();
				System.out.println("Upsert result: " + result + " rows affected");
			}
		} catch (SQLException e) {
			System.err.println("SQL Error: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("SQLエラー: " + e.getMessage(), e);
		} catch (Exception e) {
			System.err.println("General Error: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("帳票ファイルの取り込みに失敗しました", e);
		}
	}
}