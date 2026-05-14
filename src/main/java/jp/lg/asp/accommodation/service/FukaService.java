package jp.lg.asp.accommodation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDaichoListItem;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto.DailyItem;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import jp.lg.asp.accommodation.entity.ChoshuGenbo;
import jp.lg.asp.accommodation.entity.ChoshuGenboId;
import jp.lg.asp.accommodation.entity.ChoshuGenboUchi;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaId;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.FukaZeiritsuTeigaku;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboUchiRepository;
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
    private final ChoshuGenboRepository choshuGenboRepository;
    private final ChoshuGenboUchiRepository choshuGenboUchiRepository;
    private final AtenaRepository atenaRepository;
	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	// 規約に基づく定数
	private static final String STATUS_ALL = "999";
	private static final String STATUS_ZUMI = "1";
	private static final String STATUS_MI = "2";

	
    /**
     * 徴収原簿エンティティから31日分の内訳IDをリストとして抽出する
     */
    private List<Long> collectUchiIndices(ChoshuGenbo genbo) {
        List<Long> indices = new ArrayList<>();
        // 💡 31個のカラムを順番にリストに格納する。
        // 手動で書くのが最も Type Safety（型安全性：コンパイル時に型エラーを検知できる状態）が高いぜ。
        indices.add(genbo.getUchiIdx1());  indices.add(genbo.getUchiIdx2());  indices.add(genbo.getUchiIdx3());
        indices.add(genbo.getUchiIdx4());  indices.add(genbo.getUchiIdx5());  indices.add(genbo.getUchiIdx6());
        indices.add(genbo.getUchiIdx7());  indices.add(genbo.getUchiIdx8());  indices.add(genbo.getUchiIdx9());
        indices.add(genbo.getUchiIdx10()); indices.add(genbo.getUchiIdx11()); indices.add(genbo.getUchiIdx12());
        indices.add(genbo.getUchiIdx13()); indices.add(genbo.getUchiIdx14()); indices.add(genbo.getUchiIdx15());
        indices.add(genbo.getUchiIdx16()); indices.add(genbo.getUchiIdx17()); indices.add(genbo.getUchiIdx18());
        indices.add(genbo.getUchiIdx19()); indices.add(genbo.getUchiIdx20()); indices.add(genbo.getUchiIdx21());
        indices.add(genbo.getUchiIdx22()); indices.add(genbo.getUchiIdx23()); indices.add(genbo.getUchiIdx24());
        indices.add(genbo.getUchiIdx25()); indices.add(genbo.getUchiIdx26()); indices.add(genbo.getUchiIdx27());
        indices.add(genbo.getUchiIdx28()); indices.add(genbo.getUchiIdx29()); indices.add(genbo.getUchiIdx30());
        indices.add(genbo.getUchiIdx31());
        return indices;
    }

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
		// 💡 重複キーを安全に解決するマージ処理を追加したぜ！
		Map<Integer, Fuka> fukaMap = fukaList.stream()
		        .collect(Collectors.toMap(
		                Fuka::getKibetsu, 
		                f -> f, 
		                (existing, replacement) -> existing.getRno() > replacement.getRno() ? existing : replacement
		        ));
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
    
    

/**
     * 【新規登録用】初期表示データの取得（台帳連動・自動判定・明細復元・月計表対応版）
     * @param shiteiNo 指定番号
     * @param paymentMonth 納入年月 (YYYY-MM形式)
     */
    @Transactional(readOnly = true)
    public FukaDeclarationForm getDeclarationFormForRegister(String shiteiNo, String paymentMonth) {
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

            // 2. マスタから税区分リストのベースを組み立てる
            FukaMonthlyDeclarationDto monthlyDetail = new FukaMonthlyDeclarationDto();
            monthlyDetail.setPaymentYearMonth(paymentMonth); 

            List<FukaZeiritsuTeigaku> masterRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);

            for (FukaZeiritsuTeigaku master : masterRates) {
                FukaTaxDetailDto detail = new FukaTaxDetailDto();
                detail.setZeiritsuSeq(master.getSeq());
                detail.setTeigakuSeq(master.getTeigakuSeq());
                
                String label = (master.getRyokinEd() != null) 
                    ? String.format("%,d円 ～ %,d円未満", master.getRyokinSt(), master.getRyokinEd() + 1)
                    : String.format("%,d円以上", master.getRyokinSt());
                
                detail.setLabel(label);
                detail.setTaxRate(master.getZeigaku());
                
                monthlyDetail.getTaxDetails().add(detail);
            }
            form.setMonthlyDetail(monthlyDetail);

            // 💡 🔴 追加：月計表（モーダル）用の DTO を初期化してフォームにセットする
            // これがないと Thymeleaf が th:each を回す際に NullPointerException で落ちるぜ
            jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto tallyDto = new jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto();
            tallyDto.initialize(masterRates.size()); // マスタの件数分（2区分なら2）のリスト枠を作成
            form.setMonthlyTally(tallyDto);

            // 3. 既存データの存在チェックと Hydration（復元）処理
            if (StringUtils.hasText(paymentMonth)) {
                String targetYm = paymentMonth.replace("-", "");
                
                fukaRepository.findFirstByJichitaiCdAndShiteiNoAndTaishoYmOrderByRnoDesc(
                        jichitaiCd, shiteiNo, targetYm).ifPresent(latestFuka -> {
                    
                    form.setModificationCategory("修正"); 
                    
                    monthlyDetail.setExemptStayCount(latestFuka.getMenjoHakusu());
                    monthlyDetail.setTotalStayCount(latestFuka.getTotalHakusu());
                    monthlyDetail.setTotalPaymentAmount(latestFuka.getTotalZeigaku());

                    List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                            latestFuka.getJichitaiCd(), 
                            latestFuka.getShiteiNo(), 
                            latestFuka.getRno(), 
                            latestFuka.getNendo(), 
                            latestFuka.getKibetsu());

                    syncUchiDataToForm(uchiList, monthlyDetail);

                    // 💡 🔴 追加：徴収原簿（月計表）のデータをDBから復元し、先ほど初期化した DTO に流し込む
                    hydrateMonthlyTally(form, latestFuka.getJichitaiCd(), latestFuka);
                });
            }

        } catch (Exception e) {
            log.error("登録用データの取得に失敗しました。指定番号: {}, 年月: {}", shiteiNo, paymentMonth, e);
        }

        return form;
    }

    /**
     * DBから取得した内訳(明細)データを、マスタから生成した画面用のDTOリストに同期させる
     */
    private void syncUchiDataToForm(List<FukaUchi> uchiList, FukaMonthlyDeclarationDto dto) {
        if (uchiList == null || uchiList.isEmpty()) {
            return; // 💡 Early Return（アーリーリターン：無駄な処理を省くための早期離脱）
        }

        for (FukaTaxDetailDto formDetail : dto.getTaxDetails()) {
            // zeiritsuSeq（税率連番）をキーにして、対応する明細行を探し出して値を詰める
            uchiList.stream()
                .filter(uchi -> uchi.getZeiritsuSeq() != null && uchi.getZeiritsuSeq().equals(formDetail.getZeiritsuSeq()))
                .findFirst()
                .ifPresent(matchedUchi -> {
                    formDetail.setStayCount(matchedUchi.getHakusu());
                    formDetail.setTaxAmount(matchedUchi.getZeigaku());
                });
        }
    }
	// ========== 宿泊税情報 登録/編集/照会用メソッド ==========
    /**
     * 【編集・照会共通】表示データの取得ロジック
     * 高い High-level Abstraction（抽象化：細かい実装を隠して処理の流れを見せること）を実現しているぜ。
     */
    @Transactional(readOnly = true)
    public FukaDeclarationForm getDeclarationFormForEdit(String shiteiNo, String nendo, Integer kibetsu) {
        FukaDeclarationForm form = new FukaDeclarationForm();
        form.setShiteiNo(shiteiNo);
        
        // 1. 基本情報のセット（施設名など）
        hydrateFormMetadata(form);

        String jichitaiCd = getCurrentJichitaiCd();

        // 2. 最新履歴(RNO)の申告データを取得
        fukaRepository.findFirstByJichitaiCdAndShiteiNoAndNendoAndKibetsuOrderByRnoDesc(
                jichitaiCd, shiteiNo, nendo, kibetsu)
            .ifPresent(entity -> {
                // 基本項目のセット（登録日、年度、期別、変更理由など）
                form.setRegistrationDate(entity.getShinkokuYmd());
                form.setNendo(entity.getNendo());
                form.setKibetsu(entity.getKibetsu());
                form.setModificationCategory(entity.getHenkoKbn()); 
                form.setModificationReason(entity.getHenkoRiyu());
                
                // 3. 🔴 加算金項目の復元（ハイドレーション）
                hydrateAdditionalFields(entity, form);

                // 4. 🔴 月次明細サマリの復元（未定義エラーの解消箇所だぜ！）
                hydrateMonthlyDetail(entity, form, jichitaiCd);

                // 5. 月計表（モーダル）データの復元
                hydrateMonthlyTally(form, jichitaiCd, entity);
            });

        return form;
    }
	/**
	 * 【照会用】表示データの取得
	 */
	@Transactional(readOnly = true)
    public FukaDeclarationForm getDeclarationFormForView(String shiteiNo, String nendo, Integer kibetsu) {
        // getDeclarationFormForEdit を再利用しているため、
        // 上記のメソッドが修正されていれば自動的に照会も直るぜ！
        FukaDeclarationForm form = getDeclarationFormForEdit(shiteiNo, nendo, kibetsu);
        form.setView(true); 
        return form;
    }
	

	/**
     * 宿泊税情報の保存処理（修正・更生対応版）
     */
    @Transactional
    public void saveDeclaration(FukaDeclarationForm form) {
        String currentJichitaiCd = getCurrentJichitaiCd();
        String category = form.getModificationCategory(); // "1":更生, "2":修正

        // 💡 修正ポイント1: RNO の採番ロジックを分岐
        Integer targetRno;
        if ("2".equals(category)) {
            // 🔵 修正 (Update) の場合
            // 現在の最新 RNO を取得して、それをそのまま使うぜ
            targetRno = getCurrentMaxRno(currentJichitaiCd, form.getShiteiNo(), form.getNendo(), form.getKibetsu());
        } else {
            // 🔴 更生 (Insert) または新規の場合
            // 新しい履歴番号（現在の最大値 + 1）を採番するぜ
            targetRno = determineNextRno(currentJichitaiCd, form.getShiteiNo(), form.getNendo(), form.getKibetsu());
        }

        // 親データ (Fuka) の生成
        FukaMonthlyDeclarationDto dto = form.getMonthlyDetail();
        Fuka parentFuka = createParentFuka(form, dto, currentJichitaiCd);
        parentFuka.setRno(targetRno); 
        
        // 💡 修正ポイント2: 内訳リストの生成
        List<FukaUchi> uchiList = createFukaUchiList(form, parentFuka, currentJichitaiCd);
        
        // 監査項目のセット（登録日・更新日など）
        setAuditFields(parentFuka);
        if (!uchiList.isEmpty()) {
            uchiList.forEach(this::setAuditFields);
        }

        // 💡 修正ポイント3: 永続化 (Persistence) はここだけで OK
        // targetRno が既存なら UPDATE、新規なら INSERT が走るぜ
        fukaRepository.save(parentFuka);
        if (!uchiList.isEmpty()) {
            // 内訳も古い RNO のデータがあれば自動で上書きされるぜ
            fukaUchiRepository.saveAll(uchiList);
        }

        // 徴収原簿の更新
        if (form.getMonthlyTally() != null) {
            saveChoshuGenboDataWithRno(form, parentFuka, currentJichitaiCd, targetRno);
        }
    }

    /**
     * 現在の最新 RNO を取得する
     */
    private Integer getCurrentMaxRno(String jichitaiCd, String shiteiNo, String nendo, Integer kibetsu) {
        return fukaRepository.findFirstByJichitaiCdAndShiteiNoAndNendoAndKibetsuOrderByRnoDesc(
                jichitaiCd, shiteiNo, nendo, kibetsu)
            .map(Fuka::getRno)
            .orElse(1); // 万が一データがない場合は 1 とするぜ
    }
    /**
     * 指定された RNO に紐づく古い内訳データを削除する
     */
    private void deleteExistingDetailsByRno(FukaDeclarationForm form, String jichitaiCd, int rno) {
        fukaUchiRepository.deleteByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
            jichitaiCd, form.getShiteiNo(), rno, form.getNendo(), form.getKibetsu());
    }

    /**
     * 31日分の日別内訳を保存し、徴収原簿（台帳）と紐付ける
     */
    private void saveChoshuGenboData(FukaDeclarationForm form, Fuka parentFuka, String jichitaiCd) {
        Long[] uchiIndices = new Long[31];
        List<DailyItem> dailyItems = form.getMonthlyTally().getDailyItems();

        // 💡 修正箇所1: ループに入る前に「現在の最大値」を1回だけ取得する
        Long currentMaxIdx = choshuGenboUchiRepository.getMaxUchiIdx();

        for (int i = 0; i < dailyItems.size() && i < 31; i++) {
            DailyItem item = dailyItems.get(i);
            if (isDailyDataPresent(item)) {
                
                // 💡 修正箇所2: DBにないシーケンスを呼ぶのをやめ、手動でインクリメントする
                currentMaxIdx++;
                Long nextIdx = currentMaxIdx;
                
                uchiIndices[i] = nextIdx;

                ChoshuGenboUchi uchi = new ChoshuGenboUchi();
                uchi.setUchiIdx(nextIdx); // 手動で採番したIDをセット
                
                // 💡 DTOのリストから値を取り出し、Entityの各項目にセットする
                List<Integer> counts = item.getTaxCategoryCounts();
                if (counts.size() >= 1) uchi.setHakusu1(counts.get(0));
                if (counts.size() >= 2) uchi.setHakusu2(counts.get(1));
                if (counts.size() >= 3) uchi.setHakusu3(counts.get(2));
                
                uchi.setMenjoHakusu(item.getExemptCount());
                setAuditFields(uchi);
                choshuGenboUchiRepository.save(uchi);
            }
        }

        // --- B. 徴収原簿親テーブル (t_choshu_genbo) に保存 ---
        ChoshuGenbo genbo = new ChoshuGenbo();
        // 🔑 賦課データと同じ主キー（自治体コード、指定番号、年度、期別、RNO）をセット
        genbo.setJichitaiCd(jichitaiCd);
        genbo.setShiteiNo(parentFuka.getShiteiNo());
        genbo.setNendo(parentFuka.getNendo());
        genbo.setKibetsu(parentFuka.getKibetsu());
        genbo.setRno(parentFuka.getRno());

        // 💡 31個のカラムに対して、取得した識別子を一つずつマッピングする
        setUchiIndicesToGenbo(genbo, uchiIndices);

        choshuGenboRepository.save(genbo);
    }


    /**
     * Entity に共通項目（監査用項目）をセットするヘルパーメソッド
     * @param entity セット対象のエンティティオブジェクト
     */
    private void setAuditFields(Object entity) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String user = "system"; // 💡 TODO: ログイン機能実装後は認証情報から取得

            // Reflection（リフレクション：プログラム実行中に自身の構造を操作する技術）を使用して
            // 各メソッドの有無を確認しながら値をセットする
            Class<?> clazz = entity.getClass();
            
            // 共通5項目のセット
            invokeMethodIfexists(entity, "setAddDt", LocalDateTime.class, now);
            invokeMethodIfexists(entity, "setAddUser", String.class, user);
            invokeMethodIfexists(entity, "setUpdDt", LocalDateTime.class, now);
            invokeMethodIfexists(entity, "setUpdUser", String.class, user);
            invokeMethodIfexists(entity, "setVersion", Integer.class, 1);
            
        } catch (Exception e) {
            log.warn("共通項目のセット中にエラーが発生しました（一部項目がスキップされた可能性があります）: {}", e.getMessage());
        }
    }

    /**
     * メソッドが存在する場合のみ実行する内部ユーティリティ
     */
    private void invokeMethodIfexists(Object obj, String methodName, Class<?> paramType, Object value) {
        try {
            obj.getClass().getMethod(methodName, paramType).invoke(obj, value);
        } catch (NoSuchMethodException e) {
            // メソッドがない場合は何もしない（正常な挙動）
        } catch (Exception e) {
            log.error("メソッド実行エラー: {}", methodName, e);
        }
    }
    /**
     * 1日分のデータに入力があるか判定する
     * 💡 全ての宿泊数項目が 0 または Null の場合は false を返す
     */
    private boolean isDailyDataPresent(DailyItem item) {
        if (item == null) return false;
        
        // 💡 全ての税区分の合計をストリームで集計する
        int taxSum = item.getTaxCategoryCounts().stream()
                         .filter(java.util.Objects::nonNull)
                         .mapToInt(Integer::intValue)
                         .sum();
        
        int exempt = (item.getExemptCount() != null) ? item.getExemptCount() : 0;
        
        return (taxSum + exempt) > 0;
    }
    
    /**
     * 内訳IDの配列を徴収原簿エンティティの31個のカラムにマッピングする
     * 💡 引数の indices は [0]〜[30] に 1日〜31日のIDが入っている前提だ
     */
    private void setUchiIndicesToGenbo(ChoshuGenbo genbo, Long[] indices) {
        genbo.setUchiIdx1(indices[0]);   genbo.setUchiIdx2(indices[1]);   genbo.setUchiIdx3(indices[2]);
        genbo.setUchiIdx4(indices[3]);   genbo.setUchiIdx5(indices[4]);   genbo.setUchiIdx6(indices[5]);
        genbo.setUchiIdx7(indices[6]);   genbo.setUchiIdx8(indices[7]);   genbo.setUchiIdx9(indices[8]);
        genbo.setUchiIdx10(indices[9]);  genbo.setUchiIdx11(indices[10]); genbo.setUchiIdx12(indices[11]);
        genbo.setUchiIdx13(indices[12]); genbo.setUchiIdx14(indices[13]); genbo.setUchiIdx15(indices[14]);
        genbo.setUchiIdx16(indices[15]); genbo.setUchiIdx17(indices[16]); genbo.setUchiIdx18(indices[17]);
        genbo.setUchiIdx19(indices[18]); genbo.setUchiIdx20(indices[19]); genbo.setUchiIdx21(indices[20]);
        genbo.setUchiIdx22(indices[21]); genbo.setUchiIdx23(indices[22]); genbo.setUchiIdx24(indices[23]);
        genbo.setUchiIdx25(indices[24]); genbo.setUchiIdx26(indices[25]); genbo.setUchiIdx27(indices[26]);
        genbo.setUchiIdx28(indices[27]); genbo.setUchiIdx29(indices[28]); genbo.setUchiIdx30(indices[29]);
        genbo.setUchiIdx31(indices[30]);
    }
    /**
     * 画面入力から賦課内訳（t_fuka_uchi）のリストを生成する
     * 💡 ロジックを分離することで saveDeclaration の Readability（可読性）を高める
     */
    private List<FukaUchi> createFukaUchiList(FukaDeclarationForm form, Fuka parentFuka, String currentJichitaiCd) {
        List<FukaUchi> uchiList = new ArrayList<>();
        int kazeiKbn = 1;
        FukaMonthlyDeclarationDto dto = form.getMonthlyDetail();

        for (FukaTaxDetailDto detail : dto.getTaxDetails()) {
            if (detail.getStayCount() == null || detail.getStayCount() == 0) continue;

            FukaUchi uchi = new FukaUchi();
            uchi.setJichitaiCd(currentJichitaiCd);
            uchi.setShiteiNo(form.getShiteiNo());
            uchi.setNendo(parentFuka.getNendo());
            uchi.setKibetsu(parentFuka.getKibetsu());
            uchi.setRno(parentFuka.getRno());
            uchi.setKazeiKbn(kazeiKbn++);
            uchi.setFukaKbn(parentFuka.getFukaKbn());
            uchi.setZeiritsuSeq(detail.getZeiritsuSeq());
            uchi.setHakusu(detail.getStayCount());
            uchi.setZeigaku(detail.getTaxAmount());
            uchi.setCityZeigaku(detail.getTaxAmount());
            uchi.setKenZeigaku(0L);
            uchi.setZeiRitsu(java.math.BigDecimal.valueOf(detail.getTaxRate()));
            
            setAuditFields(uchi); // 💡 共通項目のセット
            uchiList.add(uchi);
        }
        return uchiList;
    }
    
    
    /**
     * 徴収原簿から月計表データを復元し、DTOにセットする
     */
    private void hydrateMonthlyTally(FukaDeclarationForm form, String jichitaiCd, Fuka parentFuka) {
        // 1. 親テーブル (t_choshu_genbo) を取得
        ChoshuGenboId genboId = new ChoshuGenboId(
            jichitaiCd, parentFuka.getShiteiNo(), parentFuka.getRno(), 
            parentFuka.getNendo(), parentFuka.getKibetsu());
        
        Optional<ChoshuGenbo> genboOpt = choshuGenboRepository.findById(genboId);
        if (genboOpt.isEmpty()) return;

        ChoshuGenbo genbo = genboOpt.get();
        
        // 2. 31日分の uchi_idx をリストにまとめる
        List<Long> uchiIndices = collectUchiIndices(genbo);

        // 3. 内訳テーブル (t_choshu_genbo_uchi) を一括取得（Batch Fetching）
        // 💡 1回ずつ select する N+1問題を回避するぜ
        List<ChoshuGenboUchi> uchiList = choshuGenboUchiRepository.findAllById(
            uchiIndices.stream().filter(java.util.Objects::nonNull).collect(Collectors.toList())
        );
        
        // 効率化のために Map 化しておく（Key: uchiIdx, Value: Entity）
        Map<Long, ChoshuGenboUchi> uchiMap = uchiList.stream()
            .collect(Collectors.toMap(ChoshuGenboUchi::getUchiIdx, u -> u));

        // 4. DTO への詰め替え
        int categoryCount = form.getMonthlyDetail().getTaxDetails().size(); // 現在のマスタ数
        form.getMonthlyTally().initialize(categoryCount);

        for (int i = 0; i < 31; i++) {
            Long idx = uchiIndices.get(i);
            if (idx != null && uchiMap.containsKey(idx)) {
                ChoshuGenboUchi uchi = uchiMap.get(idx);
                DailyItem dDto = form.getMonthlyTally().getDailyItems().get(i);
                
                // 💡 Reflection を使って hakusu1, 2, 3... を動的に取得
                for (int j = 1; j <= categoryCount; j++) {
                    Integer value = getHakusuValue(uchi, j);
                    dDto.getTaxCategoryCounts().set(j - 1, value);
                }
                dDto.setExemptCount(uchi.getMenjoHakusu());
            }
        }
    }
    /**
     * Entityから動的に hakusuN の値を取得する
     */
    private Integer getHakusuValue(ChoshuGenboUchi uchi, int index) {
        try {
            String methodName = "getHakusu" + index;
            Object val = uchi.getClass().getMethod(methodName).invoke(uchi);
            return (val != null) ? (Integer) val : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 指定された年月に該当する申告データが既に存在するか（済か）判定する
     */
    public boolean isAlreadyRegistered(String shiteiNo, String paymentMonth) {
        if (!StringUtils.hasText(paymentMonth)) return false;
        String targetYm = paymentMonth.replace("-", "");
        return fukaRepository.findFirstByJichitaiCdAndShiteiNoAndTaishoYmOrderByRnoDesc(jichitaiCd, shiteiNo, targetYm).isPresent();
    }

    /**
     * 指定された年度・期別に該当する申告データが存在するか（済か）判定する
     */
    public boolean isAlreadyRegisteredByKibetsu(String shiteiNo, String nendo, Integer kibetsu) {
        FukaId fukaId = new FukaId(jichitaiCd, shiteiNo, 1, nendo, kibetsu);
        return fukaRepository.findById(fukaId).isPresent();
    }
    
    
    /**
     * 画面入力値から賦課親テーブルのエンティティを生成する
     */
    private Fuka createParentFuka(FukaDeclarationForm form, jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto dto, String jichitaiCd) {
        Fuka parentFuka = new Fuka();
        parentFuka.setJichitaiCd(jichitaiCd);
        parentFuka.setShiteiNo(form.getShiteiNo());

        String[] ym = dto.getPaymentYearMonth().split("-");
        int year = Integer.parseInt(ym[0]);
        int month = Integer.parseInt(ym[1]);
        
        int nendo = (month >= 4) ? year : year - 1;
        int kibetsu = (month >= 4) ? month - 3 : month + 9;
        
        parentFuka.setNendo(String.valueOf(nendo));
        parentFuka.setKibetsu(kibetsu);

        parentFuka.setTorokuYmd(form.getRegistrationDate() != null ? form.getRegistrationDate() : java.time.LocalDate.now());
        parentFuka.setShinkokuYmd(java.time.LocalDate.now());
        // 💡 変更区分を画面入力からセットする（空なら "1"：新規とする）
        parentFuka.setFukaKbn(org.springframework.util.StringUtils.hasText(form.getModificationCategory()) ? form.getModificationCategory() : "1");
        parentFuka.setHenkoKbn(mapModificationCategory(form.getModificationCategory()));
        parentFuka.setHenkoRiyu(form.getModificationReason());
        
        parentFuka.setNewFlg("1");
        parentFuka.setDelFlg("0");
        parentFuka.setVersion(1);
        parentFuka.setTaishoYm(dto.getPaymentYearMonth().replace("-", ""));
        parentFuka.setTotalHakusu(dto.getTotalStayCount());
        parentFuka.setTotalZeigaku(dto.getTotalPaymentAmount());
        parentFuka.setMenjoHakusu(dto.getExemptStayCount());
        parentFuka.setCityZeigaku(parentFuka.getTotalZeigaku());
        parentFuka.setKenZeigaku(0L);
        
        mapAdditionalFields(form, parentFuka);

        return parentFuka;
    }

    /**
     * 徴収原簿（月計表）のデータを特定のRNOで保存する
     */
    private void saveChoshuGenboDataWithRno(FukaDeclarationForm form, Fuka parentFuka, String jichitaiCd, int targetRno) {
        // 💡 RNOをセットした状態でIDオブジェクトを生成
        jp.lg.asp.accommodation.entity.ChoshuGenboId genboId = new jp.lg.asp.accommodation.entity.ChoshuGenboId(
            jichitaiCd, parentFuka.getShiteiNo(), targetRno, 
            parentFuka.getNendo(), parentFuka.getKibetsu());
        
        java.util.Optional<jp.lg.asp.accommodation.entity.ChoshuGenbo> existingGenboOpt = choshuGenboRepository.findById(genboId);
        
        Long[] uchiIndices = new Long[31];
        if (existingGenboOpt.isPresent()) {
            java.util.List<Long> currentIndices = collectUchiIndices(existingGenboOpt.get());
            for (int i = 0; i < 31; i++) uchiIndices[i] = currentIndices.get(i);
        }

        java.util.List<jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto.DailyItem> dailyItems = form.getMonthlyTally().getDailyItems();

        // 💡 修正箇所1: ループに入る前に「現在の最大値」を1回だけ取得する
        // ※Repositoryに getMaxUchiIdx() メソッドを追加しておく必要があるぜ！
        Long currentMaxIdx = choshuGenboUchiRepository.getMaxUchiIdx();

        for (int i = 0; i < dailyItems.size() && i < 31; i++) {
            jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto.DailyItem item = dailyItems.get(i);
            if (isDailyDataPresent(item)) {
                
                // 💡 修正箇所2: 自動採番（getNextUchiIdx）をやめ、手動で MAX値 をカウントアップする
                Long targetIdx = uchiIndices[i];
                if (targetIdx == null) {
                    currentMaxIdx++;          // 最大値をインクリメント
                    targetIdx = currentMaxIdx; // 新しいIDとして採用
                }
                uchiIndices[i] = targetIdx;

                jp.lg.asp.accommodation.entity.ChoshuGenboUchi uchi = new jp.lg.asp.accommodation.entity.ChoshuGenboUchi();
                uchi.setUchiIdx(targetIdx); // 手動で振ったIDをセット
                
                java.util.List<Integer> counts = item.getTaxCategoryCounts();
                if (counts.size() >= 1) uchi.setHakusu1(counts.get(0));
                if (counts.size() >= 2) uchi.setHakusu2(counts.get(1));
                if (counts.size() >= 3) uchi.setHakusu3(counts.get(2));
                
                uchi.setMenjoHakusu(item.getExemptCount());
                setAuditFields(uchi);
                choshuGenboUchiRepository.save(uchi);
            }
        }
        
        jp.lg.asp.accommodation.entity.ChoshuGenbo genbo = existingGenboOpt.orElse(new jp.lg.asp.accommodation.entity.ChoshuGenbo());
        genbo.setJichitaiCd(jichitaiCd);
        genbo.setShiteiNo(parentFuka.getShiteiNo());
        genbo.setNendo(parentFuka.getNendo());
        genbo.setKibetsu(parentFuka.getKibetsu());
        genbo.setRno(targetRno); // 💡 ここで最新のRNOを紐付ける
        setUchiIndicesToGenbo(genbo, uchiIndices);
        setAuditFields(genbo);
        choshuGenboRepository.save(genbo);
    }
    
    /**
     * 画面表示に必要なメタデータをフォームに再セット（ハイドレーション）する
     */
    /**
     * 画面表示に必要なメタデータをフォームに再セット（ハイドレーション）する
     */
    public void hydrateFormMetadata(FukaDeclarationForm form) {
        if (form.getShiteiNo() == null) {
            return;
        }

        String jichitaiCd = getCurrentJichitaiCd();
        
        // 💡 修正ポイント：登録画面（Register）と完全に同じ取得ロジックに統一するぜ！
        tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, form.getShiteiNo())
            .stream()
            .findFirst()
            .ifPresent(tokugimu -> {
                // 施設名のセット
                form.setFacilityName(tokugimu.getShisetsuName());
                // 特別徴収義務者名のセット（宛名テーブル検索をやめ、直接 kyoka_name を使う）
                form.setObligorName(tokugimu.getKyokaName());
            });
    }
    
    
    /**
     * 💡 画面の「更生/修正」文字列を DB 用の 1 文字コードに変換する
     */
    private String mapModificationCategory(String category) {
        if (!org.springframework.util.StringUtils.hasText(category)) {
            return "0"; // デフォルト（新規申告）
        }
        return switch (category) {
            case "更生" -> "1";
            case "修正" -> "2";
            default -> "0";
        };
    }
    
    /**
     * 💡 次に使用すべき履歴番号 (RNO) を決定するぜ
     */
    private Integer determineNextRno(String jichitaiCd, String shiteiNo, String nendo, Integer kibetsu) {
        // 💡 「最新の1件」を取得し、存在すればその RNO に +1、なければ 1 を返す
        return fukaRepository.findFirstByJichitaiCdAndShiteiNoAndNendoAndKibetsuOrderByRnoDesc(
                    jichitaiCd, shiteiNo, nendo, kibetsu)
                .map(Fuka::getRno)   // Entity から RNO だけを抽出 (Mapping)
                .map(rno -> rno + 1) // 既存があればプラス1
                .orElse(1);          // 存在しなければ 1 (Default Value)
    }

    /**
     * フォームからエンティティへ加算項目をマッピングする。
     * BigDecimal 型への変換を行い、Data Integrity（データの一貫性）を保証するぜ。
     */
    private void mapAdditionalFields(FukaDeclarationForm form, Fuka entity) {
        // 加算区分
        entity.setKasanKbn(form.getAdditionalCategory());
        
        // 加算割合 (String -> BigDecimal)
        if (org.springframework.util.StringUtils.hasText(form.getAdditionalRate())) {
            try {
                entity.setKasanRitsu(new java.math.BigDecimal(form.getAdditionalRate()));
            } catch (NumberFormatException e) {
                log.warn("加算割合の数値変換に失敗しました: {}", form.getAdditionalRate());
                entity.setKasanRitsu(null);
            }
        } else {
            entity.setKasanRitsu(null);
        }
        
        // 加算金額
        entity.setKasanGaku(form.getAdditionalAmount());
        
        // 納期限 (nokigen)
        entity.setNokigen(form.getAdditionalDueDate());
    }

    // ---------------------------------------------------------------------


    // 💡 既存の updateParentFuka メソッドも同様に修正だ
    private void updateParentFuka(Fuka existing, FukaDeclarationForm form) {
        // ... 既存のセット処理 ...
        existing.setTotalZeigaku(form.getMonthlyDetail().getTotalPaymentAmount());
        
        // 💡 修正箇所：加算項目をマッピング
        mapAdditionalFields(form, existing);
        
        setAuditFields(existing);
        fukaRepository.save(existing);
    }
    
    /**
     * 💡 修正箇所：月次明細サマリ（総泊数、免除、税額合計）を Form に復元する。
     * 専門的な値を DTO へ Mapping（マッピング：値を詰め替えること）するぜ。
     */
    private void hydrateMonthlyDetail(Fuka entity, FukaDeclarationForm form, String jichitaiCd) {
        FukaMonthlyDeclarationDto monthDto = new FukaMonthlyDeclarationDto();
        
        // 納入年月 (YYYY-MM) の動的算出
        int calendarMonth = (entity.getKibetsu() <= 9) ? entity.getKibetsu() + 3 : entity.getKibetsu() - 9;
        int calendarYear = Integer.parseInt(entity.getNendo());
        if (calendarMonth < 4) calendarYear++; 
        monthDto.setPaymentYearMonth(String.format("%s-%02d", calendarYear, calendarMonth));
        
        monthDto.setExemptStayCount(entity.getMenjoHakusu());
        monthDto.setTotalStayCount(entity.getTotalHakusu());
        monthDto.setTotalPaymentAmount(entity.getTotalZeigaku());

        // 税率マスタ情報の紐付け
        List<FukaZeiritsuTeigaku> masterRates = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);
        for (FukaZeiritsuTeigaku m : masterRates) {
            FukaTaxDetailDto d = new FukaTaxDetailDto();
            d.setZeiritsuSeq(m.getSeq());
            d.setTeigakuSeq(m.getTeigakuSeq());
            d.setTaxRate(m.getZeigaku());
            d.setLabel(m.getRyokinEd() != null ? 
                String.format("%,d円 ～ %,d円未満", m.getRyokinSt(), m.getRyokinEd() + 1) : 
                String.format("%,d円以上", m.getRyokinSt()));
            monthDto.getTaxDetails().add(d);
        }
        form.setMonthlyDetail(monthDto);

        // 内訳データ(FukaUchi)の同期
        List<FukaUchi> uchiList = fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                jichitaiCd, form.getShiteiNo(), entity.getRno(), entity.getNendo(), entity.getKibetsu());
        syncUchiDataToForm(uchiList, monthDto);
    }

    /**
     * 💡 加算金項目の復元処理。
     * Null-Safe（ヌルセーフ：値が Null でも落ちないように安全に考慮すること）に実装しているぜ。
     */
    private void hydrateAdditionalFields(Fuka entity, FukaDeclarationForm form) {
        form.setAdditionalCategory(entity.getKasanKbn());
        if (entity.getKasanRitsu() != null) {
            form.setAdditionalRate(entity.getKasanRitsu().toString());
        }
        form.setAdditionalAmount(entity.getKasanGaku());
        form.setAdditionalDueDate(entity.getNokigen());
    }
    

}