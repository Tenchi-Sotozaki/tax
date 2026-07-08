package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.DeclarationRequest;
import jp.lg.asp.accommodation.dto.DeclarationResponse;
import jp.lg.asp.accommodation.exception.BusinessException;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: DB\u5b9f\u88c5\u5f8c\u306f @Service \u3092\u4ed8\u4e0e\u3057\u3066\u6709\u52b9\u5316\u3059\u308b\u3002
 * \u73fe\u5728\u306fDB\u672a\u63a5\u7d9a\u306e\u305f\u3081\u30b9\u30bf\u30d6\u5b9f\u88c5\u3002
 */
@Slf4j
public class AccommodationTaxDeclarationService {

    public DeclarationResponse register(DeclarationRequest request) {
        throw new BusinessException("ERR_NOT_IMPLEMENTED", "DB\u5b9f\u88c5\u5f8c\u306b\u6709\u52b9\u5316\u3055\u308c\u307e\u3059");
    }

    public DeclarationResponse update(Long declarationId, DeclarationRequest request) {
        throw new BusinessException("ERR_NOT_IMPLEMENTED", "DB\u5b9f\u88c5\u5f8c\u306b\u6709\u52b9\u5316\u3055\u308c\u307e\u3059");
    }

    public DeclarationResponse findById(Long declarationId) {
        throw new ResourceNotFoundException("\u7533\u544aID: " + declarationId + " \u304c\u898b\u3064\u304b\u308a\u307e\u305b\u3093");
    }
}
