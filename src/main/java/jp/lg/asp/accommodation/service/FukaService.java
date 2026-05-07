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
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaId;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.FukaZeiritsuTeigaku;
import jp.lg.asp.accommodation.repository.FukaMonthlyDeclarationRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.FukaZeiritsuTeigakuRepository;
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
    private final FukaZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    private final FukaUchiRepository fukaUchiRepository;
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
	
	@Value("${app.jichitai.code}")
    private String configJichitaiCd;

    /**
     * 現在の操作対象となる自治体コードを取得する
     * * TODO: ログイン機能実装後、Session（サーバー側でユーザー状態を保持する仕組み）から取得するように変更する
     */
    private String getCurrentJichitaiCd() {
        // 現在は仮組みのため application.yml から取得（DI：外部から値を注入する仕組み）
        return this.configJichitaiCd;

        /* // ログイン機能実装後のイメージ：
        // Authentication（認証情報を持つオブジェクト）からユーザー情報を抽出し、
        // 紐づく自治体コードを返却する。
        UserPrincipal user = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getJichitaiCd();
        */
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
            // 1. 特別徴収義務者情報の取得
            tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                    .stream()
                    .findFirst()
                    .ifPresent(tokugimu -> {
                        form.setObligorName(tokugimu.getKyokaName());
                        form.setFacilityName(tokugimu.getShisetsuName());
                    });

            // 2. 💡 マスタから税区分を動的に取得してリストを組み立てる
            FukaMonthlyDeclarationDto monthlyDetail = new FukaMonthlyDeclarationDto();
            
            // ymlの自治体コード（jichitaiCd）を使ってマスタを安い順に取得
            List<FukaZeiritsuTeigaku> masterRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);

            // 取得したマスタの行数分だけループして、画面用のDto（TaxDetailDto）を作る
            for (FukaZeiritsuTeigaku master : masterRates) {
                FukaTaxDetailDto detail = new FukaTaxDetailDto();
                
                detail.setZeiritsuSeq(master.getSeq());
                detail.setTeigakuSeq(master.getTeigakuSeq());
                
                // 画面に表示する「〇〇円 ～ 〇〇円」というラベルを生成
                String label;
                if (master.getRyokinEd() != null) {
                    label = String.format("%,d円 ～ %,d円未満", master.getRyokinSt(), master.getRyokinEd() + 1);
                } else {
                    label = String.format("%,d円以上", master.getRyokinSt()); // 上限なしの場合
                }
                
                detail.setLabel(label);
                detail.setTaxRate(master.getZeigaku()); // 自動計算用の単価（100円など）
                
                // リストに追加
                monthlyDetail.getTaxDetails().add(detail);
            }
            
            form.setMonthlyDetail(monthlyDetail);

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
	            FukaMonthlyDeclarationDto monthDto = new FukaMonthlyDeclarationDto();
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
     * 宿泊税情報の保存処理
     * *  @param form 画面からの入力フォーム
     */
    @Transactional(rollbackFor = Exception.class) // 💡 Transaction（トランザクション：一連の処理を一つのまとまりとして扱い、整合性を保つ仕組み）
    public void saveDeclaration(FukaDeclarationForm form) {
        
        // 1. バリデーションチェック（相関チェック）
        validatorService.validateCorrelation(form);

        FukaMonthlyDeclarationDto dto = form.getMonthlyDetail();
        if (dto == null || !StringUtils.hasText(dto.getPaymentYearMonth())) {
            return;
        }

        // 2. 自治体コードの取得
        String currentJichitaiCd = getCurrentJichitaiCd();

        // 3. 親データ (t_fuka) の作成と保存
        Fuka parentFuka = createParentFuka(form, dto, currentJichitaiCd);
        
        // 🔴 親テーブルへの保存を実行（永続性コンテキストへ登録）
        fukaRepository.save(parentFuka);

        // 4. 子データ (t_fuka_uchi) のリスト作成
        List<FukaUchi> uchiList = new ArrayList<>();
        int kazeiKbn = 1; // 課税区分の連番

        for (FukaTaxDetailDto detail : dto.getTaxDetails()) {
            // 宿泊数が 0 または null の行は保存対象外（データ量削減と整合性のため）
            if (detail.getStayCount() == null || detail.getStayCount() == 0) {
                continue;
            }

            FukaUchi uchi = new FukaUchi();
            
            // 🔑 主キー（Composite Key：複数のカラムで構成される主キー）のセット
            uchi.setJichitaiCd(currentJichitaiCd);
            uchi.setShiteiNo(form.getShiteiNo());
            uchi.setNendo(parentFuka.getNendo()); // 親と共通の年度
            uchi.setKibetsu(parentFuka.getKibetsu()); // 親と共通の期別
            uchi.setRno(parentFuka.getRno()); // 親と共通の履歴番号
            uchi.setKazeiKbn(kazeiKbn);
            uchi.setFukaKbn(parentFuka.getFukaKbn());
            uchi.setZeigaku(detail.getTaxAmount());    // 合計税額
            uchi.setCityZeigaku(detail.getTaxAmount()); // 市町村分税額（非NULL制約対策）
            uchi.setKenZeigaku(0L);                     // 都道府県分税額（非NULL制約対策）
            
            // 📄 明細値のセット（マッピング）
            uchi.setZeiritsuSeq(detail.getZeiritsuSeq()); // 税率管理番号
            uchi.setHakusu(detail.getStayCount()); // 宿泊数
            uchi.setZeigaku(detail.getTaxAmount()); // 税額
            uchi.setJichitaiCd(currentJichitaiCd);
         // 賦課区分や自治体コードのセット（既存の処理）
            uchi.setFukaKbn(parentFuka.getFukaKbn());
            uchi.setJichitaiCd(currentJichitaiCd);
            // 💡 マスタの単価（taxRate）を税率としてセット
            uchi.setZeiRitsu(java.math.BigDecimal.valueOf(detail.getTaxRate()));

            // 共通項目の設定
            uchi.setAddDt(java.time.LocalDateTime.now());
            uchi.setAddUser("system");
            uchi.setUpdDt(java.time.LocalDateTime.now());
            uchi.setUpdUser("system");
            uchi.setVersion(1);

            uchiList.add(uchi);
            kazeiKbn++;
        }

        // 5. 🔴 内訳データの一括保存（Batch Insert：複数件を一度の通信で効率的に保存する手法）
        if (!uchiList.isEmpty()) {
            fukaUchiRepository.saveAll(uchiList);
        }
    }
    
    /**
     * 画面入力値から賦課親テーブルのエンティティを生成する
     * 💡 メソッドを分けることで saveDeclaration の可読性を向上させる
     */
    private Fuka createParentFuka(FukaDeclarationForm form, FukaMonthlyDeclarationDto dto, String jichitaiCd) {
        Fuka parentFuka = new Fuka();

        parentFuka.setJichitaiCd(jichitaiCd);
        parentFuka.setShiteiNo(form.getShiteiNo());
        parentFuka.setRno(1); // TODO: 本来は採番ロジックが必要

        String[] ym = dto.getPaymentYearMonth().split("-");
        int year = Integer.parseInt(ym[0]);
        int month = Integer.parseInt(ym[1]);
        
        int nendo = (month >= 4) ? year : year - 1;
        int kibetsu = (month >= 4) ? month - 3 : month + 9;
        
        parentFuka.setNendo(String.valueOf(nendo));
        parentFuka.setKibetsu(kibetsu);

        parentFuka.setTorokuYmd(form.getRegistrationDate() != null ? form.getRegistrationDate() : LocalDate.now());
        parentFuka.setShinkokuYmd(LocalDate.now());
        parentFuka.setFukaKbn("1");
        parentFuka.setHenkoKbn("0");
        parentFuka.setNewFlg("1");
        parentFuka.setDelFlg("0");
        parentFuka.setVersion(1);
        parentFuka.setAddDt(java.time.LocalDateTime.now());
        parentFuka.setAddUser("system");
        parentFuka.setUpdDt(java.time.LocalDateTime.now());
        parentFuka.setUpdUser("system");

        parentFuka.setTaishoYm(dto.getPaymentYearMonth().replace("-", ""));
        parentFuka.setTotalHakusu(dto.getTotalStayCount());
        parentFuka.setTotalZeigaku(dto.getTotalPaymentAmount());
        parentFuka.setMenjoHakusu(dto.getExemptStayCount());
        
        parentFuka.setCityZeigaku(parentFuka.getTotalZeigaku());
        parentFuka.setKenZeigaku(0L);

        return parentFuka;
    }

}