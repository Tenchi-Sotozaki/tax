package jp.lg.asp.accommodation.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto.DiffRow;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.EltaxRenkeiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.constant.EltaxTetsuzukiConstants;
import jp.lg.asp.accommodation.service.EltaxRenkeiKakuninService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EltaxRenkeiKakuninServiceImpl implements EltaxRenkeiKakuninService {

	private final EltaxRenkeiRepository eltaxRenkeiRepository;
	private final TokugimuRepository tokugimuRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	@Transactional(readOnly = true)
	public EltaxRenkeiKakuninDto preview(MultipartFile file) {
		try {
			String[] dataRow = parseCsv(file);

			String tetsuzukiId = dataRow.length > 2 ? dataRow[2].trim() : "";
			String shubetsu = EltaxTetsuzukiConstants.TETSUZUKI_SHUBETSU_MAP.getOrDefault(tetsuzukiId, "");
			String shubetsuName = EltaxTetsuzukiConstants.SHUBETSU_NAME_MAP.getOrDefault(shubetsu, shubetsu);

			Map<Integer, String> yoshikiMap = loadYoshikiMap(tetsuzukiId);

			int shisetsuNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int shisetsuNameIdx = findIndexByName(yoshikiMap, "施設情報【名称");
			int shisetsuJushoIdx = findIndexByName(yoshikiMap, "施設情報【所在地");

			String shiteiNo = getDataValue(dataRow, shisetsuNoIdx);
			String shisetsuName = getDataValue(dataRow, shisetsuNameIdx);
			String shisetsuJusho = getDataValue(dataRow, shisetsuJushoIdx);

			String atenaName = "";
			String atenaJusho = "";
			if (shiteiNo != null && !shiteiNo.isBlank()) {
				List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
				if (!tokugimuList.isEmpty()) {
					Tokugimu t = tokugimuList.get(0);
					if (t.getAtena() != null) {
						atenaName = t.getAtena().getName();
						atenaJusho = t.getAtena().getJusho();
					}
					if (shisetsuName == null || shisetsuName.isBlank()) {
						shisetsuName = t.getShisetsuName();
					}
					if (shisetsuJusho == null || shisetsuJusho.isBlank()) {
						shisetsuJusho = t.getShisetsuJusho();
					}
				}
			}

			List<DiffRow> diffRows = buildDiffRows(dataRow, yoshikiMap, shiteiNo);

			return new EltaxRenkeiKakuninDto(
					shiteiNo, shisetsuName, shisetsuJusho,
					atenaName, atenaJusho,
					file.getOriginalFilename(), shubetsu, shubetsuName,
					diffRows);

		} catch (Exception e) {
			throw new RuntimeException("ファイルの解析に失敗しました: " + e.getMessage(), e);
		}
	}

	@Override
	@Transactional
	public void commit(byte[] fileBytes, String fileName) {
		try {
			BigDecimal nextSeq = eltaxRenkeiRepository.findNextSeq(jichitaiCd);
			String tetsuzukiId = fileBytes.length > 0 ? extractTetsuzukiId(fileBytes) : "";
			String shubetsu = EltaxTetsuzukiConstants.TETSUZUKI_SHUBETSU_MAP.getOrDefault(tetsuzukiId, "");

			EltaxRenkei entity = new EltaxRenkei();
			entity.setJichitaiCd(jichitaiCd);
			entity.setSeq(nextSeq);
			entity.setFileName(fileName);
			entity.setShubetsu(shubetsu);
			entity.setShoriDt(LocalDateTime.now());
			entity.setShoriKekka("1");
			entity.setLog(fileBytes);

			eltaxRenkeiRepository.save(entity);
		} catch (Exception e) {
			throw new RuntimeException("ファイルの取込に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * アップロードCSVを解析し、最初のデータ行を返す。
	 * eLTAXのCSVは1行のデータ行で構成される。
	 */
	private String[] parseCsv(MultipartFile file) throws IOException {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			return line != null ? line.split(",", -1) : new String[0];
		}
	}

	private String extractTetsuzukiId(byte[] fileBytes) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new java.io.ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			if (line == null)
				return "";
			String[] cols = line.split(",", -1);
			return cols.length > 2 ? cols[2].trim() : "";
		} catch (IOException e) {
			return "";
		}
	}

	/**
	 * 手続IDに対応する様式定義CSVを読み込み、No.（1始まり）→CSV項目名称のマップを返す。
	 * 様式定義CSVはヘッダー行を含むため、No.列が数値の行のみ対象とする。
	 */
	private Map<Integer, String> loadYoshikiMap(String tetsuzukiId) throws IOException {
		String resourcePath = EltaxTetsuzukiConstants.TETSUZUKI_YOSHIKI_MAP.get(tetsuzukiId);
		if (resourcePath == null) {
			return Map.of();
		}
		Map<Integer, String> map = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(
						new ClassPathResource(resourcePath).getInputStream(),
						StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] cols = line.split(",", -1);
				if (cols.length < 2)
					continue;
				try {
					int no = Integer.parseInt(cols[0].trim());
					map.put(no, cols[1].trim());
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return map;
	}

	/** 様式マップからCSV項目名称の前方一致でインデックス（0始まり）を返す。見つからない場合は-1。 */
	private int findIndexByName(Map<Integer, String> yoshikiMap, String namePrefix) {
		return yoshikiMap.entrySet().stream()
				.filter(e -> e.getValue().startsWith(namePrefix))
				.mapToInt(e -> e.getKey() - 1)
				.findFirst()
				.orElse(-1);
	}

	/** データ行から指定インデックス（0始まり）の値を返す。範囲外の場合は空文字。 */
	private String getDataValue(String[] dataRow, int index) {
		if (index < 0 || index >= dataRow.length)
			return "";
		return dataRow[index].trim();
	}

	private List<DiffRow> buildDiffRows(String[] dataRow, Map<Integer, String> yoshikiMap, String shiteiNo) {
		List<DiffRow> diffRows = new ArrayList<>();

		List<Tokugimu> existing = new ArrayList<>();
		if (shiteiNo != null && !shiteiNo.isBlank()) {
			existing = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		}
		Tokugimu prev = existing.isEmpty() ? null : existing.get(0);

		for (Map.Entry<Integer, String> entry : yoshikiMap.entrySet()) {
			String itemName = entry.getValue();
			String afterValue = getDataValue(dataRow, entry.getKey() - 1);
			String beforeValue = resolveBeforeValue(prev, itemName);
			diffRows.add(new DiffRow(itemName, beforeValue, afterValue));
		}
		return diffRows;
	}

	private String resolveBeforeValue(Tokugimu prev, String itemName) {
		if (prev == null)
			return "";
		return switch (itemName) {
		case "施設情報【名称】" -> prev.getShisetsuName();
		case "施設情報【所在地】" -> prev.getShisetsuJusho();
		case "施設情報【電話番号】" -> prev.getShisetsuTel();
		case "施設情報【客室数】" -> prev.getKyakushitsuSu() != null ? prev.getKyakushitsuSu().toPlainString() : "";
		case "施設情報【宿泊定員】" -> prev.getShuyoSu() != null ? prev.getShuyoSu().toPlainString() : "";
		default -> "";
		};
	}
}
