package jp.lg.asp.accommodation.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.AtenaRenkei;
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
	private final HashUtil hashUtil;

	@Override
	@Transactional
	public AtenaRenkei importCsv(MultipartFile file, String jichitaiCd, String userId) {
		int shinkiKensu = 0;
		int koshinKensu = 0;
		int gyosu = 0;

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

			// ヘッダー行チェック
			String headerLine = reader.readLine();
			if (headerLine == null) {
				throw new RuntimeException("CSVファイルが空です。");
			}
			
			// BOM (Byte Order Mark) を除去
			headerLine = removeBOM(headerLine);
			
			// ヘッダーのフォーマットチェック
			validateHeader(headerLine);

			String line;
			while ((line = reader.readLine()) != null) {
				gyosu++;
				if (line.isBlank()) {
					continue;
				}
				
				// データ行のフォーマットチェック
				String[] cols = validateAndParseDataLine(line, gyosu);
				
				if (cols == null) {
					continue; // 空行はスキップ
				}

				BigDecimal atenaNo = new BigDecimal(cols[0].trim());
				String kojinNo = cols.length > 1 ? cols[1].trim() : null;
				String hojinNo = cols.length > 2 ? cols[2].trim() : null;
				String name = cols[3].trim();
				String nameKana = cols[4].trim();
				String yubinNo = cols[5].trim();
				String jusho = cols[6].trim();
				String tel1 = cols[7].trim();
				String tel2 = cols.length > 8 ? cols[8].trim() : null;

				AtenaId pk = new AtenaId();
				pk.setJichitaiCd(jichitaiCd);
				pk.setAtenaNo(atenaNo);

				boolean isNew = !atenaRepository.existsById(pk);
				Atena atena = atenaRepository.findById(pk).orElse(new Atena());

				atena.setJichitaiCd(jichitaiCd);
				atena.setAtenaNo(atenaNo);
				atena.setKbn(kojinNo != null && !kojinNo.isBlank() ? "1" : "2");
				atena.setName(name);
				atena.setNameKana(nameKana.isBlank() ? null : nameKana);
				atena.setYubinNo(yubinNo.isBlank() ? null : yubinNo);
				atena.setJusho(jusho.isBlank() ? null : jusho);
				atena.setTel1(tel1.isBlank() ? null : tel1);
				atena.setTel2(tel2 == null || tel2.isBlank() ? null : tel2);
				atena.setKojinNo(kojinNo == null || kojinNo.isBlank() ? null : hashUtil.sha256(kojinNo));
				atena.setHojinNo(hojinNo == null || hojinNo.isBlank() ? null : hojinNo);
				atenaRepository.save(atena);

				if (isNew)
					shinkiKensu++;
				else
					koshinKensu++;
			}
		} catch (RuntimeException e) {
			// フォーマットエラーやデータエラーなRuntimeExceptionはメッセージをそのまま使用
			log.warn("CSV処理エラー: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			// その他の予期しないエラー
			log.error("CSV取込中に予期しないエラーが発生しました", e);
			throw new RuntimeException("CSV取込に失敗しました: " + e.getMessage(), e);
		}

		BigDecimal nextSeq = atenaRenkeiRepository.findMaxSeqByJichitaiCd(jichitaiCd).add(BigDecimal.ONE);
		AtenaRenkei renkei = new AtenaRenkei();
		renkei.setJichitaiCd(jichitaiCd);
		renkei.setSeq(nextSeq);
		renkei.setFileName(file.getOriginalFilename());
		renkei.setShoriDt(LocalDateTime.now());
		renkei.setShoriKensu(BigDecimal.valueOf(shinkiKensu + koshinKensu));
		renkei.setShinkiKensu(BigDecimal.valueOf(shinkiKensu));
		renkei.setKoshinKensu(BigDecimal.valueOf(koshinKensu));
		return atenaRenkeiRepository.save(renkei);
	}

	@Override
	public List<AtenaRenkei> findHistory(String jichitaiCd) {
		return atenaRenkeiRepository.findByJichitaiCdOrderBySeqDesc(jichitaiCd);
	}

	/**
	 * ヘッダー行のフォーマットチェック
	 */
	private void validateHeader(String headerLine) {
		if (headerLine == null || headerLine.trim().isEmpty()) {
			throw new RuntimeException("ヘッダー行が空です。");
		}
		
		log.debug("ヘッダー行の検証開始: [{}]", headerLine);
		
		String[] headers = headerLine.split(",", -1);
		log.debug("ヘッダー項目数: {}", headers.length);
		
		if (headers.length < 8) {
			throw new RuntimeException(
					"CSVファイルのフォーマットが不正です。\n" +
					"期待するフォーマット: 宛名番号,個人番号,法人番号,氏名,氏名カナ,郵便番号,住所,電話番号1[、電話番号2]");
		}
		
		// 期待されるヘッダー名とのチェック
		String[] expectedHeaders = {
				"宛名番号", "個人番号", "法人番号", "氏名/名称", "ふりがな", 
				"郵便番号", "住所", "電話番号", "電話番号"
		};
		
		for (int i = 0; i < Math.min(expectedHeaders.length, headers.length); i++) {
			// トリム、スペース除去、全角・半角正規化を行う
			String actual = normalizeHeader(headers[i]);
			String expected = normalizeHeader(expectedHeaders[i]);
			
			log.debug("ヘッダー検証[{}]: 期待値=[{}], 実際の値=[{}]", i + 1, expected, actual);
			
			if (!actual.equals(expected)) {
				log.error("ヘッダー不一致: 位置={}, 期待=[{}](長さ:{}), 実際=[{}](長さ:{})", 
						i + 1, expected, expected.length(), actual, actual.length());
						
				// 文字コードレベルでの比較
				for (int j = 0; j < Math.max(expected.length(), actual.length()); j++) {
					char expectedChar = j < expected.length() ? expected.charAt(j) : ' ';
					char actualChar = j < actual.length() ? actual.charAt(j) : ' ';
					if (expectedChar != actualChar) {
						log.error("文字不一致位置[{}]: 期待='{}' (\\u{:04x}), 実際='{}' (\\u{:04x})", 
								j, expectedChar, (int)expectedChar, actualChar, (int)actualChar);
						break; // 最初の不一致だけ表示
					}
				}
				
				throw new RuntimeException(
						String.format("ヘッダーの%d番目の項目が不正です。\n" +
								"期待値: [%s], 実際の値: [%s]\n" +
								"元のヘッダー: [%s]", i + 1, expected, actual, headers[i]));
			}
		}
		
		log.debug("ヘッダー検証成功");
	}
	
	/**
	 * ヘッダー文字列を正規化する
	 */
	private String normalizeHeader(String header) {
		if (header == null) {
			return "";
		}
		
		return header.trim()
				.replaceAll("\\s+", "")  // すべての空白文字を除去
				.replaceAll("（", "(")   // 全角かっこを半角に
				.replaceAll("）", ")")   // 全角かっこを半角に
				.replaceAll("１", "1")   // 全角数字を半角に
				.replaceAll("２", "2");
	}

	/**
	 * データ行のフォーマットチェックとパース
	 */
	private String[] validateAndParseDataLine(String line, int rowNumber) {
		if (line == null || line.trim().isEmpty()) {
			return null; // 空行はスキップ
		}
		
		String[] cols = line.split(",", -1);
		if (cols.length < 8) {
			throw new RuntimeException(
					String.format("%d行目: データの項目数が不足です。(期待: 最低8項目, 実際: %d項目)", 
							rowNumber + 1, cols.length));
		}
		
		// 宛名番号(必須)のチェック
		String atenaNoStr = cols[0].trim();
		if (atenaNoStr.isEmpty()) {
			throw new RuntimeException(
					String.format("%d行目: 宛名番号が空です。", rowNumber + 1));
		}
		
		try {
			new BigDecimal(atenaNoStr);
		} catch (NumberFormatException e) {
			throw new RuntimeException(
					String.format("%d行目: 宛名番号が数値ではありません。(値: %s)", 
							rowNumber + 1, atenaNoStr));
		}
		
		// 氏名(必須)のチェック
		String name = cols[3].trim();
		if (name.isEmpty()) {
			throw new RuntimeException(
					String.format("%d行目: 氏名が空です。", rowNumber + 1));
		}
		
		// 氏名カナ(必須)のチェック
		String nameKana = cols[4].trim();
		if (nameKana.isEmpty()) {
			throw new RuntimeException(
					String.format("%d行目: 氏名カナが空です。", rowNumber + 1));
		}
		
		// 電話番号1(必須)のチェック
		String tel1 = cols[7].trim();
		if (tel1.isEmpty()) {
			throw new RuntimeException(
					String.format("%d行目: 電話番号1が空です。", rowNumber + 1));
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
		
		// UTF-8 BOM (\ufeff) を除去
		if (text.charAt(0) == '\ufeff') {
			log.debug("BOMを検出しました。除去します。");
			return text.substring(1);
		}
		
		return text;
	}

}
