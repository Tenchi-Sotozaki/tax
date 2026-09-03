package jp.lg.asp.accommodation.service.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.lg.asp.accommodation.dto.BankImportResultDto;
import jp.lg.asp.accommodation.service.BankImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 金融機関コード取込 Service 実装
 *
 * ZenginCode（zengin-code/source-data）のzipを解凍しながらJSONを読み取り、
 * 一時テーブル経由で金融機関マスタ・支店マスタを丸ごと置き換える。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankImportServiceImpl implements BankImportService {

	/** zip内のパス（先頭のリポジトリ名フォルダは可変のため後方一致で判定する） */
	private static final String PATH_BANKS = "/data/banks.json";
	private static final String PATH_BRANCHES = "/data/branches/";
	private static final String PATH_UPDATED_AT = "/data/updated_at";

	/** バルクインサートのバッチサイズ */
	private static final int BATCH_SIZE = 1000;

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public BankImportResultDto importFromZip(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalStateException("ファイルを選択してください。");
		}

		ZipContent content = readZip(file);

		if (content.banks().isEmpty()) {
			throw new IllegalStateException("zip内に data/banks.json が見つかりません。ZenginCode のzipファイルを選択してください。");
		}

		// 金融機関マスタに存在しない金融機関コードの支店は取り込まない
		List<BranchRow> branches = new ArrayList<>();
		int skippedBankCount = 0;
		int skippedBranchCount = 0;
		for (Map.Entry<String, List<BranchRow>> entry : content.branches().entrySet()) {
			if (content.banks().containsKey(entry.getKey())) {
				branches.addAll(entry.getValue());
			} else {
				skippedBankCount++;
				skippedBranchCount += entry.getValue().size();
				log.warn("金融機関マスタに存在しないため支店を取り込みません: bankCode={}, 支店件数={}",
						entry.getKey(), entry.getValue().size());
			}
		}

		List<BankRow> banks = new ArrayList<>(content.banks().values());
		replaceMaster(banks, branches);

		BankImportResultDto result = new BankImportResultDto();
		result.setBankCount(banks.size());
		result.setBranchCount(branches.size());
		result.setSkippedBankCount(skippedBankCount);
		result.setSkippedBranchCount(skippedBranchCount);
		result.setUpdatedAt(content.updatedAt());

		log.info("金融機関コード取込完了: 金融機関={}件, 支店={}件, スキップ={}コード/{}件, updated_at={}",
				result.getBankCount(), result.getBranchCount(),
				result.getSkippedBankCount(), result.getSkippedBranchCount(), result.getUpdatedAt());

		return result;
	}

	// ========== zip読み取り ==========

	/**
	 * zipを解凍しながら必要なファイルだけを読み取る。
	 * 支店ファイルが金融機関ファイルより先に現れても処理できるよう、いったんすべて保持する。
	 */
	private ZipContent readZip(MultipartFile file) {
		Map<String, BankRow> banks = new LinkedHashMap<>();
		Map<String, List<BranchRow>> branches = new LinkedHashMap<>();
		String updatedAt = null;

		try {
			byte[] header = file.getBytes();
			if (header.length < 4
					|| header[0] != 0x50 || header[1] != 0x4B
					|| header[2] != 0x03 || header[3] != 0x04) {
				log.error("zipマジックバイトが不正です: ファイル名={}", file.getOriginalFilename());
				throw new IllegalStateException("zipファイルの読み取りに失敗しました。ファイルが壊れていないか確認してください。");
			}
		} catch (IOException e) {
			log.error("zipファイルのヘッダ読み取りに失敗しました: ファイル名={}", file.getOriginalFilename(), e);
			throw new IllegalStateException("zipファイルの読み取りに失敗しました。ファイルが壊れていないか確認してください。");
		}

		try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(file.getInputStream()),
				StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				String name = "/" + entry.getName();

				if (name.endsWith(PATH_BANKS)) {
					banks.putAll(parseBanks(readEntry(zis)));
				} else if (name.endsWith(PATH_UPDATED_AT)) {
					updatedAt = new String(readEntry(zis), StandardCharsets.UTF_8).trim();
				} else if (name.contains(PATH_BRANCHES) && name.endsWith(".json")) {
					String bankCode = name.substring(name.lastIndexOf('/') + 1, name.length() - ".json".length());
					branches.put(bankCode, parseBranches(bankCode, readEntry(zis)));
				}
			}
		} catch (IOException e) {
			log.error("zipファイルの読み取りに失敗しました", e);
			throw new IllegalStateException("zipファイルの読み取りに失敗しました。ファイルが壊れていないか確認してください。");
		}

		return new ZipContent(banks, branches, updatedAt);
	}

	/** zipエントリの内容をすべて読み出す */
	private byte[] readEntry(ZipInputStream zis) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int len;
		while ((len = zis.read(buffer)) > 0) {
			out.write(buffer, 0, len);
		}
		return out.toByteArray();
	}

	/**
	 * banks.json をパースする。
	 * 形式: { "0001": { "code":"0001", "name":"みずほ", "kana":"ミズホ", ... }, ... }
	 */
	private Map<String, BankRow> parseBanks(byte[] json) {
		Map<String, BankRow> result = new LinkedHashMap<>();
		JsonNode root = readTree(json, "data/banks.json");
		for (Iterator<String> it = root.fieldNames(); it.hasNext();) {
			String code = it.next();
			JsonNode node = root.get(code);
			result.put(code, new BankRow(code, text(node, "name"), text(node, "kana")));
		}
		return result;
	}

	/**
	 * branches/&lt;金融機関コード&gt;.json をパースする。
	 * 形式: { "001": { "code":"001", "name":"東京営業部", "kana":"トウキヨウ", ... }, ... }
	 */
	private List<BranchRow> parseBranches(String bankCode, byte[] json) {
		List<BranchRow> result = new ArrayList<>();
		JsonNode root = readTree(json, "data/branches/" + bankCode + ".json");
		for (Iterator<String> it = root.fieldNames(); it.hasNext();) {
			String code = it.next();
			JsonNode node = root.get(code);
			result.add(new BranchRow(bankCode, code, text(node, "name"), text(node, "kana")));
		}
		return result;
	}

	private JsonNode readTree(byte[] json, String path) {
		try {
			return objectMapper.readTree(json);
		} catch (IOException e) {
			log.error("JSONの解析に失敗しました: {}", path, e);
			throw new IllegalStateException("JSONの解析に失敗しました。（" + path + "）");
		}
	}

	private String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value != null && !value.isNull() ? value.asText() : "";
	}

	// ========== マスタ置き換え ==========

	/**
	 * 一時テーブルへ流し込んだうえで、金融機関マスタ・支店マスタを置き換える。
	 * 呼び出し元の1トランザクション内で完結させる。
	 */
	private void replaceMaster(List<BankRow> banks, List<BranchRow> branches) {
		LocalDateTime now = LocalDateTime.now();
		String user = getCurrentUser();

		// 前回異常終了時の残骸を考慮して先に破棄する
		jdbcTemplate.execute("DROP TABLE IF EXISTS tmp_bank_work");
		jdbcTemplate.execute("DROP TABLE IF EXISTS tmp_branch_work");

		jdbcTemplate.execute("CREATE TEMP TABLE tmp_bank_work AS SELECT * FROM m_bank WITH NO DATA");
		jdbcTemplate.execute("CREATE TEMP TABLE tmp_branch_work AS SELECT * FROM m_branch WITH NO DATA");

		insertBanks(banks, now, user);
		insertBranches(branches, now, user);

		jdbcTemplate.execute("TRUNCATE TABLE m_bank");
		jdbcTemplate.execute("TRUNCATE TABLE m_branch");

		jdbcTemplate.execute("INSERT INTO m_bank SELECT * FROM tmp_bank_work");
		jdbcTemplate.execute("INSERT INTO m_branch SELECT * FROM tmp_branch_work");

		jdbcTemplate.execute("DROP TABLE tmp_bank_work");
		jdbcTemplate.execute("DROP TABLE tmp_branch_work");
	}

	private void insertBanks(List<BankRow> banks, LocalDateTime now, String user) {
		String sql = "INSERT INTO tmp_bank_work"
				+ " (bank_code, bank_name, bank_kana, add_dt, add_user, upd_dt, upd_user, version)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		Timestamp ts = Timestamp.valueOf(now);

		for (int start = 0; start < banks.size(); start += BATCH_SIZE) {
			List<BankRow> chunk = banks.subList(start, Math.min(start + BATCH_SIZE, banks.size()));
			jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					BankRow row = chunk.get(i);
					ps.setString(1, row.code());
					ps.setString(2, row.name());
					ps.setString(3, row.kana());
					ps.setTimestamp(4, ts);
					ps.setString(5, user);
					ps.setTimestamp(6, ts);
					ps.setString(7, user);
					ps.setInt(8, 1);
				}

				@Override
				public int getBatchSize() {
					return chunk.size();
				}
			});
		}
	}

	private void insertBranches(List<BranchRow> branches, LocalDateTime now, String user) {
		String sql = "INSERT INTO tmp_branch_work"
				+ " (bank_code, branch_code, branch_name, branch_kana, add_dt, add_user, upd_dt, upd_user, version)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		Timestamp ts = Timestamp.valueOf(now);

		for (int start = 0; start < branches.size(); start += BATCH_SIZE) {
			List<BranchRow> chunk = branches.subList(start, Math.min(start + BATCH_SIZE, branches.size()));
			jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					BranchRow row = chunk.get(i);
					ps.setString(1, row.bankCode());
					ps.setString(2, row.code());
					ps.setString(3, row.name());
					ps.setString(4, row.kana());
					ps.setTimestamp(5, ts);
					ps.setString(6, user);
					ps.setTimestamp(7, ts);
					ps.setString(8, user);
					ps.setInt(9, 1);
				}

				@Override
				public int getBatchSize() {
					return chunk.size();
				}
			});
		}
	}

	private String getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.isAuthenticated() ? auth.getName() : "system";
	}

	// ========== 内部データ構造 ==========

	/** zipから読み取った内容 */
	private record ZipContent(
			Map<String, BankRow> banks,
			Map<String, List<BranchRow>> branches,
			String updatedAt) {
	}

	/** 金融機関1件 */
	private record BankRow(String code, String name, String kana) {
	}

	/** 支店1件 */
	private record BranchRow(String bankCode, String code, String name, String kana) {
	}
}
