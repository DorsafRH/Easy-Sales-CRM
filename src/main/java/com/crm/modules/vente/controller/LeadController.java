package com.crm.modules.vente.controller;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.vente.dto.LeadRequest;
import com.crm.modules.vente.dto.LeadResponse;
import com.crm.modules.vente.dto.OpportuniteResponse;
import com.crm.modules.vente.service.ILeadService;
import com.crm.shared.enums.StatutLead;
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

/**
 * REST Controller — Leads / Prospects.
 * Base path : /api/leads
 *
 * @author Riahi Dorsaf
 */
@Tag(name = "Ventes — Leads", description = "Gestion des prospects CRM")
@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class LeadController {

    private final ILeadService leadService;

    @Operation(summary = "Lister les leads", description = "Filtre optionnel par statut et keyword")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LeadResponse>>> lister(
            @RequestParam(required = false) StatutLead statut,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                leadService.lister(proprietaire.getId(), statut, keyword), "Leads récupérés."));
    }

    @Operation(summary = "Détail d'un lead")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadResponse>> obtenir(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                leadService.obtenir(id, proprietaire.getId()), "Lead récupéré."));
    }

    @Operation(summary = "Créer un lead")
    @PostMapping
    public ResponseEntity<ApiResponse<LeadResponse>> creer(
            @Valid @RequestBody LeadRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                leadService.creer(request, proprietaire.getId()), "Lead créé."));
    }

    @Operation(summary = "Modifier un lead")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody LeadRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                leadService.modifier(id, request, proprietaire.getId()), "Lead modifié."));
    }

    @Operation(summary = "Changer le statut d'un lead")
    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<LeadResponse>> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutLead statut,
            @RequestParam(required = false) String raisonPerte,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                leadService.changerStatut(id, statut, raisonPerte, proprietaire.getId()),
                "Statut mis à jour."));
    }

    @Operation(summary = "Convertir un lead en opportunité")
    @PostMapping("/{id}/convertir")
    public ResponseEntity<ApiResponse<OpportuniteResponse>> convertir(
            @PathVariable Long id,
            @RequestParam(required = false) Long clientExistantId,
            @RequestParam(defaultValue = "false") boolean creerNouveauClient,
            @RequestParam(required = false) String titreOpportunite,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                leadService.convertirEnOpportunite(id, clientExistantId,
                        creerNouveauClient, titreOpportunite, proprietaire.getId()),
                "Lead converti en opportunité."));
    }

    @Operation(summary = "Supprimer un lead")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        leadService.supprimer(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Lead supprimé."));
    }
}