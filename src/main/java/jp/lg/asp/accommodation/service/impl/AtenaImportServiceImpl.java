package jp.lg.asp.accommodation.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.AtenaImportDiffDto;
import jp.lg.asp.accommodation.dto.AtenaImportPreviewDto;
import jp.lg.asp.accommodation.dto.AtenaImportRowDto;
import jp.lg.asp.accommodation.dto.AtenaImportValueDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.AtenaRenkei;
import jp.lg.asp.accommodation.entity.AtenaRenkeiDef;
import jp.lg.asp.accommodation.repository.AtenaRenkeiDefRepository;
import jp.lg.asp.accommodation.repository.AtenaRenkeiRepository;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.service.AtenaImportService;
import jp.lg.asp.accommodation.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtenaImportServiceImpl implements AtenaImportService {

	private final AtenaRepository atenaRepository;
	private final AtenaRenkeiRepository atenaRenkeiRepository;
	private final AtenaRenkeiDefRepository atenaRenkeiDefRepository;
	private final HashUtil hashUtil;

	// ============================================================
	// 解析フェーズ
	// ============================================================

	@Override
	public AtenaImportPreviewDto analyze(MultipartFile file, String jichitaiCd) {
		AtenaImportPreviewDto preview = new AtenaImportPreviewDto();
		preview.setFileName(file.getOriginalFilename());

		int gyosu = 0;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

			// ヘッダー行チェック
			String headerLine = reader.readLine();
			if (headerLine == null) {
				throw new RuntimeException("CSVファイルが空です。");
			}
			headerLine = removeBOM(headerLine);
			validateHeader(headerLine);

			String line;
			while ((line = reader.readLine()) != null) {
				gyosu++;
				if (line.isBlank()) {
					continue;
				}

				String[] cols = validateAndParseDataLine(line, gyosu);
				if (cols == null) {
					continue;
				}

				AtenaImportValueDto value = toValue(cols);

				AtenaId pk = new AtenaId();
				pk.setJichitaiCd(jichitaiCd);
				pk.setAtenaNo(new BigDecimal(value.getAtenaNo()));
				Atena current = atenaRepository.findById(pk).orElse(null);

				AtenaImportRowDto row = new AtenaImportRowDto();
				row.setAtenaNo(value.getAtenaNo());
				row.setName(value.getName());
				row.setValue(value);

				if (current == null) {
					// 既存データなし。新規登録のため差分確認の対象外とする
					row.setShinki(true);
					row.setSabunAri(false);
				} else {
					row.setShinki(false);
					List<AtenaImportDiffDto> diffs = buildDiffs(current, value);
					row.setDiffs(diffs);
					row.setSabunAri(diffs.stream().anyMatch(AtenaImportDiffDto::isChanged));
				}
				preview.getRows().add(row);
			}
		} catch (RuntimeException e) {
			log.warn("CSV解析エラー: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("CSV解析中に予期しないエラーが発生しました", e);
			throw new RuntimeException("CSV取込に失敗しました: " + e.getMessage(), e);
		}
		return preview;
	}

	/**
	 * 既存データとCSVの値を項目単位で比較する。
	 */
	private List<AtenaImportDiffDto> buildDiffs(Atena current, AtenaImportValueDto value) {
		List<AtenaImportDiffDto> diffs = new ArrayList<>();
		diffs.add(diff("氏名/名称", current.getName(), value.getName()));
		diffs.add(diff("ふりがな", current.getNameKana(), value.getNameKana()));
		diffs.add(diff("郵便番号", current.getYubinNo(), value.getYubinNo()));
		diffs.add(diff("住所", current.getJusho(), value.getJusho()));
		diffs.add(diff("電話番号1", current.getTel1(), value.getTel1()));
		diffs.add(diff("電話番号2", current.getTel2(), value.getTel2()));
		diffs.add(diff("法人番号", current.getHojinNo(), value.getHojinNo()));
		// 個人番号はハッシュ化して保持しているため、値そのものは表示しない
		AtenaImportDiffDto kojinNo = diff("個人番号", current.getKojinNo(), value.getKojinNo());
		kojinNo.setCurrent(maskKojinNo(current.getKojinNo()));
		kojinNo.setUpdated(maskKojinNo(value.getKojinNo()));
		diffs.add(kojinNo);
		return diffs;
	}

	private AtenaImportDiffDto diff(String label, String current, String updated) {
		String c = current == null ? "" : current;
		String u = updated == null ? "" : updated;
		return new AtenaImportDiffDto(label, c, u, !Objects.equals(c, u));
	}

	private String maskKojinNo(String hashed) {
		return (hashed == null || hashed.isBlank()) ? "" : "設定あり";
	}

	// ============================================================
	// 確定フェーズ
	// ============================================================

	@Override
	@Transactional
	public AtenaRenkei confirm(AtenaImportPreviewDto preview, Set<String> torikomuAtenaNo,
			String jichitaiCd, String userId) {

		BigDecimal nextSeq = atenaRenkeiRepository.findMaxSeqByJichitaiCd(jichitaiCd).add(BigDecimal.ONE);

		int shinkiKensu = 0;
		int koshinKensu = 0;

		for (AtenaImportRowDto row : preview.getRows()) {
			String kbn;
			if (row.isShinki()) {
				// 新規は無条件に登録する
				saveAtena(row.getValue(), jichitaiCd);
				shinkiKensu++;
				kbn = AtenaRenkeiDef.KBN_TORIKOMI;
			} else if (!row.isSabunAri()) {
				// 既存データと同一のため更新しない
				kbn = AtenaRenkeiDef.KBN_SAI_NASHI;
			} else if (torikomuAtenaNo != null && torikomuAtenaNo.contains(row.getAtenaNo())) {
				saveAtena(row.getValue(), jichitaiCd);
				koshinKensu++;
				kbn = AtenaRenkeiDef.KBN_TORIKOMI;
			} else {
				// 差分ありだが取り込まないと選択された
				kbn = AtenaRenkeiDef.KBN_SKIP;
			}
			saveRenkeiDef(jichitaiCd, nextSeq, row, kbn);
		}

		AtenaRenkei renkei = new AtenaRenkei();
		renkei.setJichitaiCd(jichitaiCd);
		renkei.setSeq(nextSeq);
		renkei.setFileName(preview.getFileName());
		renkei.setShoriDt(LocalDateTime.now());
		renkei.setShoriKensu(BigDecimal.valueOf(shinkiKensu + koshinKensu));
		renkei.setShinkiKensu(BigDecimal.valueOf(shinkiKensu));
		renkei.setKoshinKensu(BigDecimal.valueOf(koshinKensu));
		return atenaRenkeiRepository.save(renkei);
	}

	private void saveAtena(AtenaImportValueDto value, String jichitaiCd) {
		BigDecimal atenaNo = new BigDecimal(value.getAtenaNo());
		AtenaId pk = new AtenaId();
		pk.setJichitaiCd(jichitaiCd);
		pk.setAtenaNo(atenaNo);

		Atena atena = atenaRepository.findById(pk).orElse(new Atena());
		atena.setJichitaiCd(jichitaiCd);
		atena.setAtenaNo(atenaNo);
		atena.setKbn(value.getKbn());
		atena.setName(value.getName());
		atena.setNameKana(blankToNull(value.getNameKana()));
		atena.setYubinNo(blankToNull(value.getYubinNo()));
		atena.setJusho(blankToNull(value.getJusho()));
		atena.setTel1(blankToNull(value.getTel1()));
		atena.setTel2(blankToNull(value.getTel2()));
		atena.setKojinNo(blankToNull(value.getKojinNo()));
		atena.setHojinNo(blankToNull(value.getHojinNo()));
		atenaRepository.save(atena);
	}

	private void saveRenkeiDef(String jichitaiCd, BigDecimal seq, AtenaImportRowDto row, String kbn) {
		AtenaRenkeiDef def = new AtenaRenkeiDef();
		def.setJichitaiCd(jichitaiCd);
		def.setSeq(seq);
		def.setAtenaNo(new BigDecimal(row.getAtenaNo()));
		def.setName(row.getName());
		def.setKbn(kbn);
		atenaRenkeiDefRepository.save(def);
	}

	// ============================================================
	// 参照
	// ============================================================

	@Override
	public List<AtenaRenkei> findHistory(String jichitaiCd) {
		return atenaRenkeiRepository.findByJichitaiCdOrderBySeqDesc(jichitaiCd);
	}

	@Override
	public List<AtenaRenkeiDef> findDetail(String jichitaiCd, BigDecimal seq) {
		return atenaRenkeiDefRepository.findByJichitaiCdAndSeqOrderByAtenaNoAsc(jichitaiCd, seq);
	}

	// ============================================================
	// CSVパース
	// ============================================================

	/**
	 * CSVの1行を登録値に変換する。
	 */
	private AtenaImportValueDto toValue(String[] cols) {
		String atenaNo = cols[0].trim();
		String kojinNo = cols.length > 1 ? cols[1].trim() : null;
		String hojinNo = cols.length > 2 ? cols[2].trim() : null;
		String tel2 = cols.length > 8 ? cols[8].trim() : null;

		AtenaImportValueDto value = new AtenaImportValueDto();
		value.setAtenaNo(new BigDecimal(atenaNo).toPlainString());
		value.setKbn(kojinNo != null && !kojinNo.isBlank() ? "1" : "2");
		value.setName(cols[3].trim());
		value.setNameKana(cols[4].trim());
		value.setYubinNo(cols[5].trim());
		value.setJusho(cols[6].trim());
		value.setTel1(cols[7].trim());
		value.setTel2(tel2);
		value.setKojinNo(kojinNo == null || kojinNo.isBlank() ? null : hashUtil.sha256(kojinNo));
		value.setHojinNo(hojinNo == null || hojinNo.isBlank() ? null : hojinNo);
		return value;
	}

	private String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}

	/**
	 * CSVの1行を項目に分割する。
	 *
	 * Excel等から出力されたCSVは各項目がダブルクォートで囲まれる場合があるため、
	 * 引用符の内側のカンマを区切りとして扱わないようにし、前後の引用符は除去する。
	 * 引用符内の "" は 1つの " として扱う。
	 */
	private String[] parseCsvLine(String line) {
		List<String> cols = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inQuote = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (inQuote) {
				if (c == '"') {
					// 連続する引用符はエスケープされた引用符とみなす
					if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
						sb.append('"');
						i++;
					} else {
						inQuote = false;
					}
				} else {
					sb.append(c);
				}
			} else if (c == '"') {
				inQuote = true;
			} else if (c == ',') {
				cols.add(sb.toString());
				sb.setLength(0);
			} else {
				sb.append(c);
			}
		}
		cols.add(sb.toString());
		return cols.toArray(new String[0]);
	}

	/**
	 * ヘッダー行のフォーマットチェック
	 */
	private void validateHeader(String headerLine) {
		if (headerLine == null || headerLine.trim().isEmpty()) {
			throw new RuntimeException("ヘッダー行が空です。");
		}

		String[] headers = parseCsvLine(headerLine);
		if (headers.length < 8) {
			throw new RuntimeException(
					"CSVファイルのフォーマットが不正です。\n" +
					"期待するフォーマット: 宛名番号,個人番号,法人番号,氏名,氏名カナ,郵便番号,住所,電話番号1[、電話番号2]");
		}

		String[] expectedHeaders = {
				"宛名番号", "個人番号", "法人番号", "氏名/名称", "ふりがな",
				"郵便番号", "住所", "電話番号", "電話番号"
		};

		for (int i = 0; i < Math.min(expectedHeaders.length, headers.length); i++) {
			String actual = normalizeHeader(headers[i]);
			String expected = normalizeHeader(expectedHeaders[i]);
			if (!actual.equals(expected)) {
				throw new RuntimeException(
						String.format("ヘッダーの%d番目の項目が不正です。\n" +
								"期待値: [%s], 実際の値: [%s]\n" +
								"元のヘッダー: [%s]", i + 1, expected, actual, headers[i]));
			}
		}
	}

	/**
	 * ヘッダー文字列を正規化する
	 */
	private String normalizeHeader(String header) {
		if (header == null) {
			return "";
		}
		return header.trim()
				.replaceAll("\\s+", "")
				.replaceAll("（", "(")
				.replaceAll("）", ")")
				.replaceAll("１", "1")
				.replaceAll("２", "2");
	}

	/**
	 * データ行のフォーマットチェックとパース
	 */
	private String[] validateAndParseDataLine(String line, int rowNumber) {
		if (line == null || line.trim().isEmpty()) {
			return null;
		}

		String[] cols = parseCsvLine(line);
		if (cols.length < 8) {
			throw new RuntimeException(
					String.format("%d行目: データの項目数が不足です。(期待: 最低8項目, 実際: %d項目)",
							rowNumber + 1, cols.length));
		}

		String atenaNoStr = cols[0].trim();
		if (atenaNoStr.isEmpty()) {
			throw new RuntimeException(String.format("%d行目: 宛名番号が空です。", rowNumber + 1));
		}
		try {
			new BigDecimal(atenaNoStr);
		} catch (NumberFormatException e) {
			throw new RuntimeException(
					String.format("%d行目: 宛名番号が数値ではありません。(値: %s)", rowNumber + 1, atenaNoStr));
		}

		if (cols[3].trim().isEmpty()) {
			throw new RuntimeException(String.format("%d行目: 氏名が空です。", rowNumber + 1));
		}
		if (cols[4].trim().isEmpty()) {
			throw new RuntimeException(String.format("%d行目: 氏名カナが空です。", rowNumber + 1));
		}
		if (cols[7].trim().isEmpty()) {
			throw new RuntimeException(String.format("%d行目: 電話番号1が空です。", rowNumber + 1));
		}

		return cols;
	}

	/**
	 * BOM (Byte Order Mark) を除去する
	 */
	private String removeBOM(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		if (text.charAt(0) == '﻿') {
			return text.substring(1);
		}
		return text;
	}
}
