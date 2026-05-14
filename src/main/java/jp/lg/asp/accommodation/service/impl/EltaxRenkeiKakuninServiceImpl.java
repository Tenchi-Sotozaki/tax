package jp.lg.asp.accommodation.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.constant.EltaxTetsuzukiConstants;
import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto.DiffRow;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.Shoyusha;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.EltaxRenkeiRepository;
import jp.lg.asp.accommodation.repository.NozeiShukiRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.EltaxRenkeiKakuninService;
import jp.lg.asp.accommodation.service.FukaCommonService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EltaxRenkeiKakuninServiceImpl implements EltaxRenkeiKakuninService {

	private final EltaxRenkeiRepository eltaxRenkeiRepository;
	private final TokugimuRepository tokugimuRepository;
	private final ShoyushaRepository shoyushaRepository;
	private final AtenaRepository atenaRepository;
	private final NozeiShukiRepository nozeiShukiRepository;
	private final FukaCommonService fukaCommonService;

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
			String tetsuzukiId = fileBytes.length > 0 ? extractTetsuzukiId(fileBytes) : "";
			String shubetsu = EltaxTetsuzukiConstants.TETSUZUKI_SHUBETSU_MAP.getOrDefault(tetsuzukiId, "");
			switch (shubetsu) {
			case "01":
				// 特別徴収義務者
				saveTokugimu(fileBytes);
				break;
			case "02":
				// 納入申告（定額）
				saveNonyuTeigaku(fileBytes);
				break;
			case "03":
				// 納入申告（定率）
				saveNonyuTeiritsu(fileBytes);
				break;
			case "04":
				// 特例納入申告（定額）
				saveNonyuTokureiTeigaku(fileBytes);
				break;
			case "05":
				// 特例納入申告（定率）
				saveNonyuTokureiTeiritsu(fileBytes);
				break;
			default:
				break;
			}
			saveEltaxRenkei(fileBytes, fileName);

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

	/**
	 * 特別徴収義務者登録申請書（種別01）のDB更新
	 */
	private void saveTokugimu(byte[] fileBytes) {
		try {
			String tetsuzukiId = extractTetsuzukiId(fileBytes);
			Map<Integer, String> yoshikiMap = loadYoshikiMap(tetsuzukiId);
			String[] dataRow = parseBytesAsCsv(fileBytes);

			// 様式CSVのインデックス取得（1始まり→0始まり変換済み）
			int shinseikubunIdx = findIndexByName(yoshikiMap, "特別徴収義務者【申請区分");
			int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");
			int shisetsuNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int shisetsuNameIdx = findIndexByName(yoshikiMap, "施設情報【名称");
			int shisetsuJushoIdx = findIndexByName(yoshikiMap, "施設情報【所在地");
			int shisetsuTelIdx = findIndexByName(yoshikiMap, "施設情報【電話番号");
			int yukaMensekiIdx = findIndexByName(yoshikiMap, "施設情報【床面積");
			int chijoKaiIdx = findIndexByName(yoshikiMap, "施設情報【階数（地上");
			int chikaKaiIdx = findIndexByName(yoshikiMap, "施設情報【階数（地下");
			int kyakushitsuSuIdx = findIndexByName(yoshikiMap, "施設情報【客室数");
			int shuyoSuIdx = findIndexByName(yoshikiMap, "施設情報【宿泊定員");
			int eigyoStYmdIdx = findIndexByName(yoshikiMap, "施設情報【経営開始年月日");
			int kyokaNameIdx = findIndexByName(yoshikiMap, "宿泊施設の営業許可等情報【氏名");
			int kyokaYubinNoIdx = findIndexByName(yoshikiMap, "宿泊施設の営業許可等情報【郵便番号");
			int kyokaJushoIdx = findIndexByName(yoshikiMap, "宿泊施設の営業許可等情報【住所又は所在地");
			int kyokaNoIdx = findIndexByName(yoshikiMap, "宿泊施設の営業許可等情報【許可番号");
			int kyokaShuIdx = findIndexByName(yoshikiMap, "宿泊施設の営業許可等情報【営業種別");
			int soufusakiNameIdx = findIndexByName(yoshikiMap, "送付先情報【氏名");
			int soufusakiYubinNoIdx = findIndexByName(yoshikiMap, "送付先情報【郵便番号");
			int soufusakiJushoIdx = findIndexByName(yoshikiMap, "送付先情報【住所又は所在地");
			int soufusakiTelIdx = findIndexByName(yoshikiMap, "送付先情報【電話番号");
			int bikoIdx = findIndexByName(yoshikiMap, "備考");
			int kyushiStYmdIdx = findIndexByName(yoshikiMap, "休止廃止再開情報【休止期間（自");
			int kyushiEdYmdIdx = findIndexByName(yoshikiMap, "休止廃止再開情報【休止期間（至");
			int eigyoEdYmdIdx = findIndexByName(yoshikiMap, "休止廃止再開情報【廃止年月日");
			int saikaiYmdIdx = findIndexByName(yoshikiMap, "休止廃止再開情報【再開年月日");
			int tokugimuNameIdx = findIndexByName(yoshikiMap, "特別徴収義務者【氏名又は名称");
			int shoyushaNameIdx = findIndexByName(yoshikiMap, "施設の所有者情報【氏名");
			int shoyushaYubinNoIdx = findIndexByName(yoshikiMap, "施設の所有者情報【郵便番号");
			int shoyushaJushoIdx = findIndexByName(yoshikiMap, "施設の所有者情報【住所又は所在地");
			int shoyushaTelIdx = findIndexByName(yoshikiMap, "施設の所有者情報【電話番号");

			String shinseikubun = getDataValue(dataRow, shinseikubunIdx);
			String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);
			boolean isNew = shinseikubun.startsWith("1");

			// 指定番号の決定
			String shiteiNo;
			if (isNew) {
				int maxNo = tokugimuRepository.findMaxShiteiNoByJichitaiCd(jichitaiCd).orElse(0);
				shiteiNo = String.valueOf(maxNo + 1);
			} else {
				shiteiNo = getDataValue(dataRow, shisetsuNoIdx);
			}

			// 前履歴取得
			List<Tokugimu> prevList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
			Tokugimu prev = prevList.isEmpty() ? null : prevList.get(0);

			// 履歴番号
			int maxRno = tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).orElse(0);
			BigDecimal newRno = BigDecimal.valueOf(maxRno + 1);

			// 登録年月日
			LocalDate torokuYmd = isNew ? LocalDate.now() : (prev != null ? prev.getTorokuYmd() : LocalDate.now());
			LocalDate shinkokuYmd = parseDate(teishutsuYmd);
			if (shinkokuYmd == null)
				shinkokuYmd = LocalDate.now();

			// 宛名番号
			BigDecimal atenaNo;
			if (isNew) {
				String tokugimuName = getDataValue(dataRow, tokugimuNameIdx);
				List<Atena> atenaList = atenaRepository.searchByAnyField(
						jichitaiCd, null, tokugimuName, null, null, null, null);
				atenaNo = atenaList.isEmpty() ? BigDecimal.ZERO : atenaList.get(0).getAtenaNo();
			} else {
				atenaNo = prev != null ? prev.getAtenaNo() : BigDecimal.ZERO;
			}

			// 納税周期
			BigDecimal nokigen;
			if (isNew) {
				var nozeiList = nozeiShukiRepository.findActiveByJichitaiCd(jichitaiCd);
				nokigen = nozeiList.isEmpty() ? null : nozeiList.get(0).getShuki();
			} else {
				nokigen = prev != null ? prev.getNokigen() : null;
			}

			// 営業開始年月日：再開年月日があればそちら、なければ経営開始年月日
			String saikaiYmdStr = getDataValue(dataRow, saikaiYmdIdx);
			LocalDate eigyoStYmd = (!saikaiYmdStr.isBlank())
					? parseDate(saikaiYmdStr)
					: parseDate(getDataValue(dataRow, eigyoStYmdIdx));
			if (eigyoStYmd == null)
				eigyoStYmd = LocalDate.now();

			// 営業種別変換
			String kyokaShu = convertKyokaShu(getDataValue(dataRow, kyokaShuIdx));

			Tokugimu entity = new Tokugimu();
			entity.setJichitaiCd(jichitaiCd);
			entity.setShiteiNo(shiteiNo);
			entity.setRno(newRno);
			entity.setTorokuYmd(torokuYmd);
			entity.setShinkokuYmd(shinkokuYmd);
			entity.setHenkoYmd(shinkokuYmd);
			entity.setAtenaNo(atenaNo);
			entity.setShisetsuName(getDataValue(dataRow, shisetsuNameIdx));
			entity.setShisetsuNameKana(prev != null ? prev.getShisetsuNameKana() : "");
			entity.setShisetsuYubinNo(prev != null ? prev.getShisetsuYubinNo() : null);
			entity.setShisetsuJusho(getDataValue(dataRow, shisetsuJushoIdx));
			entity.setShisetsuTel(getDataValue(dataRow, shisetsuTelIdx));
			entity.setYukaMenseki(parseBigDecimal(getDataValue(dataRow, yukaMensekiIdx)));
			entity.setChijoKai(parseBigDecimal(getDataValue(dataRow, chijoKaiIdx)));
			entity.setChikaKai(parseBigDecimal(getDataValue(dataRow, chikaKaiIdx)));
			entity.setKyakushitsuSu(parseBigDecimal(getDataValue(dataRow, kyakushitsuSuIdx)));
			entity.setShuyoSu(parseBigDecimal(getDataValue(dataRow, shuyoSuIdx)));
			entity.setKyokaName(getDataValue(dataRow, kyokaNameIdx));
			entity.setKyokaNameKana(prev != null ? prev.getKyokaNameKana() : "");
			entity.setKyokaYubinNo(getDataValue(dataRow, kyokaYubinNoIdx));
			entity.setKyokaJusho(getDataValue(dataRow, kyokaJushoIdx));
			entity.setKyokaTel(prev != null ? prev.getKyokaTel() : null);
			entity.setKyokaShu(kyokaShu);
			entity.setKyokaNo(getDataValue(dataRow, kyokaNoIdx));
			entity.setSoufusakiName(getDataValue(dataRow, soufusakiNameIdx));
			entity.setSoufusakiNameKana(prev != null ? prev.getSoufusakiNameKana() : "");
			entity.setSoufusakiYubinNo(getDataValue(dataRow, soufusakiYubinNoIdx));
			entity.setSoufusakiJusho(getDataValue(dataRow, soufusakiJushoIdx));
			entity.setSoufusakiTel(getDataValue(dataRow, soufusakiTelIdx));
			entity.setBiko(getDataValue(dataRow, bikoIdx));
			entity.setEigyoStYmd(eigyoStYmd);
			entity.setEigyoEdYmd(parseDate(getDataValue(dataRow, eigyoEdYmdIdx)));
			entity.setKyushiStYmd(parseDate(getDataValue(dataRow, kyushiStYmdIdx)));
			entity.setKyushiEdYmd(parseDate(getDataValue(dataRow, kyushiEdYmdIdx)));
			entity.setKyuhaishiRiyu(prev != null ? prev.getKyuhaishiRiyu() : null);
			entity.setEltaxUmu("1");
			entity.setNokigen(nokigen);
			entity.setNewFlg("1");
			entity.setDelFlg("0");
			tokugimuRepository.save(entity);

			// t_shoyusha
			String shoyushaName = getDataValue(dataRow, shoyushaNameIdx);
			if (!shoyushaName.isBlank()) {
				String prevShoyushaNameKana = shoyushaRepository
						.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).stream()
						.filter(s -> prev != null && s.getRno().compareTo(prev.getRno()) == 0
								&& s.getIdx().compareTo(BigDecimal.ONE) == 0)
						.map(Shoyusha::getShoyushaNameKana)
						.findFirst()
						.orElse("");
				Shoyusha shoyusha = new Shoyusha();
				shoyusha.setJichitaiCd(jichitaiCd);
				shoyusha.setShiteiNo(shiteiNo);
				shoyusha.setRno(newRno);
				shoyusha.setIdx(BigDecimal.ONE);
				shoyusha.setShoyushaName(shoyushaName);
				shoyusha.setShoyushaNameKana(prevShoyushaNameKana);
				shoyusha.setShoyushaYubinNo(getDataValue(dataRow, shoyushaYubinNoIdx));
				shoyusha.setShoyushaJusho(getDataValue(dataRow, shoyushaJushoIdx));
				shoyusha.setShoyushaTel(getDataValue(dataRow, shoyushaTelIdx));
				shoyushaRepository.save(shoyusha);
			}
		} catch (Exception e) {
			throw new RuntimeException("特別徴収義務者情報の更新に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * 納入申告（定額）（種別02）のDB更新
	 */
	private void saveNonyuTeigaku(byte[] fileBytes) {
		try {
			String tetsuzukiId = extractTetsuzukiId(fileBytes);
			Map<Integer, String> yoshikiMap = loadYoshikiMap(tetsuzukiId);
			String[] dataRow = parseBytesAsCsv(fileBytes);

			int shiteiNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月");
			int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");

			String shiteiNo = getDataValue(dataRow, shiteiNoIdx);
			String taishoYm = getDataValue(dataRow, taishoYmIdx);
			String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);

			fukaCommonService.saveFuka(shiteiNo, taishoYm, teishutsuYmd, FukaConstants.TEIGAKU, dataRow, yoshikiMap,
					"納入税額－行為年月－");
		} catch (Exception e) {
			throw new RuntimeException("賦課情報の更新に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * 納入申告（定率）（種別03）のDB更新
	 */
	private void saveNonyuTeiritsu(byte[] fileBytes) {
		try {
			String tetsuzukiId = extractTetsuzukiId(fileBytes);
			Map<Integer, String> yoshikiMap = loadYoshikiMap(tetsuzukiId);
			String[] dataRow = parseBytesAsCsv(fileBytes);

			int shiteiNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月");
			int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");

			String shiteiNo = getDataValue(dataRow, shiteiNoIdx);
			String taishoYm = getDataValue(dataRow, taishoYmIdx);
			String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);

			fukaCommonService.saveFuka(shiteiNo, taishoYm, teishutsuYmd, FukaConstants.TEIRITSU, dataRow, yoshikiMap,
					"納入税額－行為年月－");
		} catch (Exception e) {
			throw new RuntimeException("賦課情報の更新に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * 特例納入申告（定額）（種別04）のDB更新
	 */
	private void saveNonyuTokureiTeigaku(byte[] fileBytes) {
		try {
			String tetsuzukiId = extractTetsuzukiId(fileBytes);
			Map<Integer, String> yoshikiMap = loadYoshikiMap(tetsuzukiId);
			String[] dataRow = parseBytesAsCsv(fileBytes);

			int shiteiNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");
			String shiteiNo = getDataValue(dataRow, shiteiNoIdx);
			String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);

			for (String suffix : List.of("１", "２", "３")) {
				int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月" + suffix);
				String taishoYm = getDataValue(dataRow, taishoYmIdx);
				if (taishoYm.isBlank())
					continue;
				fukaCommonService.saveFuka(shiteiNo, taishoYm, teishutsuYmd,
						FukaConstants.TEIGAKU, dataRow, yoshikiMap,
						"納入税額－行為年月" + suffix + "－");
			}
		} catch (Exception e) {
			throw new RuntimeException("賦課情報の更新に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * 特例納入申告（定率）（種別05）のDB更新
	 */
	private void saveNonyuTokureiTeiritsu(byte[] fileBytes) {
		try {
			String tetsuzukiId = extractTetsuzukiId(fileBytes);
			Map<Integer, String> yoshikiMap = loadYoshikiMap(tetsuzukiId);
			String[] dataRow = parseBytesAsCsv(fileBytes);

			int shiteiNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");
			String shiteiNo = getDataValue(dataRow, shiteiNoIdx);
			String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);

			for (String suffix : List.of("１", "２", "３")) {
				int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月" + suffix);
				String taishoYm = getDataValue(dataRow, taishoYmIdx);
				if (taishoYm.isBlank())
					continue;
				fukaCommonService.saveFuka(shiteiNo, taishoYm, teishutsuYmd,
						FukaConstants.TEIRITSU, dataRow, yoshikiMap,
						"納入税額－行為年月" + suffix + "－");
			}
		} catch (Exception e) {
			throw new RuntimeException("賦課情報の更新に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * eLTAX連携管理を更新する
	 */
	private void saveEltaxRenkei(byte[] fileBytes, String fileName) {
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
			throw new RuntimeException("eLTAX連携管理の更新に失敗しました: " + e.getMessage(), e);
		}
	}

	private String[] parseBytesAsCsv(byte[] fileBytes) throws IOException {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new java.io.ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			return line != null ? line.split(",", -1) : new String[0];
		}
	}

	private LocalDate parseDate(String value) {
		if (value == null || value.isBlank())
			return null;
		for (DateTimeFormatter fmt : List.of(
				DateTimeFormatter.ofPattern("yyyy/MM/dd"),
				DateTimeFormatter.ofPattern("yyyyMMdd"),
				DateTimeFormatter.ISO_LOCAL_DATE)) {
			try {
				return LocalDate.parse(value, fmt);
			} catch (DateTimeParseException ignored) {
			}
		}
		return null;
	}

	private BigDecimal parseBigDecimal(String value) {
		if (value == null || value.isBlank())
			return null;
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 営業種別変換：eLTAX値 → システム値
	 * 
	 * @param raw eLTAX値
	 * @return システム値 
	 */
	private String convertKyokaShu(String raw) {
		return switch (raw) {
		case "1" -> "1"; // 旅館・ホテル営業 → ホテル
		case "2" -> "3"; // 簡易宿所営業 → 簡易宿所
		case "3", "4" -> "4"; // 下宿営業・その他 → 民泊
		default -> raw;
		};
	}

}
