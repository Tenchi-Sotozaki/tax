package jp.lg.asp.accommodation.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto.DiffRow;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.EltaxRenkeiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
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
			List<String[]> rows = parseCsv(file);

			String shiteiNo = extractValue(rows, "施設番号");
			String shisetsuName = extractValue(rows, "名称");
			String shisetsuJusho = extractValue(rows, "住所又は所在地");
			String shubetsu = detectShubetsu(rows);

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

			List<DiffRow> diffRows = buildDiffRows(rows, shiteiNo);

			return new EltaxRenkeiKakuninDto(
					shiteiNo, shisetsuName, shisetsuJusho,
					atenaName, atenaJusho,
					file.getOriginalFilename(), shubetsu,
					diffRows);

		} catch (Exception e) {
			throw new RuntimeException("ファイルの解析に失敗しました: " + e.getMessage(), e);
		}
	}

	@Override
	@Transactional
	public void commit(MultipartFile file) {
		try {
			BigDecimal nextSeq = eltaxRenkeiRepository.findNextSeq(jichitaiCd);
			List<String[]> rows = parseCsv(file);
			String shubetsu = detectShubetsu(rows);

			EltaxRenkei entity = new EltaxRenkei();
			entity.setJichitaiCd(jichitaiCd);
			entity.setSeq(nextSeq);
			entity.setFileName(file.getOriginalFilename());
			entity.setShubetsu(shubetsu);
			entity.setShoriDt(LocalDateTime.now());
			entity.setShoriKekka("1");
			entity.setLog(file.getBytes());

			eltaxRenkeiRepository.save(entity);
		} catch (Exception e) {
			throw new RuntimeException("ファイルの取込に失敗しました: " + e.getMessage(), e);
		}
	}

	private List<String[]> parseCsv(MultipartFile file) throws Exception {
		List<String[]> rows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				rows.add(line.split(",", -1));
			}
		}
		return rows;
	}

	private String extractValue(List<String[]> rows, String key) {
		return rows.stream()
				.filter(r -> r.length >= 2 && r[0].trim().equals(key))
				.map(r -> r[1].trim())
				.findFirst()
				.orElse("");
	}

	private String detectShubetsu(List<String[]> rows) {
		String shubetsu = extractValue(rows, "様式");
		if (shubetsu.contains("特別徴収義務者登録申請書"))
			return "01";
		if (shubetsu.contains("定額"))
			return "02";
		if (shubetsu.contains("定率"))
			return "03";
		return shubetsu;
	}

	private List<DiffRow> buildDiffRows(List<String[]> rows, String shiteiNo) {
		List<DiffRow> diffRows = new ArrayList<>();

		List<Tokugimu> existing = new ArrayList<>();
		if (shiteiNo != null && !shiteiNo.isBlank()) {
			existing = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		}
		Tokugimu prev = existing.isEmpty() ? null : existing.get(0);

		for (String[] row : rows) {
			if (row.length < 2)
				continue;
			String itemName = row[0].trim();
			String afterValue = row[1].trim();
			if (itemName.isBlank())
				continue;

			String beforeValue = resolveBeforeValue(prev, itemName);
			diffRows.add(new DiffRow(itemName, beforeValue, afterValue));
		}
		return diffRows;
	}

	private String resolveBeforeValue(Tokugimu prev, String itemName) {
		if (prev == null)
			return "";
		return switch (itemName) {
		case "名称" -> prev.getShisetsuName();
		case "住所又は所在地" -> prev.getShisetsuJusho();
		case "電話番号" -> prev.getShisetsuTel();
		case "客室数" -> prev.getKyakushitsuSu() != null ? prev.getKyakushitsuSu().toPlainString() : "";
		case "宿泊定員" -> prev.getShuyoSu() != null ? prev.getShuyoSu().toPlainString() : "";
		default -> "";
		};
	}
}
