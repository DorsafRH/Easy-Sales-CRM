package com.crm.modules.vente.controller;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.vente.dto.*;
import com.crm.modules.vente.service.IOpportuniteService;
import com.crm.shared.enums.StatutOpportunite;
import com.crm.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller — Opportunités.
 * Base path : /api/opportunites
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Ventes — Opportunités", description = "Pipeline commercial Kanban")
@RestController
@RequestMapping("/opportunites")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class OpportuniteController {

    private final IOpportuniteService opportuniteService;

    @Operation(summary = "Lister les opportunités")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OpportuniteResponse>>> lister(
            @RequestParam(required = false) StatutOpportunite statut,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                opportuniteService.lister(proprietaire.getId(), statut, keyword),
                "Opportunités récupérées."));
    }

    @Operation(summary = "Kanban — opportunités groupées par statut")
    @GetMapping("/kanban")
    public ResponseEntity<ApiResponse<Map<StatutOpportunite, List<OpportuniteResponse>>>> kanban(
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                opportuniteService.listerParStatut(proprietaire.getId()),
                "Pipeline Kanban récupéré."));
    }

    @Operation(summary = "Détail d'une opportunité")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OpportuniteResponse>> obtenir(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                opportuniteService.obtenir(id, proprietaire.getId()), "Opportunité récupérée."));
    }

    @Operation(summary = "Créer une opportunité")
    @PostMapping
    public ResponseEntity<ApiResponse<OpportuniteResponse>> creer(
            @Valid @RequestBody OpportuniteRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                opportuniteService.creer(request, proprietaire.getId()), "Opportunité créée."));
    }

    @Operation(summary = "Modifier une opportunité")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OpportuniteResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody OpportuniteRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                opportuniteService.modifier(id, request, proprietaire.getId()), "Opportunité modifiée."));
    }

    @Operation(summary = "Changer le statut — drag & drop Kanban")
    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<OpportuniteResponse>> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutOpportunite statut,
            @RequestParam(required = false) String raisonPerte,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                opportuniteService.changerStatut(id, statut, raisonPerte, proprietaire.getId()),
                "Statut mis à jour."));
    }

    @Operation(summary = "Générer un devis brouillon depuis l'opportunité")
    @PostMapping("/{id}/generer-devis")
    public ResponseEntity<ApiResponse<DevisResponse>> genererDevis(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                opportuniteService.genererDevis(id, proprietaire.getId()), "Devis brouillon créé."));
    }

    @Operation(summary = "Supprimer une opportunité")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        opportuniteService.supprimer(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Opportunité supprimée."));
    }
}