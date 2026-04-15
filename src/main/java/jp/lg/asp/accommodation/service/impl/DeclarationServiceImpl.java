package jp.lg.asp.accommodation.service.impl;

import jp.lg.asp.accommodation.dto.ConsolidatedDeclarationForm;
import jp.lg.asp.accommodation.dto.PaymentLedgerSearchForm;
import jp.lg.asp.accommodation.dto.PaymentRecordDto;
import jp.lg.asp.accommodation.dto.TaxCycleInfoDto;
import jp.lg.asp.accommodation.dto.TaxDeclarationForm;
import jp.lg.asp.accommodation.service.DeclarationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeclarationServiceImpl implements DeclarationService {

    // TODO: DB実装後は以下のRepositoryを注入して差し替える
    // private final AccommodationTaxDeclarationRepository declarationRepository;
    // private final TaxCycleRepository taxCycleRepository;

    private static final List<PaymentRecordDto> DUMMY_RECORDS = List.of(
        new PaymentRecordDto("PR001", YearMonth.of(2024, 8), 2_150_300, "未"),
        new PaymentRecordDto("PR002", YearMonth.of(2024, 7), 1_680_750, "未"),
        new PaymentRecordDto("PR003", YearMonth.of(2024, 6), 1_450_200, "未"),
        new PaymentRecordDto("PR004", YearMonth.of(2024, 5),   980_500, "済"),
        new PaymentRecordDto("PR005", YearMonth.of(2024, 4), 1_250_000, "済"),
        new PaymentRecordDto("PR006", YearMonth.of(2024, 3), 1_100_000, "済"),
        new PaymentRecordDto("PR007", YearMonth.of(2024, 2),   870_000, "済"),
        new PaymentRecordDto("PR008", YearMonth.of(2024, 1), 1_320_000, "済")
    );

    @Override
    public String getObligorName(String obligorId) {
        // TODO: collectorRepository.findById(obligorId).getName() に差し替え
        return switch (obligorId != null ? obligorId : "") {
            case "1" -> "グランドホテル東京";
            case "2" -> "温泉旅館やまと";
            case "3" -> "シティイン新宿";
            default  -> "株式会社グランドホテル東京（ID:" + obligorId + "）";
        };
    }

    @Override
    public TaxCycleInfoDto getTaxCycleInfo(String obligorId) {
        // TODO: taxCycleRepository.findByObligorId(obligorId) に差し替え
        return new TaxCycleInfoDto("毎月", "1日〜末日", "翌月末");
    }

    @Override
    public List<PaymentRecordDto> searchRecords(String obligorId, PaymentLedgerSearchForm form) {
        // TODO: declarationRepository.findByCondition(obligorId, form) に差し替え
        return DUMMY_RECORDS.stream()
                .filter(r -> form.getTargetYear() == null
                        || r.getTargetYearMonth().getYear() == form.getTargetYear().getYear())
                .filter(r -> !StringUtils.hasText(form.getStatus())
                        || r.getStatus().equals(form.getStatus()))
                .toList();
    }

    @Override
    public int calcTotalAmount(List<PaymentRecordDto> records) {
        return records.stream().mapToInt(PaymentRecordDto::getAmount).sum();
    }

    @Override
    public TaxDeclarationForm buildInitialDeclarationForm(String obligorId) {
        // TODO: DB実装後は既存申告データを取得してフォームに詰め替え
        TaxDeclarationForm form = new TaxDeclarationForm();
        form.setObligorId(obligorId);
        form.setTaxRate(200);
        form.setTaxableGuestCount(0);
        form.setTotalTaxAmount(0);
        form.setIsCorrection(false);
        return form;
    }

    @Override
    public void registerDeclaration(TaxDeclarationForm form) {
        // 納入金額をサーバー側で再計算（改ざん防止）
        int total = (form.getTaxableGuestCount() != null ? form.getTaxableGuestCount() : 0)
                  * (form.getTaxRate()           != null ? form.getTaxRate()           : 0);
        form.setTotalTaxAmount(total);
        // TODO: declarationRepository.save(toEntity(form)) に差し替え
        log.info("宿泊税申告登録: obligorId={}, yearMonth={}, total={}",
                form.getObligorId(), form.getTargetYearMonth(), total);
    }

    @Override
    public ConsolidatedDeclarationForm buildInitialConsolidatedForm(String obligorId) {
        // TODO: DB実装後は既存合算申告データを取得してフォームに詰め替え
        ConsolidatedDeclarationForm form = new ConsolidatedDeclarationForm();
        form.setRegistrationDate(LocalDate.now());
        form.setObligorId(obligorId);
        return form;
    }

    @Override
    public void registerConsolidated(ConsolidatedDeclarationForm form) {
        // TODO: consolidatedDeclarationRepository.save(toEntity(form)) に差し替え
        log.info("合算申告登録: obligorId={}, period={}, facilities={}",
                form.getObligorId(), form.getApplicablePeriod(), form.getFacilities());
    }
}
