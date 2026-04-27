package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.entity.TaxManager;
import jp.lg.asp.accommodation.entity.TaxManagerId;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.TaxManagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxManagerService {

	private final TaxManagerRepository taxManagerRepository;
	private final TokugimuRepository tokugimuRepository;
	private final CollectorService collectorService;

	// application.yml (app.jichitai.code) から自治体コードを注入
	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	/**
	 * IDからデータを取得し、画面表示用のFormを作成する
	 */
	@Transactional(readOnly = true) // ★必ず readOnly を付ける
	public TaxManagerForm getById(Long id) {
		TaxManagerForm form = new TaxManagerForm();
		form.setCollectorId(id);
		form.setRegistrationDate(LocalDate.now());

		// 指定番号が取れない場合や、リポジトリでエラーが出た場合に備えて
		// メソッド全体ではなく、個別の処理を try-catch で保護し、
		// 失敗しても「空のフォーム」を返すようにします。
		try {
			// 引数の id は「宛名番号（atenaNo）」として渡ってくるため、直接 tokugimu を引きにいきます
			BigDecimal atenaNo = BigDecimal.valueOf(id);

			// 1. 特別徴収義務者（親）の取得
			tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo)
					.ifPresent(tokugimu -> {
						
						// ★ここで確実に画面ヘッダー用の情報をセットします
						form.setObligorName(tokugimu.getKyokaName());
						form.setFacilityName(tokugimu.getShisetsuName());

						// tokugimu から確実な指定番号（shiteiNo）を取り出します
						String shiteiNo = tokugimu.getShiteiNo();

						// 2. 納税管理人の取得（親の処理の中で確実に行う）
						TaxManagerId nokanId = new TaxManagerId(jichitaiCd, shiteiNo, 1);
						taxManagerRepository.findById(nokanId).ifPresent(nokan -> {
							form.setEdit(true);
							form.setRegistrationDate(nokan.getTorokuYmd());

							// ▼▼▼ 追加：宛名マスタ(m_atena)と連携している場合はマスタの最新情報を優先 ▼▼▼
							if (nokan.getAtena() != null) {
								form.setAtenaNo(nokan.getAtenaNo()); // 宛名番号
								form.setManagerName(nokan.getAtena().getName());
								form.setManagerNameKana(nokan.getAtena().getNameKana());
								form.setManagerAddress(nokan.getAtena().getJusho());
								form.setManagerPhone(nokan.getAtena().getTel1()); // 電話番号1を使用
							} else {
								// 紐づいていない場合は t_nokan 直接の値を使用
								form.setManagerName(nokan.getName());
								form.setManagerNameKana(nokan.getNameKana());
								form.setManagerAddress(nokan.getJusho());
								form.setManagerPhone(nokan.getTel());
							}
							// ▲▲▲ 宛名マスタ連携処理ここまで ▲▲▲

							form.setExemptionFlag("1".equals(nokan.getMenjoKbn()));
							form.setExemptionReason(nokan.getMenjoRiyu());
						});
					});

		} catch (Exception e) {
			// エラーをログに出すが、例外は投げない（画面を表示させるため）
			log.warn("データの取得中にエラーが発生しました。新規登録として処理します: {}", e.getMessage());
		}

		return form;
	}
	/**
	 * 保存処理
	 */
	@Transactional
	public void save(Long id, TaxManagerForm form) {
		String shiteiNo = collectorService.getShiteiNoById(id);
		LocalDateTime now = LocalDateTime.now();

		// 1. 既存データを取得（なければ新規作成）
		TaxManagerId nokanId = new TaxManagerId(jichitaiCd, shiteiNo, 1);
		TaxManager entity = taxManagerRepository.findById(nokanId)
				.orElse(new TaxManager());

		// 2. 定義書に基づき値をマッピング
		entity.setJichitaiCd(jichitaiCd);
		entity.setShiteiNo(shiteiNo);
		entity.setRno(1);
		entity.setMenjoKbn(form.isExemptionFlag() ? "1" : "0");
		entity.setTorokuYmd(form.getRegistrationDate());

		// ★必須項目：申告年月日（画面の登録日をセット）
		entity.setShinkokuYmd(form.getRegistrationDate());

		entity.setName(form.getManagerName());
		entity.setNameKana(form.getManagerNameKana());
		entity.setJusho(form.getManagerAddress());
		entity.setTel(form.getManagerPhone());
		entity.setMenjoRiyu(form.getExemptionReason());

		entity.setNewFlg("1");
		entity.setDelFlg("0");

		// 3. 共通項目の手動セット（本来は共通処理で行うのが望ましいですが、一旦ここで）
		if (entity.getAddDt() == null) {
			entity.setAddDt(now);
			entity.setAddUser("system");
		}
		entity.setUpdDt(now);
		entity.setUpdUser("system");
		entity.setVersion(1); // 簡易的に1をセット

		taxManagerRepository.save(entity);
	}
}