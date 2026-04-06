package jp.lg.asp.accommodation.controller;

import jakarta.validation.Valid;
import jp.lg.asp.accommodation.dto.DeclarationRequest;
import jp.lg.asp.accommodation.dto.DeclarationResponse;
import jp.lg.asp.accommodation.service.AccommodationTaxDeclarationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/declarations")
@RequiredArgsConstructor
public class AccommodationTaxDeclarationController {

    private final AccommodationTaxDeclarationService declarationService;

    /**
     * 申告を1件取得する。
     * GET /api/declarations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeclarationResponse> getDeclaration(@PathVariable Long id) {
        return ResponseEntity.ok(declarationService.findById(id));
    }

    /**
     * 申告を新規登録する。
     * POST /api/declarations
     */
    @PostMapping
    public ResponseEntity<DeclarationResponse> registerDeclaration(
            @Valid @RequestBody DeclarationRequest request) {

        DeclarationResponse response = declarationService.register(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getDeclarationId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * 申告を編集する。
     * PUT /api/declarations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<DeclarationResponse> updateDeclaration(
            @PathVariable Long id,
            @Valid @RequestBody DeclarationRequest request) {

        return ResponseEntity.ok(declarationService.update(id, request));
    }
}
