package jp.lg.asp.accommodation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDaichoListItem;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.MonthlyDeclarationDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaId;
import jp.lg.asp.accommodation.entity.FukaMonthlyDeclaration;
import jp.lg.asp.accommodation.repository.FukaMonthlyDeclarationRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税納入（賦課）に関するビジネスロジックを担当するサービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FukaService {

	private final FukaRepository fukaRepository;
	private final TokugimuRepository tokugimuRepository;
	
	private final FukaMonthlyDeclarationRepository repository;
    private final FukaValidatorService validatorService;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	// 規約に基づく定数
	private static final String STATUS_ALL = "999";
	private static final String STATUS_ZUMI = "1";
	private static final String STATUS_MI = "2";

	/**
	 * 納入金額管理台帳のデータを取得する
	 */
	@Transactional(readOnly = true)
	public FukaDaichoForm getDaichoData(String shiteiNo, String nendo, String status) {
		FukaDaichoForm form = new FukaDaichoForm();
		form.setShiteiNo(shiteiNo);
		form.setNendo(nendo);
		form.setStatus(status != null ? status : STATUS_ALL);

		// 1. ヘッダー情報（特別徴収義務者名称）の取得
		tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.stream()
				.findFirst()
				.ifPresent(tokugimu -> form.setObligorName(tokugimu.getKyokaName()));

		// 仮設定：今回は「毎月申告（全12期）」として処理
		int maxKibetsu = 12;

		// 2. DBから実データを取得し、期別(kibetsu)をキーにしたMapに変換
		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(jichitaiCd, shiteiNo, nendo);
		Map<Integer, Fuka> fukaMap = fukaList.stream()
				.collect(Collectors.toMap(Fuka::getKibetsu, f -> f));

		// 3. 1期〜12期までのリストを生成
		List<FukaDaichoListItem> items = new ArrayList<>();

		for (int i = 1; i <= maxKibetsu; i++) {
			FukaDaichoListItem item = new FukaDaichoListItem();
			item.setNendo(nendo);
			item.setKibetsu(i);

			int displayMonth = (i + 3) > 12 ? (i + 3) - 12 : (i + 3);
			item.setDisplayNengetsu(displayMonth + "月");

			int nokiMonth = displayMonth == 12 ? 1 : displayMonth + 1;
			item.setDisplayNoki(nokiMonth + "月末");

			if (fukaMap.containsKey(i)) {
				Fuka dbData = fukaMap.get(i);
				item.setAmount(dbData.getTotalZeigaku());
				item.setTotalZeigaku(dbData.getTotalZeigaku());
				item.setStatus("済");

				int year = Integer.parseInt(nendo);
				if (displayMonth < 4) year++;
				item.setTargetYearMonth(LocalDate.of(year, displayMonth, 1));

				item.setShinkokuYmd(dbData.getShinkokuYmd());
				item.setShinkokuZumi(true);
			} else {
				item.setAmount(0L);
				item.setTotalZeigaku(0L);
				item.setStatus("未");

				int year = Integer.parseInt(nendo);
				if (displayMonth < 4) year++;
				item.setTargetYearMonth(LocalDate.of(year, displayMonth, 1));

				item.setShinkokuZumi(false);
			}

			// 4. 画面のステータス絞り込みを適用
			if (STATUS_ZUMI.equals(form.getStatus()) && !item.isShinkokuZumi()) continue;
			if (STATUS_MI.equals(form.getStatus()) && item.isShinkokuZumi()) continue;

			items.add(item);
		}

		form.setItems(items);
		return form;
	}

	// ========== 宿泊税情報 登録/編集/照会用メソッド ==========

	/**
	 * 【新規登録用】初期表示データの取得
	 */
	@Transactional(readOnly = true)
	public FukaDeclarationForm getDeclarationFormForRegister(String shiteiNo) {
		FukaDeclarationForm form = new FukaDeclarationForm();
		form.setShiteiNo(shiteiNo);
		form.setRegistrationDate(LocalDate.now());

		try {
			// 特別徴収義務者情報の取得
			tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
					.stream()
					.findFirst()
					.ifPresent(tokugimu -> {
						form.setObligorName(tokugimu.getKyokaName());
						form.setFacilityName(tokugimu.getShisetsuName());
					});
		} catch (Exception e) {
			log.error("新規登録用データの取得に失敗しました。指定番号: {}", shiteiNo, e);
		}

		return form;
	}

	// ========== 宿泊税情報 登録/編集/照会用メソッド ==========

	/**
	 * 【編集用】表示データの取得
	 */
	@Transactional(readOnly = true)
	public FukaDeclarationForm getDeclarationFormForEdit(String shiteiNo, String nendo, Integer kibetsu) {
	    FukaDeclarationForm form = new FukaDeclarationForm();
	    form.setEdit(true);

	    try {
	        FukaId fukaId = new FukaId(jichitaiCd, shiteiNo, 1, nendo, kibetsu);

	        fukaRepository.findById(fukaId).ifPresent(entity -> {
	            
	            // --- ① 親フォーム（FukaDeclarationForm）へのセット ---
	            form.setShiteiNo(entity.getShiteiNo());
	            // 登録日 (type="date") 用: "yyyy-MM-dd" 形式にする
	            form.setRegistrationDate(entity.getShinkokuYmd()); 
	            
	            // 特別徴収義務者情報の取得（任意：表示用）
	            tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
	                .stream()
	                .findFirst()
	                .ifPresent(tokugimu -> {
	                    form.setObligorName(tokugimu.getKyokaName());
	                    form.setFacilityName(tokugimu.getShisetsuName());
	                });

	            // --- ② 1ヶ月分のDTO（MonthlyDeclarationDto）の作成とセット ---
	            MonthlyDeclarationDto monthDto = new MonthlyDeclarationDto();
	            String formattedMonth = String.format("%s-%02d", entity.getNendo(), entity.getKibetsu());
	            monthDto.setPaymentYearMonth(formattedMonth);
	            
	            // （DBに保存されている合計値などをセットする）
	            monthDto.setTotalStayCount(entity.getTotalHakusu());
	            monthDto.setTotalPaymentAmount(entity.getTotalZeigaku());
	            monthDto.setExemptStayCount(entity.getMenjoHakusu());
	            
	            form.setMonthlyDetail(monthDto);

	        });
	    } catch (Exception e) {
	        log.error("編集用データの取得に失敗しました。指定番号: {}, 年度: {}, 期別: {}", shiteiNo, nendo, kibetsu, e);
	    }

	    return form;
	}
	/**
	 * 【照会用】表示データの取得
	 */
	@Transactional(readOnly = true)
	public FukaDeclarationForm getDeclarationFormForView(String shiteiNo, String nendo, Integer kibetsu) {
	    FukaDeclarationForm form = getDeclarationFormForEdit(shiteiNo, nendo, kibetsu);
	    form.setView(true); 
	    return form;
	}
	
	/**
     * 宿泊税情報の保存処理（新規・更新）
     */
	@Transactional
    public void saveDeclaration(FukaDeclarationForm form) {
        validatorService.validateCorrelation(form);

        // ❌ forループはもう不要！
        // ⭕ 修正後：単一のオブジェクトを取り出してそのまま処理
        MonthlyDeclarationDto dto = form.getMonthlyDetail();

        if (dto != null && StringUtils.hasText(dto.getPaymentYearMonth())) {
            
            // ① 親データ（t_fuka）の作成と保存
            Fuka parentFuka = new Fuka();
            // ... (parentFuka.set... の処理はそのまま) ...
            
            // 合計値の計算もループ不要で直接セットできる
            parentFuka.setTotalHakusu(dto.getTotalStayCount() != null ? dto.getTotalStayCount() : 0);
            parentFuka.setTotalZeigaku(dto.getTotalPaymentAmount() != null ? dto.getTotalPaymentAmount() : 0);
            parentFuka.setMenjoHakusu(dto.getExemptStayCount() != null ? dto.getExemptStayCount() : 0);
            parentFuka.setTaishoYm(dto.getPaymentYearMonth().replace("-", "")); // "2026-05" -> "202605"

            fukaRepository.save(parentFuka);
            
            // ② 子データ（t_fuka_uchi）の作成と保存のロジックへ続く...
        }
    }

    /**
     * DTOからEntityへの詰め替えを行うプライベートメソッド
     */
    private FukaMonthlyDeclaration convertToEntity(String shiteiNo, MonthlyDeclarationDto dto) {
        FukaMonthlyDeclaration entity = new FukaMonthlyDeclaration();
        
        // 誰の申告かをセット
        entity.setShiteiNo(shiteiNo);
        
        // 月ごとのデータをセット
        entity.setPaymentYearMonth(dto.getPaymentYearMonth());
        
        entity.setStayCount1(dto.getStayCount1());
        entity.setTaxAmount1(dto.getTaxAmount1());
        
        entity.setStayCount2(dto.getStayCount2());
        entity.setTaxAmount2(dto.getTaxAmount2());
        
        entity.setStayCount3(dto.getStayCount3());
        entity.setTaxAmount3(dto.getTaxAmount3());
        
        entity.setExemptStayCount(dto.getExemptStayCount());
        entity.setTotalStayCount(dto.getTotalStayCount());
        entity.setTotalPaymentAmount(dto.getTotalPaymentAmount());
        
        return entity;
    }

}