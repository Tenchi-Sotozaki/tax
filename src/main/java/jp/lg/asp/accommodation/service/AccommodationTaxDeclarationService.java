package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.*;
import jp.lg.asp.accommodation.entity.*;
import jp.lg.asp.accommodation.exception.*;
import jp.lg.asp.accommodation.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccommodationTaxDeclarationService {

    private final AccommodationTaxDeclarationRepository declarationRepository;
    private final AccommodationFacilityRepository facilityRepository;
    private final SpecialCollectorRepository collectorRepository;
    private final TaxCategoryRepository taxCategoryRepository;

    public AccommodationTaxDeclarationService(
            @Lazy AccommodationTaxDeclarationRepository declarationRepository,
            @Lazy AccommodationFacilityRepository facilityRepository,
            @Lazy SpecialCollectorRepository collectorRepository,
            @Lazy TaxCategoryRepository taxCategoryRepository) {
        this.declarationRepository = declarationRepository;
        this.facilityRepository = facilityRepository;
        this.collectorRepository = collectorRepository;
        this.taxCategoryRepository = taxCategoryRepository;
    }

    /**
     * 申告を新規登録する。
     * 同一徴収義務者・施設・納入年月の重複登録はエラーとする。
     */
    @Transactional
    public DeclarationResponse register(DeclarationRequest request) {
        SpecialCollector collector = findCollector(request.getCollectorId());
        AccommodationFacility facility = findFacility(request.getFacilityId());

        validateFacilityBelongsToCollector(facility, collector);
        checkDuplicateDeclaration(request.getCollectorId(), request.getFacilityId(), request.getPaymentYearMonth());
        validateNights(request);

        AccommodationTaxDeclaration declaration = buildDeclaration(request, collector, facility);
        declarationRepository.save(declaration);
        return toResponse(declaration);
    }

    /**
     * 申告を編集する。
     * SUBMITTED/CONFIRMED 状態の申告は編集不可とする。
     */
    @Transactional
    public DeclarationResponse update(Long declarationId, DeclarationRequest request) {
        AccommodationTaxDeclaration declaration = declarationRepository.findByIdWithDetails(declarationId)
                .orElseThrow(() -> new ResourceNotFoundException("申告ID: " + declarationId + " が見つかりません"));

        if (!"DRAFT".equals(declaration.getStatus())) {
            throw new BusinessException("ERR_INVALID_STATUS", "提出済みまたは確定済みの申告は編集できません");
        }

        SpecialCollector collector = findCollector(request.getCollectorId());
        AccommodationFacility facility = findFacility(request.getFacilityId());

        validateFacilityBelongsToCollector(facility, collector);
        validateNights(request);

        // 納入年月が変更された場合のみ重複チェック
        if (!declaration.getPaymentYearMonth().equals(request.getPaymentYearMonth())) {
            checkDuplicateDeclaration(request.getCollectorId(), request.getFacilityId(), request.getPaymentYearMonth());
        }

        declaration.getDetails().clear();
        applyRequestToDeclaration(declaration, request, collector, facility);
        declarationRepository.save(declaration);
        return toResponse(declaration);
    }

    /**
     * 申告を1件取得する。
     */
    @Transactional(readOnly = true)
    public DeclarationResponse findById(Long declarationId) {
        return declarationRepository.findByIdWithDetails(declarationId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("申告ID: " + declarationId + " が見つかりません"));
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    private SpecialCollector findCollector(String collectorId) {
        return collectorRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("特別徴収義務者ID: " + collectorId + " が見つかりません"));
    }

    private AccommodationFacility findFacility(String facilityId) {
        return facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("宿泊施設ID: " + facilityId + " が見つかりません"));
    }

    private void validateFacilityBelongsToCollector(AccommodationFacility facility, SpecialCollector collector) {
        if (!facility.getCollector().getCollectorId().equals(collector.getCollectorId())) {
            throw new BusinessException("ERR_FACILITY_MISMATCH",
                    "指定された宿泊施設は、特別徴収義務者に紐づいていません");
        }
    }

    private void checkDuplicateDeclaration(String collectorId, String facilityId, String paymentYearMonth) {
        declarationRepository
                .findByCollector_CollectorIdAndFacility_FacilityIdAndPaymentYearMonth(
                        collectorId, facilityId, paymentYearMonth)
                .ifPresent(d -> {
                    throw new DuplicateDeclarationException(collectorId, facilityId, paymentYearMonth);
                });
    }

    /**
     * 総宿泊数 >= 課税対象外宿泊数 かつ
     * 総宿泊数 >= Σ課税対象宿泊数 + 課税対象外宿泊数 のチェック
     */
    private void validateNights(DeclarationRequest request) {
        int sumTaxableNights = request.getDetails().stream()
                .mapToInt(DeclarationDetailRequest::getTaxableNights)
                .sum();
        int expectedTotal = sumTaxableNights + request.getExemptNights();

        if (request.getTotalNights() < 0 || request.getExemptNights() < 0) {
            throw new BusinessException("ERR_INVALID_NIGHTS", "宿泊数に負の値は入力できません");
        }
        if (request.getTotalNights() != expectedTotal) {
            throw new BusinessException("ERR_NIGHTS_MISMATCH",
                    String.format("総宿泊数（%d）が課税対象宿泊数の合計（%d）＋課税対象外宿泊数（%d）と一致しません",
                            request.getTotalNights(), sumTaxableNights, request.getExemptNights()));
        }
    }

    private AccommodationTaxDeclaration buildDeclaration(
            DeclarationRequest request, SpecialCollector collector, AccommodationFacility facility) {

        AccommodationTaxDeclaration declaration = new AccommodationTaxDeclaration();
        applyRequestToDeclaration(declaration, request, collector, facility);
        return declaration;
    }

    private void applyRequestToDeclaration(
            AccommodationTaxDeclaration declaration,
            DeclarationRequest request,
            SpecialCollector collector,
            AccommodationFacility facility) {

        declaration.setCollector(collector);
        declaration.setFacility(facility);
        declaration.setPaymentYearMonth(request.getPaymentYearMonth());
        declaration.setTotalNights(request.getTotalNights());
        declaration.setExemptNights(request.getExemptNights());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (DeclarationDetailRequest detailReq : request.getDetails()) {
            TaxCategory taxCategory = taxCategoryRepository.findById(detailReq.getTaxCategoryCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "税区分コード: " + detailReq.getTaxCategoryCode() + " が見つかりません"));

            AccommodationTaxDeclarationDetail detail = new AccommodationTaxDeclarationDetail();
            detail.setDeclaration(declaration);
            detail.setTaxCategory(taxCategory);
            detail.setTaxableNights(detailReq.getTaxableNights());
            detail.setTaxAmountPerNight(taxCategory.getTaxAmount()); // スナップショット
            detail.calculateSubtotal();

            declaration.getDetails().add(detail);
            totalAmount = totalAmount.add(detail.getSubtotalAmount());
        }

        declaration.setTotalPaymentAmount(totalAmount);
    }

    private DeclarationResponse toResponse(AccommodationTaxDeclaration declaration) {
        List<DeclarationResponse.DeclarationDetailResponse> detailResponses = declaration.getDetails().stream()
                .map(d -> DeclarationResponse.DeclarationDetailResponse.builder()
                        .taxCategoryCode(d.getTaxCategory().getTaxCategoryCode())
                        .categoryName(d.getTaxCategory().getCategoryName())
                        .taxableNights(d.getTaxableNights())
                        .taxAmountPerNight(d.getTaxAmountPerNight())
                        .subtotalAmount(d.getSubtotalAmount())
                        .build())
                .toList();

        return DeclarationResponse.builder()
                .declarationId(declaration.getDeclarationId())
                .collectorId(declaration.getCollector().getCollectorId())
                .collectorName(declaration.getCollector().getCollectorName())
                .facilityId(declaration.getFacility().getFacilityId())
                .facilityName(declaration.getFacility().getFacilityName())
                .paymentYearMonth(declaration.getPaymentYearMonth())
                .totalNights(declaration.getTotalNights())
                .exemptNights(declaration.getExemptNights())
                .totalPaymentAmount(declaration.getTotalPaymentAmount())
                .status(declaration.getStatus())
                .details(detailResponses)
                .build();
    }
}
