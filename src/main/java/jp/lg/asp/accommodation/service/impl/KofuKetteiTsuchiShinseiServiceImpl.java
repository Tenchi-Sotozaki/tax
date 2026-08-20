package jp.lg.asp.accommodation.service.impl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.atilika.kuromoji.ipadic.Tokenizer;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税特別徴収事務交付金決定通知書・交付申請書 Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KofuKetteiTsuchiShinseiServiceImpl implements KofuKetteiTsuchiShinseiService {

	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final ShoreikinRepository shoreikinRepository;
	private final ReportsDefRepository reportsDefRepository;
	private final ReportsCommonService reportsCommonService;
	private final FurikomiKozaRepository furikomiKozaRepository;

	private final JichitaiContext jichitaiContext;

	private String jichitaiName;
	private String jorei;
	private byte[] koin;

	private void init() {
		Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
		if (jichitaiInfo != null) {
			jichitaiName = jichitaiInfo.getName();
		}
		jorei = reportsCommonService.getReportsDefText(ReportsConstants.SHOREIKIN_KOFU_JOREI);
		koin = reportsCommonService.getReportsDefData(ReportsConstants.KOIN);
	}

	@Override
	public KofuKetteiTsuchiShinseiDto getReportData(String shiteiNo) {
		init();
		// デフォルト年度（現在年度）で取得
		LocalDate now = LocalDate.now();
		String currentNendo = String.valueOf(
				now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1);
		return getReportData(shiteiNo, currentNendo);
	}

	@Override
	public KofuKetteiTsuchiShinseiDto getReportData(String shiteiNo, String nendo) {
		init();
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		try {
			log.debug("交付申請書データ取得開始 - 指定番号: {}, 年度: {}", shiteiNo, nendo);

			// 特別徴収義務者情報取得（最新・未削除）
			Optional<Tokugimu> tokugimuOpt = tokugimuRepository
					.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
							jichitaiCode, shiteiNo, "1", "0");

			if (tokugimuOpt.isEmpty()) {
				log.error("特別徴収義務者が見つかりません: shiteiNo={}", shiteiNo);
				return null;
			}

			Tokugimu tokugimu = tokugimuOpt.get();
			log.debug("特別徴収義務者情報取得: {}", tokugimu.getShiteiNo());

			// 宛名情報取得
			Optional<Atena> atenaOpt = atenaRepository
					.findByJichitaiCdAndAtenaNo(jichitaiCode, tokugimu.getAtenaNo());

			if (atenaOpt.isEmpty()) {
				log.error("宛名情報が見つかりません: atenaNo={}", tokugimu.getAtenaNo());
				return null;
			}

			Atena atena = atenaOpt.get();
			log.debug("宛名情報取得: {}", atena.getName());

			// 奨励金情報取得
			Optional<Shoreikin> shoreikinOpt = shoreikinRepository
					.findByJichitaiCdAndShiteiNoAndNendo(jichitaiCode, shiteiNo, nendo);

			// 帳票定義から発行様式と交付条件を取得
			String hakkoYoshiki = getReportsDefText(ReportsConstants.KOFU_HAKKO_YOSHIKI);
			String kofuJoken = getReportsDefText(ReportsConstants.KOFU_JOKEN);
			
			// DTOに設定
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(tokugimu.getShiteiNo());
			dto.setNendo(nendo);
			dto.setTokuName(atena.getName());
			dto.setShisetsuName(tokugimu.getShisetsuName());
			
			// 施設郵便番号を設定
			if (tokugimu.getShisetsuYubinNo() != null && !tokugimu.getShisetsuYubinNo().isEmpty()) {
				dto.setShisetsuYubin("〒" + tokugimu.getShisetsuYubinNo());
			}
			// 施設住所を設定
			if (tokugimu.getShisetsuJusho() != null && !tokugimu.getShisetsuJusho().isEmpty()) {
				dto.setShisetsuJusho(tokugimu.getShisetsuJusho());
			}

			// 奨励金情報設定（数値のみ）
			if (shoreikinOpt.isPresent()) {
				Shoreikin shoreikin = shoreikinOpt.get();
				// 数値を文字列に変換し、nullチェックを実施
				Long kofuZeigaku = shoreikin.getKofuZeigaku();
				Long kofuGaku = shoreikin.getKofuGaku();
			
				// カンマ区切りの文字列に変換
				dto.setNonyugaku(kofuZeigaku != null ? String.format("%,d", kofuZeigaku) : "0");
				dto.setKofugaku(kofuGaku != null ? String.format("%,d", kofuGaku) : "0");
				
				if (shoreikin.getKofuYmd() != null) {
					
					dto.setKofuYmd(shoreikin.getKofuYmd().toString());
				}

				log.debug("奨励金情報設定: 納入額={}, 交付額={}", dto.getNonyugaku(), dto.getKofugaku());
			} else {
				// 奨励金情報が存在しない場合はデフォルト値
				dto.setNonyugaku("0");
				dto.setKofugaku("0");
				log.error("奨励金情報が見つかりません: shiteiNo={}, nendo={}", shiteiNo, nendo);
				return null;
			}

			// 固定値設定
			dto.setCityName(jichitaiName);
			dto.setJorei(jorei);
			dto.setHakkoJorei(jorei);
			dto.setHakkoYoshiki(hakkoYoshiki);
			dto.setKofuJoken(kofuJoken);
			dto.setKoin(koin != null && koin.length > 0 ? koin : null);
			
			// 口座情報を取得
			Optional<FurikomiKoza> furikomiKoza = furikomiKozaRepository.findByJichitaiCdAndShiteiNo(jichitaiCode,
					tokugimu.getShiteiNo());

			// 口座情報が存在する
			if (furikomiKoza.isPresent()) {

				FurikomiKoza koza = furikomiKoza.get();

				// 口座情報を設定
				// 口座情報を設定
				dto.setBankCd(koza.getBankCd() != null && !koza.getBankCd().isEmpty() ? 
						koza.getBankCd() : "-1"); // 金融機関コード
				
				dto.setBankName(koza.getBankName() != null && !koza.getBankName().isEmpty() ?
						processBankName(koza.getBankName()) : "****"); // 金融機関名
				
				dto.setBranchName(koza.getBranchName() != null && !koza.getBranchName().isEmpty() ? 
						processBranchName(koza.getBranchName(), dto) : "****"); // 支店名
				
				dto.setShumoku(koza.getShumoku() != null && !koza.getShumoku().isEmpty() ?
						koza.getShumoku() : "0"); // 預金種目
				
				dto.setFurigana(koza.getMeigi() != null && !koza.getMeigi().isEmpty() ?
						convertToKatakana(koza.getMeigi()) : "****"); // フリガナ
				
				dto.setMeigi(koza.getMeigi() != null && !koza.getMeigi().isEmpty() ?
						koza.getMeigi() : "****"); // 口座名義
				
				dto.setKozaNo(formatKozaNo(koza.getKozaNo())); // 口座番号
			}

			// 口座情報が存在しない場合は **** でマスク
			else {
				dto.setBankCd("-1");
				dto.setBankName("****");
				dto.setBranchName("****");
				dto.setShumoku("0");
				dto.setFurigana("****");
				dto.setMeigi("****");
				dto.setKozaNo(formatKozaNo(null));
			}
					
			log.debug("交付申請書データ取得完了: {}, 年度: {}", dto.getShiteiNo(), dto.getNendo());
			return dto;

		} catch (Exception e) {
			log.error("交付申請書データ取得中にエラーが発生しました - 指定番号: {}, 年度: {}", shiteiNo, nendo, e);
			return null;
		}
	}

	@Override
	public List<KofuKetteiTsuchiShinseiDto> getAllReportData(String nendo) {
		if (nendo == null || nendo.isBlank()) {
			return List.of();
		}
		init();
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		String hakkoYoshiki = getReportsDefText(ReportsConstants.KOFU_HAKKO_YOSHIKI);
		String kofuJoken = getReportsDefText(ReportsConstants.KOFU_JOKEN);

		List<Tokugimu> tokugimuList = tokugimuRepository.findAllByJichitaiCd(jichitaiCode);
		if (tokugimuList.isEmpty()) {
			return List.of();
		}

		List<BigDecimal> atenaNos = tokugimuList.stream().map(Tokugimu::getAtenaNo).distinct().collect(Collectors.toList());
		Map<BigDecimal, Atena> atenaMap = atenaRepository.findByJichitaiCdAndAtenaNoIn(jichitaiCode, atenaNos)
				.stream().collect(Collectors.toMap(Atena::getAtenaNo, a -> a));

		List<String> shiteiNoList = tokugimuList.stream().map(Tokugimu::getShiteiNo).collect(Collectors.toList());
		Map<String, Shoreikin> shoreikinMap = shoreikinRepository
				.findByJichitaiCdAndShiteiNoInAndNendo(jichitaiCode, shiteiNoList, nendo)
				.stream().collect(Collectors.toMap(Shoreikin::getShiteiNo, s -> s));

		List<KofuKetteiTsuchiShinseiDto> result = new ArrayList<>();
		for (Tokugimu tokugimu : tokugimuList) {
			Atena atena = atenaMap.get(tokugimu.getAtenaNo());
			Shoreikin shoreikin = shoreikinMap.get(tokugimu.getShiteiNo());
			if (atena == null || shoreikin == null) {
				log.warn("帳票データスキップ: shiteiNo={}", tokugimu.getShiteiNo());
				continue;
			}

			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(tokugimu.getShiteiNo());
			dto.setNendo(nendo);
			dto.setTokuName(atena.getName());
			dto.setShisetsuName(tokugimu.getShisetsuName());

			// 施設郵便番号を設定
			if (tokugimu.getShisetsuYubinNo() != null && !tokugimu.getShisetsuYubinNo().isEmpty()) {
				dto.setShisetsuYubin("〒" + tokugimu.getShisetsuYubinNo());
			}
			// 施設住所を設定
			if (tokugimu.getShisetsuJusho() != null && !tokugimu.getShisetsuJusho().isEmpty()) {
				dto.setShisetsuJusho(tokugimu.getShisetsuJusho());
			}

			Long kofuZeigaku = shoreikin.getKofuZeigaku();
			Long kofuGaku = shoreikin.getKofuGaku();
			dto.setNonyugaku(kofuZeigaku != null ? String.format("%,d", kofuZeigaku) : "0");
			dto.setKofugaku(kofuGaku != null ? String.format("%,d", kofuGaku) : "0");
			if (shoreikin.getKofuYmd() != null) {
				dto.setKofuYmd(shoreikin.getKofuYmd().toString());
			}

			dto.setCityName(jichitaiName);
			dto.setJorei(jorei);
			dto.setHakkoJorei(jorei);
			dto.setHakkoYoshiki(hakkoYoshiki);
			dto.setKofuJoken(kofuJoken);
			dto.setKoin(koin != null && koin.length > 0 ? koin : null);
			
			// 口座情報を取得
			Optional<FurikomiKoza> furikomiKoza = furikomiKozaRepository.findByJichitaiCdAndShiteiNo(jichitaiCode,
					tokugimu.getShiteiNo());

			// 口座情報が存在する
			if (furikomiKoza.isPresent()) {

				FurikomiKoza koza = furikomiKoza.get();

				// 口座情報を設定
				dto.setBankCd(koza.getBankCd() != null && !koza.getBankCd().isEmpty() ? 
						koza.getBankCd() : "-1"); // 金融機関コード
				
				dto.setBankName(koza.getBankName() != null && !koza.getBankName().isEmpty() ?
						processBankName(koza.getBankName()) : "****"); // 金融機関名
				
				dto.setBranchName(koza.getBranchName() != null && !koza.getBranchName().isEmpty() ? 
						processBranchName(koza.getBranchName(), dto) : "****"); // 支店名
				
				dto.setShumoku(koza.getShumoku() != null && !koza.getShumoku().isEmpty() ?
						koza.getShumoku() : "0"); // 預金種目
				
				dto.setFurigana(koza.getMeigi() != null && !koza.getMeigi().isEmpty() ?
						convertToKatakana(koza.getMeigi()) : "****"); // フリガナ
				
				dto.setMeigi(koza.getMeigi() != null && !koza.getMeigi().isEmpty() ?
						koza.getMeigi() : "****"); // 口座名義
				
				dto.setKozaNo(formatKozaNo(koza.getKozaNo())); // 口座番号
			}

			// 口座情報が存在しない場合は **** でマスク
			else {
				dto.setBankCd("-1");
				dto.setBankName("****");
				dto.setBranchName("****");
				dto.setShumoku("0");
				dto.setFurigana("****");
				dto.setMeigi("****");
				dto.setKozaNo(formatKozaNo(null));
			}
			
			result.add(dto);
		}
		return result;
	}

	/**
	 * 帳票定義からテキストを取得する
	 */
	private String getReportsDefText(String id) {
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		try {
			Optional<ReportsDef> reportsDefOpt = reportsDefRepository
					.findByIdAndJichitaiCd(id, jichitaiCode);

			if (reportsDefOpt.isPresent()) {
				String defText = reportsDefOpt.get().getDefText();
				return defText != null ? defText : "";
			} else {
				log.error("帳票定義が見つかりません: id={}", id);
				return "";
			}
		} catch (Exception e) {
			log.error("帳票定義取得中にエラーが発生しました: id={}", id, e);
			return "";
		}
	}
	
	/**
	 * 漢字からカタカナへの変換
	 */
	private String convertToKatakana(String text) {
		return new Tokenizer().tokenize(text).stream()
				.map(token -> {
					String reading = token.getReading();
					return (reading != null && !reading.equals("*")) ? reading : token.getSurface();
				})
				.collect(Collectors.joining());
	}

	/**
	 * 金融機関名からキーワードを除いた部分を抽出する処理
	 */
	private String processBankName(String bankName) {
		// 判定したいキーワード
		String[] keywords = { "信用金庫", "信用組合", "銀行", "農協" };

		for (String keyword : keywords) {
			int index = bankName.indexOf(keyword);
			if (index != -1) {
				// キーワードが含まれている位置まで切り取る（キーワードは含めない）
				return bankName.substring(0, index);
			}
		}

		// キーワードがいずれも含まれない場合はそのまま返す
		return bankName;
	}
	
	/**
	 * 支店名からキーワードを除いた部分を抽出する処理
	 */
	private String processBranchName(String branchName, KofuKetteiTsuchiShinseiDto dto) {
		// 判定したいキーワード
		String[] keywords = { "本店", "支店", "出張所" };

		for (String keyword : keywords) {
			int index = branchName.indexOf(keyword);
			if (index != -1) {
				// 切り取ったキーワードを帳票フィールドへ設定（丸印の判別用）
				dto.setBranchShubetsu(keyword);
				
				// キーワードが含まれている位置まで切り取る（キーワードは含めない）
				return branchName.substring(0, index);
			}
		}

		// キーワードがいずれも含まれない場合はそのまま返す
		return branchName;
	}
	
	/**
	 * 口座番号を7桁のリストに変換する（7桁未満の場合は * で埋める）
	 */
	private List<String> formatKozaNo(String kozaNoStr) {
		List<String> kozaNoList = new ArrayList<>();

		// 文字列がnullまたは空の場合は全て "*"
		if (kozaNoStr == null || kozaNoStr.isBlank()) {
			for (int i = 0; i < 7; i++) {
				kozaNoList.add("*");
			}
			return kozaNoList;
		}

		// 1文字ずつリストに追加
		List<String> chars = kozaNoStr.codePoints()
				.mapToObj(Character::toChars)
				.map(String::new)
				.collect(Collectors.toList());

		for (String c : chars) {
			// 半角スペース、全角スペース、または空文字の場合は "*" に置き換え
			if (c.equals(" ") || c.isEmpty()) {
				kozaNoList.add("*");
			} else {
				kozaNoList.add(c);
			}
		}

		// 7桁未満なら "*" を追加して7桁にする
		while (kozaNoList.size() < 7) {
			kozaNoList.add("*");
		}

		return kozaNoList;
	}
}
