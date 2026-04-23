package jp.lg.asp.accommodation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.entity.TaxManager;
import jp.lg.asp.accommodation.entity.TaxManagerId;
import jp.lg.asp.accommodation.repository.TaxManagerRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
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
            String shiteiNo = collectorService.getShiteiNoById(id);

            // 1. 特別徴収義務者の取得
            tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .ifPresent(tokugimu -> {
                    form.setObligorName(tokugimu.getKyokaName());
                    form.setFacilityName(tokugimu.getShisetsuName());
                });

            // 2. 納税管理人の取得
            TaxManagerId nokanId = new TaxManagerId(jichitaiCd, shiteiNo, 1);
            taxManagerRepository.findById(nokanId).ifPresent(nokan -> {
                form.setEdit(true);
                
                // --- 以下を修正 ---
                // getRegistrationDate() -> getTorokuYmd()
                form.setRegistrationDate(nokan.getTorokuYmd());
                
                form.setManagerName(nokan.getName());
                form.setManagerNameKana(nokan.getNameKana());
                
                // getAddress() -> getJusho()
                form.setManagerAddress(nokan.getJusho());
                
                // getPhone() -> getTel()
                form.setManagerPhone(nokan.getTel());
                
                // getExemptionKbn() -> getMenjoKbn()
                form.setExemptionFlag("1".equals(nokan.getMenjoKbn()));
                
                // getExemptionReason() -> getMenjoRiyu()
                form.setExemptionReason(nokan.getMenjoRiyu());
                // ------------------
            });
        } catch (Exception e) {
            // エラーをログに出すが、例外は投げない（画面を表示させるため）
            log.warn("データの取得中にエラーが発生しました。新規登録として処理します: {}", e.getMessage());
        }

        return form;
    }
    /**
     * 保存処理（リポジトリを使用）
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