package jp.lg.asp.accommodation.controller;

import jakarta.validation.Valid;
import jp.lg.asp.accommodation.dto.DeclarationRequest;
import jp.lg.asp.accommodation.dto.DeclarationResponse;
import jp.lg.asp.accommodation.service.AccommodationTaxDeclarationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/declarations")
public class AccommodationTaxDeclarationController {

    private final AccommodationTaxDeclarationService declarationService;

    @Autowired
    public AccommodationTaxDeclarationController(
            @Lazy AccommodationTaxDeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeclarationResponse> getDeclaration(@PathVariable Long id) {
        return ResponseEntity.ok(declarationService.findById(id));
    }

    @PostMapping
    public ResponseEntity<DeclarationResponse> registerDeclaration(
            @Valid @RequestBody DeclarationRequest request) {
        DeclarationResponse response = declarationService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getDeclarationId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeclarationResponse> updateDeclaration(
            @PathVariable Long id,
            @Valid @RequestBody DeclarationRequest request) {
        return ResponseEntity.ok(declarationService.update(id, request));
    }
}
