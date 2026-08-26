package jp.lg.asp.accommodation.service.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
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
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.EltaxConstants;
import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.constant.ZeiritsuConstants;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto.DiffRow;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.KyodoJigyosha;
import jp.lg.asp.accommodation.entity.Shoyusha;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.exception.EltaxRenkeiKakuninValidationException;
import jp.lg.asp.accommodation.repository.EltaxRenkeiRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.KyodoJigyoshaRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.EltaxRenkeiKakuninService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EltaxRenkeiKakuninServiceImpl implements EltaxRenkeiKakuninService {

	private final EltaxRenkeiRepository eltaxRenkeiRepository;
	private final TokugimuRepository tokugimuRepository;
	private final ShoyushaRepository shoyushaRepository;
	private final KyodoJigyoshaRepository kyodoJigyoshaRepository;
	private final GassanRepository gassanRepository;
	private final FukaRepository fukaRepository;
	private final FukaUchiRepository fukaUchiRepository;
	private final ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
	private final ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
	private final JichitaiRepository jichitaiRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public EltaxRenkeiKakuninDto preview(MultipartFile file) {
		try {
			return buildPreviewDto(file.getBytes(), file.getOriginalFilename(), null, true);
		} catch (IOException e) {
			throw new UncheckedIOException("CSVファイルの解析に失敗しました。", e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public EltaxRenkeiKakuninDto repreview(byte[] fileBytes, String overrideShiteiNo) {
		return buildPreviewDto(fileBytes, null, overrideShiteiNo, false);
	}

	private EltaxRenkeiKakuninDto buildPreviewDto(byte[] fileBytes, String fileName, String overrideShiteiNo,
			boolean isFromPreview) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		String[] dataRow;
		try {
			dataRow = parseBytesAsCsv(fileBytes);
		} catch (IOException e) {
			throw new UncheckedIOException("CSVファイルの解析に失敗しました。", e);
		}

		if (dataRow.length == 0 || (dataRow.length == 1 && dataRow[0].isBlank())) {
			throw new RuntimeException("ファイルの解析に失敗しました：ファイルが空です。");
		}

		String tetsuzukiId = dataRow.length > 2 ? dataRow[2].trim() : "";
		String shubetsu = EltaxConstants.TETSUZUKI_SHUBETSU_MAP.getOrDefault(tetsuzukiId, "");
		String shubetsuName = EltaxConstants.SHUBETSU_NAME_MAP.getOrDefault(shubetsu, shubetsu);

		Map<Integer, YoshikiItem> yoshikiMap;
		try {
			yoshikiMap = loadYoshikiMap(tetsuzukiId);
		} catch (IOException e) {
			throw new UncheckedIOException("様式マップの読み込みに失敗しました。", e);
		}

		int shisetsuNoIdx = -1;
		int shinseikubunIdx = -1;
		String shinseikubun = "";
		boolean isTokugimuNew = false;
		boolean isTokugimu = false;

		switch (shubetsu) {
		case EltaxConstants.SHUBETSU_TOKUGIMU:
			// 特別徴収義務者
			isTokugimu = true;
			shisetsuNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			shinseikubunIdx = findIndexByName(yoshikiMap, "特別徴収義務者【申請区分");
			shinseikubun = getDataValue(dataRow, shinseikubunIdx);
			isTokugimuNew = shinseikubun.startsWith(EltaxConstants.SHINSEI_KBN_SHINKI);
			if (!shinseikubun.startsWith(EltaxConstants.SHINSEI_KBN_SHINKI) &&
					!shinseikubun.startsWith(EltaxConstants.SHINSEI_KBN_HENKO) &&
					!shinseikubun.startsWith(EltaxConstants.SHINSEI_KBN_KYUSHI) &&
					!shinseikubun.startsWith(EltaxConstants.SHINSEI_KBN_SAIKAI) &&
					!shinseikubun.startsWith(EltaxConstants.SHINSEI_KBN_HAISHI)) {
				throw new RuntimeException("システム対応外の申請区分です。");
			}
			break;
		case EltaxConstants.SHUBETSU_TEIGAKU:
		case EltaxConstants.SHUBETSU_TEIRITSU:
		case EltaxConstants.SHUBETSU_TOKU_TEIGAKU:
		case EltaxConstants.SHUBETSU_TOKU_TEIRITSU:
			// 納入申告
			shisetsuNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号（宿泊施設番号、指定番号）】");
			break;
		default:
			throw new RuntimeException("システム対応外の手続き種別です: " + shubetsu);
		}
		String shiteiNo = overrideShiteiNo != null ? overrideShiteiNo : getDataValue(dataRow, shisetsuNoIdx);

		String atenaName = "";
		String atenaJusho = "";
		String shisetsuName = "";
		String shisetsuJusho = "";
		if (shiteiNo != null && !shiteiNo.isBlank()) {
			List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
			if (!tokugimuList.isEmpty()) {
				Tokugimu t = tokugimuList.get(0);
				if (t.getAtena() != null) {
					atenaName = t.getAtena().getName();
					atenaJusho = t.getAtena().getJusho();
				}
				shisetsuName = t.getShisetsuName();
				shisetsuJusho = t.getShisetsuJusho();
			} else {
				if (!isTokugimu) {
					List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, shiteiNo);
					if (!gassanList.isEmpty()) {
						String daihyoShiteiNo = gassanList.get(0).getShiteiNo();
						List<Tokugimu> daihyoTokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd,
								daihyoShiteiNo);
						if (!daihyoTokugimuList.isEmpty()) {
							Tokugimu t = daihyoTokugimuList.get(0);
							if (t.getAtena() != null) {
								atenaName = t.getAtena().getName();
								atenaJusho = t.getAtena().getJusho();
							}
							shisetsuName = t.getShisetsuName();
							shisetsuJusho = t.getShisetsuJusho();
						}
					}
				}
				if (shisetsuName.isEmpty()) {
					if (!isFromPreview)
						throw new RuntimeException("指定番号（" + shiteiNo + "）に該当する特別徴収義務者が登録されていません。");
					return buildDtoWithEmptyBefore(fileName, shiteiNo, shubetsu, shubetsuName, yoshikiMap, dataRow,
							isTokugimuNew,
							"指定番号（" + shiteiNo + "）に該当する特別徴収義務者が登録されていません。");
				}
			}
		} else {
			if (!isTokugimuNew) {
				if (!isFromPreview)
					throw new RuntimeException("施設番号が未設定です。");
				return buildDtoWithEmptyBefore(fileName, shiteiNo, shubetsu, shubetsuName, yoshikiMap, dataRow,
						isTokugimuNew,
						"施設番号が未設定です。");
			}
		}

		List<DiffRow> diffRows = null;
		boolean atenaSearchRequired = false;
		String tokugimuName = "";
		String tokugimuJusho = "";
		String tokugimuTel = "";
		String kojinNo = "";
		String hojinNo = "";
		switch (shubetsu) {
		case EltaxConstants.SHUBETSU_TOKUGIMU:
			// 特別徴収義務者
			diffRows = buildDiffRowsTokugimu(dataRow, yoshikiMap, shiteiNo);
			// 新規登録の場合は宛名検索が必要
			if (isTokugimuNew) {
				atenaSearchRequired = true;
				int tokugimuNameIdx = findIndexByName(yoshikiMap, "特別徴収義務者【氏名又は名称】");
				tokugimuName = getDataValue(dataRow, tokugimuNameIdx);
				int tokugimuJushoIdx = findIndexByName(yoshikiMap, "特別徴収義務者【住所又は所在地】");
				tokugimuJusho = getDataValue(dataRow, tokugimuJushoIdx);
				int tokugimuTelIdx = findIndexByName(yoshikiMap, "特別徴収義務者【電話番号】");
				tokugimuTel = getDataValue(dataRow, tokugimuTelIdx);
				int kojinNoIdx = findIndexByName(yoshikiMap, "特別徴収義務者【個人番号】");
				kojinNo = getDataValue(dataRow, kojinNoIdx);
				int hojinNoIdx = findIndexByName(yoshikiMap, "特別徴収義務者【法人番号】");
				hojinNo = getDataValue(dataRow, hojinNoIdx);
			}
			break;
		case EltaxConstants.SHUBETSU_TEIGAKU:
		case EltaxConstants.SHUBETSU_TEIRITSU:
		case EltaxConstants.SHUBETSU_TOKU_TEIGAKU:
		case EltaxConstants.SHUBETSU_TOKU_TEIRITSU:
			// 納入申告
			diffRows = buildDiffRowsFuka(dataRow, yoshikiMap, shiteiNo, shubetsu);
			break;
		default:
			throw new RuntimeException("システム対応外の手続き種別です: " + shubetsu);
		}

		EltaxRenkeiKakuninDto dto = new EltaxRenkeiKakuninDto(
				shiteiNo, shisetsuName, shisetsuJusho,
				atenaName, atenaJusho,
				fileName, shubetsu, shubetsuName,
				atenaSearchRequired, tokugimuName, tokugimuJusho, tokugimuTel, kojinNo, hojinNo,
				null, diffRows);

		// 必須項目チェック（全件チェック）
		List<String> errorMessages = new ArrayList<>();
		for (Map.Entry<Integer, YoshikiItem> entry : yoshikiMap.entrySet()) {
			if ("1".equals(entry.getValue().requiredFlg()) && getDataValue(dataRow, entry.getKey() - 1).isBlank()) {
				errorMessages.add("必須項目「" + entry.getValue().itemName() + "」が入力されていません。");
			}
		}
		if (!errorMessages.isEmpty()) {
			throw new EltaxRenkeiKakuninValidationException(errorMessages, dto);
		}

		return dto;
	}

	private EltaxRenkeiKakuninDto buildDtoWithEmptyBefore(String fileName, String shiteiNo,
			String shubetsu, String shubetsuName, Map<Integer, YoshikiItem> yoshikiMap, String[] dataRow,
			boolean isTokugimuNew, String errorMessage) {
		List<DiffRow> diffRows;
		boolean atenaSearchRequired = false;
		String tokugimuName = "", tokugimuJusho = "", tokugimuTel = "", kojinNo = "", hojinNo = "";
		switch (shubetsu) {
		case EltaxConstants.SHUBETSU_TOKUGIMU:
			diffRows = buildDiffRowsTokugimu(dataRow, yoshikiMap, null);
			if (isTokugimuNew) {
				atenaSearchRequired = true;
				tokugimuName = getDataValue(dataRow, findIndexByName(yoshikiMap, "特別徴収義務者【氏名又は名称】"));
				tokugimuJusho = getDataValue(dataRow, findIndexByName(yoshikiMap, "特別徴収義務者【住所又は所在地】"));
				tokugimuTel = getDataValue(dataRow, findIndexByName(yoshikiMap, "特別徴収義務者【電話番号】"));
				kojinNo = getDataValue(dataRow, findIndexByName(yoshikiMap, "特別徴収義務者【個人番号】"));
				hojinNo = getDataValue(dataRow, findIndexByName(yoshikiMap, "特別徴収義務者【法人番号】"));
			}
			break;
		default:
			diffRows = buildDiffRowsFuka(dataRow, yoshikiMap, null, shubetsu);
			break;
		}
		diffRows = diffRows.stream()
				.map(r -> new DiffRow(r.getItemName(), "－", r.getAfterValue(), r.getDispFlg(), r.getRequiredFlg()))
				.collect(java.util.stream.Collectors.toList());
		EltaxRenkeiKakuninDto dto = new EltaxRenkeiKakuninDto(
				shiteiNo, "", "", "", "",
				fileName, shubetsu, shubetsuName,
				atenaSearchRequired, tokugimuName, tokugimuJusho, tokugimuTel, kojinNo, hojinNo,
				null, diffRows);
		dto.setErrorMessage(errorMessage);
		return dto;
	}

	@Override
	@Transactional
	public void commit(byte[] fileBytes, String fileName, BigDecimal atenaNoFromSession, String shiteiNo) {
		try {
			String tetsuzukiId = fileBytes.length > 0 ? extractTetsuzukiId(fileBytes) : "";
			String shubetsu = EltaxConstants.TETSUZUKI_SHUBETSU_MAP.getOrDefault(tetsuzukiId, "");
			switch (shubetsu) {
			case EltaxConstants.SHUBETSU_TOKUGIMU:
				// 特別徴収義務者
				saveTokugimu(fileBytes, atenaNoFromSession, shiteiNo);
				break;
			case EltaxConstants.SHUBETSU_TEIGAKU:
				// 納入申告（定額）
				saveNonyuTeigaku(fileBytes, shiteiNo);
				break;
			case EltaxConstants.SHUBETSU_TEIRITSU:
				// 納入申告（定率）
				saveNonyuTeiritsu(fileBytes, shiteiNo);
				break;
			case EltaxConstants.SHUBETSU_TOKU_TEIGAKU:
				// 特例納入申告（定額）
				saveNonyuTokureiTeigaku(fileBytes, shiteiNo);
				break;
			case EltaxConstants.SHUBETSU_TOKU_TEIRITSU:
				// 特例納入申告（定率）
				saveNonyuTokureiTeiritsu(fileBytes, shiteiNo);
				break;
			default:
				break;
			}
			saveEltaxRenkei(fileBytes, fileName);

		} catch (Exception e) {
			throw new RuntimeException("ファイルの取込に失敗しました: " + e.getMessage(), e);
		}
	}

	private String extractTetsuzukiId(byte[] fileBytes) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			if (line == null)
				return "";
			String[] cols = line.split(",", -1);
			return cols.length > 2 ? cols[2].trim() : "";
		} catch (IOException e) {
			return "";
		}
	}

	record YoshikiItem(String itemName, String dispFlg, String requiredFlg) {
	}

	/**
	 * 手続IDに対応する様式定義CSVを読み込み、No.（1始まり）→YoshikiItemのマップを返す。
	 * 様式定義CSVはヘッダー行を含むため、No.列が数値の行のみ対象とする。
	 * CSVフォーマット: no, itemName, dispFlg, requiredFlg
	 */
	private Map<Integer, YoshikiItem> loadYoshikiMap(String tetsuzukiId) throws IOException {
		String resourcePath = EltaxConstants.TETSUZUKI_YOSHIKI_MAP.get(tetsuzukiId);
		if (resourcePath == null) {
			return Map.of();
		}
		Map<Integer, YoshikiItem> map = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(
						new ClassPathResource(resourcePath).getInputStream(),
						StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] cols = line.split(",", -1);
				if (cols.length < 4)
					continue;
				try {
					int no = Integer.parseInt(cols[0].trim());
					map.put(no, new YoshikiItem(cols[1].trim(), cols[2].trim(), cols[3].trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return map;
	}

	/** 様式マップからCSV項目名称の前方一致でインデックス（0始まり）を返す。見つからない場合は-1。 */
	private int findIndexByName(Map<Integer, YoshikiItem> yoshikiMap, String namePrefix) {
		return yoshikiMap.entrySet().stream()
				.filter(e -> e.getValue().itemName().startsWith(namePrefix))
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

	private List<DiffRow> buildDiffRowsTokugimu(String[] dataRow, Map<Integer, YoshikiItem> yoshikiMap,
			String shiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		List<DiffRow> diffRows = new ArrayList<>();

		List<Tokugimu> existingTokugimu = new ArrayList<>();
		if (shiteiNo != null && !shiteiNo.isBlank()) {
			existingTokugimu = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		}
		Tokugimu prevTokugimu = existingTokugimu.isEmpty() ? null : existingTokugimu.get(0);

		Shoyusha prevSyoyusha = shoyushaRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).stream()
				.filter(s -> prevTokugimu != null && s.getRno().compareTo(prevTokugimu.getRno()) == 0
						&& s.getIdx().compareTo(BigDecimal.ONE) == 0)
				.findFirst().orElse(null);

		KyodoJigyosha prevKyodoJigyosha = kyodoJigyoshaRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream()
				.filter(s -> prevTokugimu != null && s.getRno().compareTo(prevTokugimu.getRno()) == 0
						&& s.getIdx().compareTo(BigDecimal.ONE) == 0)
				.findFirst().orElse(null);

		for (Map.Entry<Integer, YoshikiItem> entry : yoshikiMap.entrySet()) {
			if (entry.getValue().dispFlg().equals("0"))
				continue;
			String itemName = entry.getValue().itemName();
			String afterValue = getDataValue(dataRow, entry.getKey() - 1);
			String beforeValue = resolveBeforeValueTokugimu(prevTokugimu, prevSyoyusha, prevKyodoJigyosha, itemName);
			if (itemName.equals("特別徴収義務者【申請区分】")) {
				afterValue = EltaxConstants.SHINSEI_KBN_NAME_MAP.getOrDefault(afterValue, afterValue);
			}
			diffRows.add(new DiffRow(itemName, beforeValue, afterValue, entry.getValue().dispFlg(),
					entry.getValue().requiredFlg()));
		}
		return diffRows;
	}

	private String resolveBeforeValueTokugimu(Tokugimu prevTokugimu, Shoyusha prevShoyusha,
			KyodoJigyosha prevKyodoJigyosha, String itemName) {
		if (prevTokugimu == null)
			return "－";
		return switch (itemName) {
		case "提出年月日" -> parseString(prevTokugimu.getShinkokuYmd());
		case "施設情報【名称】" -> parseString(prevTokugimu.getShisetsuName());
		case "施設情報【所在地】" -> parseString(prevTokugimu.getShisetsuJusho());
		case "施設情報【電話番号】" -> parseString(prevTokugimu.getShisetsuTel());
		case "施設情報【床面積】" -> parseString(prevTokugimu.getYukaMenseki());
		case "施設情報【階数（地上）】" -> parseString(prevTokugimu.getChijoKai());
		case "施設情報【階数（地下）】" -> parseString(prevTokugimu.getChikaKai());
		case "施設情報【客室数】" -> parseString(prevTokugimu.getKyakushitsuSu());
		case "施設情報【宿泊定員】" -> parseString(prevTokugimu.getShuyoSu());
		case "施設情報【経営開始年月日】" -> parseString(prevTokugimu.getEigyoStYmd());
		case "宿泊施設の営業許可等情報【氏名（名称及び代表者名）】" -> parseString(prevTokugimu.getKyokaName());
		case "宿泊施設の営業許可等情報【郵便番号】" -> parseString(prevTokugimu.getKyokaYubinNo());
		case "宿泊施設の営業許可等情報【住所又は所在地】" -> parseString(prevTokugimu.getKyokaJusho());
		case "宿泊施設の営業許可等情報【許可番号（届出番号）】" -> parseString(prevTokugimu.getKyokaNo());
		case "宿泊施設の営業許可等情報【営業種別】" -> parseString(prevTokugimu.getKyokaShu());
		case "送付先情報【氏名（名称及び代表者名）】" -> parseString(prevTokugimu.getSoufusakiName());
		case "送付先情報【郵便番号】" -> parseString(prevTokugimu.getSoufusakiYubinNo());
		case "送付先情報【住所又は所在地】" -> parseString(prevTokugimu.getSoufusakiJusho());
		case "送付先情報【電話番号】" -> parseString(prevTokugimu.getSoufusakiTel());
		case "備考" -> parseString(prevTokugimu.getBiko());
		case "届出理由（変更・休止・廃止・再開）" -> parseString(prevTokugimu.getKyuhaishiRiyu());
		case "休止廃止再開情報【休止期間（自）】" -> parseString(prevTokugimu.getKyushiStYmd());
		case "休止廃止再開情報【休止期間（至）】" -> parseString(prevTokugimu.getKyushiEdYmd());
		case "休止廃止再開情報【廃止年月日】" -> parseString(prevTokugimu.getEigyoEdYmd());
		case "施設の所有者情報【氏名（名称及び代表者名）】" -> prevShoyusha == null ? "－" : parseString(prevShoyusha.getShoyushaName());
		case "施設の所有者情報【郵便番号】" -> prevShoyusha == null ? "－" : parseString(prevShoyusha.getShoyushaYubinNo());
		case "施設の所有者情報【住所又は所在地】" -> prevShoyusha == null ? "－" : parseString(prevShoyusha.getShoyushaJusho());
		case "施設の所有者情報【電話番号】" -> prevShoyusha == null ? "－" : parseString(prevShoyusha.getShoyushaTel());
		case "共同事業者情報【氏名（名称及び代表者名）】" -> prevKyodoJigyosha == null ? "－"
				: parseString(prevKyodoJigyosha.getKyodoJigyoshaName());
		case "共同事業者情報【郵便番号】" -> prevKyodoJigyosha == null ? "－"
				: parseString(prevKyodoJigyosha.getKyodoJigyoshaYubinNo());
		case "共同事業者情報【住所又は所在地】" -> prevKyodoJigyosha == null ? "－"
				: parseString(prevKyodoJigyosha.getKyodoJigyoshaJusho());
		case "共同事業者情報【電話番号】" -> prevKyodoJigyosha == null ? "－" : parseString(prevKyodoJigyosha.getKyodoJigyoshaTel());
		default -> "－";
		};
	}

	private List<DiffRow> buildDiffRowsFuka(String[] dataRow, Map<Integer, YoshikiItem> yoshikiMap, String shiteiNo,
			String shubetsu) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		List<DiffRow> diffRows = new ArrayList<>();

		List<Fuka> prevFukaList = new ArrayList<>();
		List<List<FukaUchi>> prevUchiList = new ArrayList<>();
		if (EltaxConstants.SHUBETSU_TEIGAKU.equals(shubetsu) || EltaxConstants.SHUBETSU_TEIRITSU.equals(shubetsu)) {
			// 特例納入以外
			int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月");
			String taishoYm = getDataValue(dataRow, taishoYmIdx);
			String nendo = toNendo(taishoYm);
			Integer kibetsu = toKibetsu(taishoYm);

			List<Fuka> existingFuka = null;
			if (shiteiNo != null && !shiteiNo.isBlank()) {
				existingFuka = fukaRepository.findLatestByNendoAndKibetsu(jichitaiCd, shiteiNo, nendo, kibetsu);
			}
			if (existingFuka != null && !existingFuka.isEmpty()) {
				Fuka prevFuka = existingFuka.get(0);
				prevFukaList.add(prevFuka);
				List<FukaUchi> prevUchi = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
						jichitaiCd, shiteiNo,
						prevFuka.getRno(), prevFuka.getNendo(), prevFuka.getKibetsu());
				prevUchiList.add(prevUchi);
			}
		} else {
			// 特例納入
			for (String suffix : List.of("１", "２", "３")) {
				int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月" + suffix);
				String taishoYm = getDataValue(dataRow, taishoYmIdx);
				if (taishoYm.isBlank())
					continue;
				String nendo = toNendo(taishoYm);
				Integer kibetsu = toKibetsu(taishoYm);

				List<Fuka> existingFuka = null;
				if (shiteiNo != null && !shiteiNo.isBlank()) {
					existingFuka = fukaRepository.findLatestByNendoAndKibetsu(jichitaiCd, shiteiNo, nendo, kibetsu);
				}
				if (existingFuka != null && !existingFuka.isEmpty()) {
					Fuka prevFuka = existingFuka.get(0);
					prevFukaList.add(prevFuka);
					List<FukaUchi> prevUchi = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
							jichitaiCd, shiteiNo,
							prevFuka.getRno(), prevFuka.getNendo(), prevFuka.getKibetsu());
					prevUchiList.add(prevUchi);
				}
			}
		}

		for (Map.Entry<Integer, YoshikiItem> entry : yoshikiMap.entrySet()) {
			if (entry.getValue().dispFlg().equals("0"))
				continue;
			String itemName = entry.getValue().itemName();
			String afterValue = getDataValue(dataRow, entry.getKey() - 1);
			String beforeValue = null;
			if (EltaxConstants.SHUBETSU_TEIGAKU.equals(shubetsu)) {
				beforeValue = resolveBeforeValueFuka(prevFukaList, prevUchiList, itemName, false, true);
			} else if (EltaxConstants.SHUBETSU_TEIRITSU.equals(shubetsu)) {
				beforeValue = resolveBeforeValueFuka(prevFukaList, prevUchiList, itemName, false, false);
			} else if (EltaxConstants.SHUBETSU_TOKU_TEIGAKU.equals(shubetsu)) {
				beforeValue = resolveBeforeValueFuka(prevFukaList, prevUchiList, itemName, true, true);
			} else if (EltaxConstants.SHUBETSU_TOKU_TEIRITSU.equals(shubetsu)) {
				beforeValue = resolveBeforeValueFuka(prevFukaList, prevUchiList, itemName, true, false);
			}
			diffRows.add(new DiffRow(itemName, beforeValue, afterValue, entry.getValue().dispFlg(),
					entry.getValue().requiredFlg()));
		}
		return diffRows;
	}

	private String resolveBeforeValueFuka(List<Fuka> prevFukaList, List<List<FukaUchi>> prevUchiList, String itemName,
			boolean isToku, boolean isTeigaku) {
		if (prevFukaList == null || prevFukaList.isEmpty() || prevUchiList == null || prevUchiList.isEmpty())
			return "－";

		if (itemName.equals("提出年月日")) {
			return parseString(prevFukaList.get(0).getShinkokuYmd());
		}

		String taishoYmPrefix = null;
		int idx = -1;
		if (isToku) {
			for (int i = 0; i < 3; i++) {
				taishoYmPrefix = "納入税額－行為年月" + List.of("１－", "２－", "３－").get(i);
				if (itemName.startsWith(taishoYmPrefix)) {
					idx = i;
					break;
				}
			}
		} else {
			taishoYmPrefix = "納入税額－行為年月－";
			if (itemName.startsWith(taishoYmPrefix))
				idx = 0;
		}
		if (idx < 0 || prevFukaList.size() <= idx || prevUchiList.size() <= idx)
			return "－";

		if (itemName.startsWith(taishoYmPrefix + "課税対象宿泊合計【宿泊数】")) {

		}

		if (isTeigaku) {
			if (itemName.startsWith(taishoYmPrefix + "課税対象宿泊合計【宿泊数】")) {
				return parseString(prevFukaList.get(idx).getKazeiHakusu());
			} else if (itemName.startsWith(taishoYmPrefix + "課税対象宿泊合計【税額】")) {
				return parseString(prevFukaList.get(idx).getZeigaku());
			} else if (itemName.startsWith(taishoYmPrefix + "課税免除【宿泊数】")) {
				return parseString(prevFukaList.get(idx).getMenjoHakusu());
			} else if (itemName.startsWith(taishoYmPrefix + "合計【宿泊数】")) {
				return parseString(prevFukaList.get(idx).getTotalHakusu());
			} else if (itemName.startsWith(taishoYmPrefix + "合計【税額】")) {
				return parseString(prevFukaList.get(idx).getTotalZeigaku());
			}
		} else {
			if (itemName.startsWith(taishoYmPrefix + "課税対象宿泊合計【宿泊者数】")) {
				return parseString(prevFukaList.get(idx).getKazeiHakusu());
			} else if (itemName.startsWith(taishoYmPrefix + "課税対象宿泊合計【宿泊料金】")) {
				return parseString(prevFukaList.get(idx).getKazeiRyokin());
			} else if (itemName.startsWith(taishoYmPrefix + "課税対象宿泊合計【税額】")) {
				return parseString(prevFukaList.get(idx).getZeigaku());
			} else if (itemName.startsWith(taishoYmPrefix + "課税免除【宿泊者数】")) {
				return parseString(prevFukaList.get(idx).getMenjoHakusu());
			} else if (itemName.startsWith(taishoYmPrefix + "課税免除【宿泊料金】")) {
				return parseString(prevFukaList.get(idx).getMenjoRyokin());
			} else if (itemName.startsWith(taishoYmPrefix + "合計【宿泊者数】")) {
				return parseString(prevFukaList.get(idx).getTotalHakusu());
			} else if (itemName.startsWith(taishoYmPrefix + "合計【税額】")) {
				return parseString(prevFukaList.get(idx).getTotalZeigaku());
			}
		}

		for (int kbn = 1; kbn <= 10; kbn++) {
			if (prevUchiList.get(idx).size() < kbn) {
				continue;
			}
			String kbnStr = toFullWidth(kbn);
			FukaUchi prevUchi = prevUchiList.get(idx).get(kbn - 1);
			if (isTeigaku) {
				if (itemName.startsWith(taishoYmPrefix + "申告区分" + kbnStr + "【税率】")) {
					return parseString(prevUchi.getZeiRitsu());
				} else if (itemName.startsWith(taishoYmPrefix + "申告区分" + kbnStr + "【宿泊数】")) {
					return parseString(prevUchi.getHakusu());
				} else if (itemName.startsWith(taishoYmPrefix + "申告区分" + kbnStr + "【税額】")) {
					return parseString(prevUchi.getZeigaku());
				}
			} else {
				if (itemName.startsWith(taishoYmPrefix + "申告区分" + kbnStr + "【宿泊料金の総額】")) {
					return parseString(prevUchi.getRyokinSogaku());
				} else if (itemName.startsWith(taishoYmPrefix + "申告区分" + kbnStr + "【宿泊者数】")) {
					return parseString(prevUchi.getHakusu());
				} else if (itemName.startsWith(taishoYmPrefix + "申告区分" + kbnStr + "【宿泊料金】")) {
					return parseString(prevUchi.getRyokin());
				} else if (itemName.startsWith(taishoYmPrefix + "申告区分" + kbnStr + "【税率】")) {
					return parseString(prevUchi.getZeiRitsu());
				} else if (itemName.startsWith(taishoYmPrefix + "申告区分" + kbnStr + "【税額】")) {
					return parseString(prevUchi.getZeigaku());
				}
			}
		}

		return "－";
	}

	/**
	 * 特別徴収義務者登録申請書（種別01）のDB更新
	 */
	private void saveTokugimu(byte[] fileBytes, BigDecimal atenaNoFromSession, String overrideShiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		String tetsuzukiId = extractTetsuzukiId(fileBytes);

		Map<Integer, YoshikiItem> yoshikiMap;
		String[] dataRow;
		try {
			yoshikiMap = loadYoshikiMap(tetsuzukiId);
			dataRow = parseBytesAsCsv(fileBytes);
		} catch (IOException e) {
			throw new RuntimeException("特別徴収義務者情報の更新に失敗しました: " + e.getMessage(), e);
		}

		// 様式CSVのインデックス取得（1始まり→0始まり変換済み）
		int shinseikubunIdx = findIndexByName(yoshikiMap, "特別徴収義務者【申請区分");
		int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");
		int shiteiNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
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
		int kyuhaishiRiyuIdx = findIndexByName(yoshikiMap, "届出理由（変更・休止・廃止・再開）");
		int kyushiStYmdIdx = findIndexByName(yoshikiMap, "休止廃止再開情報【休止期間（自");
		int kyushiEdYmdIdx = findIndexByName(yoshikiMap, "休止廃止再開情報【休止期間（至");
		int eigyoEdYmdIdx = findIndexByName(yoshikiMap, "休止廃止再開情報【廃止年月日");
		int saikaiYmdIdx = findIndexByName(yoshikiMap, "休止廃止再開情報【再開年月日");
		int shoyushaNameIdx = findIndexByName(yoshikiMap, "施設の所有者情報【氏名");
		int shoyushaYubinNoIdx = findIndexByName(yoshikiMap, "施設の所有者情報【郵便番号");
		int shoyushaJushoIdx = findIndexByName(yoshikiMap, "施設の所有者情報【住所又は所在地");
		int shoyushaTelIdx = findIndexByName(yoshikiMap, "施設の所有者情報【電話番号");
		int kyodoJigyoshaNameIdx = findIndexByName(yoshikiMap, "共同事業者情報【氏名");
		int kyodoJigyoshaYubinNoIdx = findIndexByName(yoshikiMap, "共同事業者情報【郵便番号");
		int kyodoJigyoshaJushoIdx = findIndexByName(yoshikiMap, "共同事業者情報【住所又は所在地");
		int kyodoJigyoshaTelIdx = findIndexByName(yoshikiMap, "共同事業者情報【電話番号");

		String shinseikubun = getDataValue(dataRow, shinseikubunIdx);
		String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);
		boolean isNew = shinseikubun.startsWith(EltaxConstants.SHINSEI_KBN_SHINKI);

		// 指定番号の決定
		String shiteiNo;
		if (isNew) {
			String prefix = jichitaiRepository.findById(jichitaiCd)
					.map(j -> j.getShiteiStChar() != null ? j.getShiteiStChar() : "000")
					.orElse("000");
			int max = tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(jichitaiCd, prefix).orElse(0);
			shiteiNo = prefix + String.format("%05d", max + 1);
		} else {
			shiteiNo = overrideShiteiNo != null && !overrideShiteiNo.isBlank() ? overrideShiteiNo
					: getDataValue(dataRow, shiteiNoIdx);
		}

		// 前履歴取得
		List<Tokugimu> prevList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
		Tokugimu prev = prevList.isEmpty() ? null : prevList.get(0);
		if (!isNew && prev == null) {
			throw new RuntimeException("指定番号（" + shiteiNo + "）に該当する特別徴収義務者が登録されていません。");
		}

		// 履歴番号
		int maxRno = tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).orElse(0);
		BigDecimal newRno = BigDecimal.valueOf(maxRno + 1);

		// 登録年月日
		LocalDate torokuYmd = isNew ? LocalDate.now() : prev.getTorokuYmd();
		LocalDate shinkokuYmd = parseDate(teishutsuYmd);
		if (shinkokuYmd == null)
			shinkokuYmd = LocalDate.now();

		// 宛名番号
		BigDecimal atenaNo;
		if (isNew) {
			if (atenaNoFromSession != null) {
				atenaNo = atenaNoFromSession;
			} else {
				throw new RuntimeException("宛名番号が検索されていません。");
			}
		} else {
			atenaNo = prev.getAtenaNo();
		}

		// 営業開始終了年月日、休止開始終了年月日
		LocalDate eigyoStYmd = parseDate(getDataValue(dataRow, eigyoStYmdIdx)) == null
				? (isNew ? LocalDate.now() : prev.getEigyoStYmd())
				: parseDate(getDataValue(dataRow, eigyoStYmdIdx));
		LocalDate eigyoEdYmd = null;
		LocalDate kyushiStYmd = null;
		LocalDate kyushiEdYmd = null;
		String kyuhaishiRiyu = getDataValue(dataRow, kyuhaishiRiyuIdx).isEmpty()
				? (isNew ? null : prev.getKyuhaishiRiyu())
				: getDataValue(dataRow, kyuhaishiRiyuIdx);
		switch (shinseikubun) {
		case EltaxConstants.SHINSEI_KBN_KYUSHI:
			eigyoEdYmd = prev.getEigyoEdYmd();
			kyushiStYmd = parseDate(getDataValue(dataRow, kyushiStYmdIdx));
			if (kyushiStYmd == null) {
				throw new RuntimeException("休止年月日が入力されていません。");
			}
			kyushiEdYmd = parseDate(getDataValue(dataRow, kyushiEdYmdIdx));
			kyuhaishiRiyu = getDataValue(dataRow, kyuhaishiRiyuIdx);
			break;
		case EltaxConstants.SHINSEI_KBN_SAIKAI:
			eigyoEdYmd = prev.getEigyoEdYmd();
			kyushiStYmd = prev.getKyushiStYmd();
			kyushiEdYmd = parseDate(getDataValue(dataRow, saikaiYmdIdx));
			if (kyushiEdYmd == null) {
				throw new RuntimeException("再開年月日が入力されていません。");
			}
			kyushiEdYmd = kyushiEdYmd.minusDays(1);
			kyuhaishiRiyu = getDataValue(dataRow, kyuhaishiRiyuIdx);
			break;
		case EltaxConstants.SHINSEI_KBN_HAISHI:
			eigyoEdYmd = parseDate(getDataValue(dataRow, eigyoEdYmdIdx));
			if (eigyoEdYmd == null) {
				throw new RuntimeException("廃止年月日が入力されていません。");
			}
			kyushiStYmd = prev.getKyushiStYmd();
			kyushiEdYmd = kyushiStYmd != null && prev.getKyushiEdYmd() == null
					? parseDate(getDataValue(dataRow, eigyoEdYmdIdx))
					: prev.getKyushiEdYmd();
			kyuhaishiRiyu = getDataValue(dataRow, kyuhaishiRiyuIdx);
			break;
		default:
			if (!isNew) {
				eigyoEdYmd = prev.getEigyoEdYmd();
				kyushiStYmd = prev.getKyushiStYmd();
				kyushiEdYmd = prev.getKyushiEdYmd();
				kyuhaishiRiyu = prev.getKyuhaishiRiyu();
			}
			break;

		}

		// 営業種別変換
		String kyokaShu = convertKyokaShu(getDataValue(dataRow, kyokaShuIdx));

		String kyokaName = getDataValue(dataRow, kyokaNameIdx).isEmpty()
				? !isNew && !prev.getKyokaName().isEmpty() ? prev.getKyokaName() : ""
				: getDataValue(dataRow, kyokaNameIdx);
		String kyokaYubinNo = getDataValue(dataRow, kyokaYubinNoIdx).isEmpty()
				? !isNew && !prev.getKyokaYubinNo().isEmpty() ? prev.getKyokaYubinNo() : ""
				: getDataValue(dataRow, kyokaYubinNoIdx);
		String kyokaJusho = getDataValue(dataRow, kyokaJushoIdx).isEmpty()
				? !isNew && !prev.getKyokaJusho().isEmpty() ? prev.getKyokaJusho() : ""
				: getDataValue(dataRow, kyokaJushoIdx);
		String soufusakiName = getDataValue(dataRow, soufusakiNameIdx).isEmpty()
				? !isNew && !prev.getSoufusakiName().isEmpty() ? prev.getSoufusakiName() : ""
				: getDataValue(dataRow, soufusakiNameIdx);

		Tokugimu entity = new Tokugimu();
		entity.setJichitaiCd(jichitaiCd);
		entity.setShiteiNo(shiteiNo);
		entity.setRno(newRno);
		entity.setTorokuYmd(torokuYmd);
		entity.setShinkokuYmd(shinkokuYmd);
		entity.setHenkoYmd(shinkokuYmd);
		entity.setAtenaNo(atenaNo);
		entity.setShisetsuName(getDataValue(dataRow, shisetsuNameIdx));
		entity.setShisetsuNameKana(!isNew ? prev.getShisetsuNameKana() : "");
		entity.setShisetsuYubinNo(!isNew ? prev.getShisetsuYubinNo() : null);
		entity.setShisetsuJusho(getDataValue(dataRow, shisetsuJushoIdx));
		entity.setShisetsuTel(getDataValue(dataRow, shisetsuTelIdx));
		entity.setYukaMenseki(parseBigDecimal(getDataValue(dataRow, yukaMensekiIdx)));
		entity.setChijoKai(parseBigDecimal(getDataValue(dataRow, chijoKaiIdx)));
		entity.setChikaKai(parseBigDecimal(getDataValue(dataRow, chikaKaiIdx)));
		entity.setKyakushitsuSu(parseBigDecimal(getDataValue(dataRow, kyakushitsuSuIdx)));
		entity.setShuyoSu(parseBigDecimal(getDataValue(dataRow, shuyoSuIdx)));
		entity.setKyokaName(kyokaName);
		entity.setKyokaNameKana(!isNew ? prev.getKyokaNameKana() : "");
		entity.setKyokaYubinNo(kyokaYubinNo);
		entity.setKyokaJusho(kyokaJusho);
		entity.setKyokaTel(!isNew ? prev.getKyokaTel() : null);
		entity.setKyokaShu(kyokaShu);
		entity.setKyokaNo(getDataValue(dataRow, kyokaNoIdx));
		entity.setSoufusakiName(soufusakiName);
		entity.setSoufusakiNameKana(!isNew ? prev.getSoufusakiNameKana() : "");
		entity.setSoufusakiYubinNo(getDataValue(dataRow, soufusakiYubinNoIdx));
		entity.setSoufusakiJusho(getDataValue(dataRow, soufusakiJushoIdx));
		entity.setSoufusakiTel(getDataValue(dataRow, soufusakiTelIdx));
		entity.setBiko(getDataValue(dataRow, bikoIdx));
		entity.setEigyoStYmd(eigyoStYmd);
		entity.setEigyoEdYmd(eigyoEdYmd);
		entity.setKyushiStYmd(kyushiStYmd);
		entity.setKyushiEdYmd(kyushiEdYmd);
		entity.setKyuhaishiRiyu(kyuhaishiRiyu);
		entity.setEltaxUmu("1");
		entity.setNewFlg("1");
		entity.setDelFlg("0");
		tokugimuRepository.save(entity);
		if (prev != null) {
			// 履歴の最新フラグを"0"にする
			prev.setNewFlg("0");
			tokugimuRepository.save(prev);
		}

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

		// t_kyodo_jigyosha
		String kyodoJigyoshaName = getDataValue(dataRow, kyodoJigyoshaNameIdx);
		if (!shoyushaName.isBlank()) {
			String prevKyodoJigyoshaNameKana = kyodoJigyoshaRepository
					.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).stream()
					.filter(s -> prev != null && s.getRno().compareTo(prev.getRno()) == 0
							&& s.getIdx().compareTo(BigDecimal.ONE) == 0)
					.map(KyodoJigyosha::getKyodoJigyoshaNameKana)
					.findFirst()
					.orElse("");
			KyodoJigyosha kyodoJigyosha = new KyodoJigyosha();
			kyodoJigyosha.setJichitaiCd(jichitaiCd);
			kyodoJigyosha.setShiteiNo(shiteiNo);
			kyodoJigyosha.setRno(newRno);
			kyodoJigyosha.setIdx(BigDecimal.ONE);
			kyodoJigyosha.setKyodoJigyoshaName(kyodoJigyoshaName);
			kyodoJigyosha.setKyodoJigyoshaNameKana(prevKyodoJigyoshaNameKana);
			kyodoJigyosha.setKyodoJigyoshaYubinNo(getDataValue(dataRow, kyodoJigyoshaYubinNoIdx));
			kyodoJigyosha.setKyodoJigyoshaJusho(getDataValue(dataRow, kyodoJigyoshaJushoIdx));
			kyodoJigyosha.setKyodoJigyoshaTel(getDataValue(dataRow, kyodoJigyoshaTelIdx));
			kyodoJigyoshaRepository.save(kyodoJigyosha);
		}
	}

	/**
	 * 納入申告（定額）（種別02）のDB更新
	 */
	private void saveNonyuTeigaku(byte[] fileBytes, String overrideShiteiNo) {
		try {
			String tetsuzukiId = extractTetsuzukiId(fileBytes);
			Map<Integer, YoshikiItem> yoshikiMap = loadYoshikiMap(tetsuzukiId);
			String[] dataRow = parseBytesAsCsv(fileBytes);

			int shiteiNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月");
			int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");

			String shiteiNo = overrideShiteiNo != null && !overrideShiteiNo.isBlank() ? overrideShiteiNo
					: getDataValue(dataRow, shiteiNoIdx);
			String taishoYm = getDataValue(dataRow, taishoYmIdx);
			String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);

			saveFuka(shiteiNo, taishoYm, teishutsuYmd, FukaConstants.TEIGAKU, dataRow, yoshikiMap,
					"納入税額－行為年月－");
		} catch (Exception e) {
			throw new RuntimeException("賦課情報の更新に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * 納入申告（定率）（種別03）のDB更新
	 */
	private void saveNonyuTeiritsu(byte[] fileBytes, String overrideShiteiNo) {
		try {
			String tetsuzukiId = extractTetsuzukiId(fileBytes);
			Map<Integer, YoshikiItem> yoshikiMap = loadYoshikiMap(tetsuzukiId);
			String[] dataRow = parseBytesAsCsv(fileBytes);

			int shiteiNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月");
			int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");

			String shiteiNo = overrideShiteiNo != null && !overrideShiteiNo.isBlank() ? overrideShiteiNo
					: getDataValue(dataRow, shiteiNoIdx);
			String taishoYm = getDataValue(dataRow, taishoYmIdx);
			String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);

			saveFuka(shiteiNo, taishoYm, teishutsuYmd, FukaConstants.TEIRITSU, dataRow, yoshikiMap,
					"納入税額－行為年月－");
		} catch (Exception e) {
			throw new RuntimeException("賦課情報の更新に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * 特例納入申告（定額）（種別04）のDB更新
	 */
	private void saveNonyuTokureiTeigaku(byte[] fileBytes, String overrideShiteiNo) {
		try {
			String tetsuzukiId = extractTetsuzukiId(fileBytes);
			Map<Integer, YoshikiItem> yoshikiMap = loadYoshikiMap(tetsuzukiId);
			String[] dataRow = parseBytesAsCsv(fileBytes);

			int shiteiNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");
			String shiteiNo = overrideShiteiNo != null && !overrideShiteiNo.isBlank() ? overrideShiteiNo
					: getDataValue(dataRow, shiteiNoIdx);
			String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);

			for (String suffix : List.of("１", "２", "３")) {
				int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月" + suffix);
				String taishoYm = getDataValue(dataRow, taishoYmIdx);
				if (taishoYm.isBlank())
					continue;
				saveFuka(shiteiNo, taishoYm, teishutsuYmd,
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
	private void saveNonyuTokureiTeiritsu(byte[] fileBytes, String overrideShiteiNo) {
		try {
			String tetsuzukiId = extractTetsuzukiId(fileBytes);
			Map<Integer, YoshikiItem> yoshikiMap = loadYoshikiMap(tetsuzukiId);
			String[] dataRow = parseBytesAsCsv(fileBytes);

			int shiteiNoIdx = findIndexByName(yoshikiMap, "施設情報【施設番号");
			int teishutsuYmdIdx = findIndexByName(yoshikiMap, "提出年月日");
			String shiteiNo = overrideShiteiNo != null && !overrideShiteiNo.isBlank() ? overrideShiteiNo
					: getDataValue(dataRow, shiteiNoIdx);
			String teishutsuYmd = getDataValue(dataRow, teishutsuYmdIdx);

			for (String suffix : List.of("１", "２", "３")) {
				int taishoYmIdx = findIndexByName(yoshikiMap, "納入税額－行為年月" + suffix);
				String taishoYm = getDataValue(dataRow, taishoYmIdx);
				if (taishoYm.isBlank())
					continue;
				saveFuka(shiteiNo, taishoYm, teishutsuYmd,
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
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		try {
			BigDecimal nextSeq = eltaxRenkeiRepository.findNextSeq(jichitaiCd);
			String tetsuzukiId = fileBytes.length > 0 ? extractTetsuzukiId(fileBytes) : "";
			String shubetsu = EltaxConstants.TETSUZUKI_SHUBETSU_MAP.getOrDefault(tetsuzukiId, "");

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

	@Transactional
	private void saveFuka(String shiteiNo, String taishoYm, String teishutsuYmd,
			FukaConstants fukaKbn, String[] dataRow, Map<Integer, YoshikiItem> yoshikiMap,
			String taishoYmPrefix) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		String nendo = toNendo(taishoYm);
		Integer kibetsu = toKibetsu(taishoYm);
		boolean isTeigaku = FukaConstants.TEIGAKU.equals(fukaKbn);

		List<Fuka> prevList = fukaRepository.findLatestByNendoAndKibetsu(jichitaiCd, shiteiNo, nendo, kibetsu);
		Fuka prev = prevList.isEmpty() ? null : prevList.get(0);

		int newRno = (prev != null ? prev.getRno() : 0) + 1;
		LocalDate shinkokuYmd = parseDate(teishutsuYmd);
		if (shinkokuYmd == null)
			shinkokuYmd = LocalDate.now();

		int kazeiHakusuIdx = isTeigaku
				? findIndexByName(yoshikiMap, taishoYmPrefix + "課税対象宿泊合計【宿泊数】")
				: findIndexByName(yoshikiMap, taishoYmPrefix + "課税対象宿泊合計【宿泊者数】");
		int kazeiRyokinIdx = isTeigaku ? -1
				: findIndexByName(yoshikiMap, taishoYmPrefix + "課税対象宿泊合計【宿泊料金】");
		int zeigakuIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "課税対象宿泊合計【税額】");
		int menjoHakusuIdx = isTeigaku
				? findIndexByName(yoshikiMap, taishoYmPrefix + "課税免除【宿泊数】")
				: findIndexByName(yoshikiMap, taishoYmPrefix + "課税免除【宿泊者数】");
		int menjoRyokinIdx = isTeigaku ? -1
				: findIndexByName(yoshikiMap, taishoYmPrefix + "課税免除【宿泊料金】");
		int totalHakusuIdx = isTeigaku
				? findIndexByName(yoshikiMap, taishoYmPrefix + "合計【宿泊数】")
				: findIndexByName(yoshikiMap, taishoYmPrefix + "合計【宿泊者数】");
		int totalZeigakuIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "合計【税額】");

		Fuka fuka = new Fuka();
		fuka.setJichitaiCd(jichitaiCd);
		fuka.setShiteiNo(shiteiNo);
		fuka.setRno(newRno);
		fuka.setNendo(nendo);
		fuka.setKibetsu(kibetsu);
		fuka.setTorokuYmd(shinkokuYmd);
		fuka.setShinkokuYmd(shinkokuYmd);
		fuka.setTaishoYm(taishoYm);
		fuka.setFukaKbn(fukaKbn.getValue());
		fuka.setHenkoKbn(prev != null ? prev.getHenkoKbn() : FukaConstants.SHINKOKU.getValue());
		fuka.setKazeiHakusu(parseLong(getDataValue(dataRow, kazeiHakusuIdx)));
		fuka.setKazeiRyokin(parseLong(getDataValue(dataRow, kazeiRyokinIdx)));
		fuka.setZeigaku(parseLong(getDataValue(dataRow, zeigakuIdx)));
		fuka.setMenjoHakusu(parseLong(getDataValue(dataRow, menjoHakusuIdx)));
		fuka.setMenjoRyokin(parseLong(getDataValue(dataRow, menjoRyokinIdx)));
		fuka.setTotalHakusu(parseLong(getDataValue(dataRow, totalHakusuIdx)));
		fuka.setTotalZeigaku(parseLong(getDataValue(dataRow, totalZeigakuIdx)));
		fuka.setKenZeigaku(0L);
		fuka.setCityZeigaku(0L);
		fuka.setKasanKbn1(prev != null ? prev.getKasanKbn1() : null);
		fuka.setKasanRitsu1(prev != null ? prev.getKasanRitsu1() : null);
		fuka.setKasanGaku1(prev != null ? prev.getKasanGaku1() : null);
		fuka.setNokigen(prev != null ? prev.getNokigen() : null);
		fuka.setEntaikin(prev != null ? prev.getEntaikin() : null);
		fuka.setKasanKbn2(prev != null ? prev.getKasanKbn2() : null);
		fuka.setKasanRitsu2(prev != null ? prev.getKasanRitsu2() : null);
		fuka.setKasanGaku2(prev != null ? prev.getKasanGaku2() : null);
		fuka.setKasanKbn3(prev != null ? prev.getKasanKbn3() : null);
		fuka.setKasanRitsu3(prev != null ? prev.getKasanRitsu3() : null);
		fuka.setKasanGaku3(prev != null ? prev.getKasanGaku3() : null);
		fuka.setNewFlg("1");
		fuka.setDelFlg("0");
		fuka = fukaRepository.save(fuka);
		if (prev != null) {
			// 履歴の最新フラグを"0"にする
			prev.setNewFlg("0");
			fukaRepository.save(prev);
		}

		long totalKenZeigaku = 0L;
		for (int kbn = 1; kbn <= 10; kbn++) {
			String kbnStr = toFullWidth(kbn);
			int hakusuIdx, ryokinSogakuIdx, ryokinIdx, zeiRitsuIdx, uchiZeigakuIdx;
			if (isTeigaku) {
				zeiRitsuIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【税率】");
				hakusuIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【宿泊数】");
				uchiZeigakuIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【税額】");
				ryokinSogakuIdx = -1;
				ryokinIdx = -1;
			} else {
				ryokinSogakuIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【宿泊料金の総額】");
				hakusuIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【宿泊者数】");
				ryokinIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【宿泊料金】");
				zeiRitsuIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【税率】");
				uchiZeigakuIdx = findIndexByName(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【税額】");
			}
			if (hakusuIdx < 0 || getDataValue(dataRow, hakusuIdx).isBlank())
				continue;

			BigDecimal zeiritsuSeq = null;
			Long uchiKenZeigaku = null;
			Long uchiCityZeigaku = null;
			Long uchiZeigaku = parseLong(getDataValue(dataRow, uchiZeigakuIdx));
			Long uchiHakuSu = parseLong(getDataValue(dataRow, hakusuIdx));
			if (isTeigaku) {
				List<ZeiritsuTeigaku> teigakuList = zeiritsuTeigakuRepository
						.findActiveByTaishoKbnAndTekiyoYm(jichitaiCd, ZeiritsuConstants.CITY.getValue(), taishoYm);
				if (teigakuList.size() < kbn)
					throw new RuntimeException("申告区分" + kbn + "に該当する税率定額詳細マスタが存在しません。");
				zeiritsuSeq = teigakuList.get(kbn - 1).getTeigakuSeq();
				uchiKenZeigaku = teigakuList.get(kbn - 1).getZeigaku() * uchiHakuSu;
				uchiCityZeigaku = uchiZeigaku - uchiKenZeigaku;
				totalKenZeigaku += uchiCityZeigaku;
			} else {
				List<ZeiritsuTeiritsu> teiritsuList = zeiritsuTeiritsuRepository
						.findActiveByTaishoKbnAndTekiyoYm(jichitaiCd, ZeiritsuConstants.CITY.getValue(), taishoYm);
				if (teiritsuList.size() < kbn)
					throw new RuntimeException("申告区分" + kbn + "に該当する税率定率詳細マスタが存在しません。");
				zeiritsuSeq = teiritsuList.get(kbn - 1).getTeiritsuSeq();
			}

			FukaUchi uchi = new FukaUchi();
			uchi.setJichitaiCd(jichitaiCd);
			uchi.setShiteiNo(shiteiNo);
			uchi.setRno(newRno);
			uchi.setNendo(nendo);
			uchi.setKibetsu(kibetsu);
			uchi.setKazeiKbn(kbn);
			uchi.setZeiritsuSeq(zeiritsuSeq);
			uchi.setFukaKbn(fukaKbn.getValue());
			uchi.setRyokinSogaku(parseLong(getDataValue(dataRow, ryokinSogakuIdx)));
			uchi.setHakusu(uchiHakuSu);
			uchi.setRyokin(parseLong(getDataValue(dataRow, ryokinIdx)));
			uchi.setZeiRitsu(parseBigDecimal(getDataValue(dataRow, zeiRitsuIdx)));
			uchi.setZeigaku(uchiZeigaku);
			uchi.setCityZeigaku(uchiCityZeigaku);
			uchi.setKenZeigaku(uchiKenZeigaku);
			fukaUchiRepository.save(uchi);
		}

		if (isTeigaku) {
			fuka.setKenZeigaku(totalKenZeigaku);
			fuka.setCityZeigaku(fuka.getTotalZeigaku() - totalKenZeigaku);
		} else {
			long totalRyokin = fuka.getKazeiRyokin();
			long totalShukuhakushaSu = fuka.getKazeiHakusu();
			long ryokin = totalShukuhakushaSu > 0 ? totalRyokin / totalShukuhakushaSu : 0;
			long kenZeigaku = getKenZeigaku(ryokin, taishoYm) * totalShukuhakushaSu;
			long cityZeigaku = fuka.getTotalZeigaku() - kenZeigaku >= 0
					? fuka.getTotalZeigaku() - kenZeigaku
					: 0L;
			kenZeigaku = fuka.getTotalZeigaku() - cityZeigaku;
			fuka.setCityZeigaku(cityZeigaku);
			fuka.setKenZeigaku(kenZeigaku);
		}
		fukaRepository.save(fuka);
	}

	private long getKenZeigaku(Long shukuhakuRyokin, String taishoYm) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		Optional<ZeiritsuTeigaku> teigakuOp = zeiritsuTeigakuRepository
				.findActiveByTaishoKbnAndTekiyoYmAndRyokin(jichitaiCd, ZeiritsuConstants.KEN.getValue(), taishoYm,
						shukuhakuRyokin);
		return teigakuOp.map(ZeiritsuTeigaku::getZeigaku).orElse(0L);
	}

	private String toNendo(String taishoYm) {
		if (taishoYm == null || taishoYm.length() < 6)
			return "";
		int year = Integer.parseInt(taishoYm.substring(0, 4));
		int month = Integer.parseInt(taishoYm.substring(4, 6));
		return String.valueOf(month <= 2 ? year - 1 : year);
	}

	private Integer toKibetsu(String taishoYm) {
		if (taishoYm == null || taishoYm.length() < 6)
			return null;
		int month = Integer.parseInt(taishoYm.substring(4, 6));
		return month >= 3 ? month - 2 : month + 10;
	}

	private String toFullWidth(int n) {
		String[] fw = { "", "１", "２", "３", "４", "５", "６", "７", "８", "９", "１０" };
		return n <= 10 ? fw[n] : String.valueOf(n);
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

	private Long parseLong(String value) {
		if (value == null || value.isBlank())
			return null;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private <T> String parseString(T value) {
		if (value == null)
			return "－";

		String retValue = "－";
		if (value instanceof String) {
			retValue = (String) value;
		} else if (value instanceof BigDecimal) {
			retValue = ((BigDecimal) value).toPlainString();
		} else if (value instanceof Integer) {
			retValue = ((Integer) value).toString();
		} else if (value instanceof Long) {
			retValue = ((Long) value).toString();
		} else if (value instanceof LocalDate) {
			retValue = ((LocalDate) value).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		}

		return StringUtils.hasText(retValue) ? retValue : "－";
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

	/**
	 * 申請区分変換：eLTAX値 → 区分名称
	 * 
	 * @param raw eLTAX値
	 * @return 区分名称 
	 */
	private String convertShinseiKbn(String raw) {
		return switch (raw) {
		case "1" -> "1";
		case "2" -> "3"; // 簡易宿所営業 → 簡易宿所
		case "3", "4" -> "4"; // 下宿営業・その他 → 民泊
		default -> raw;
		};
	}
}
