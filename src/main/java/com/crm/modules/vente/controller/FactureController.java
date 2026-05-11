package com.crm.modules.vente.controller;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.vente.dto.FactureResponse;
import com.crm.modules.vente.service.IFactureService;
import com.crm.shared.enums.StatutFacture;
import com.crm.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Tag(name = "Ventes — Factures")
@RestController
@RequestMapping("/factures")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class FactureController {

    private final IFactureService factureService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FactureResponse>>> lister(
            @RequestParam(required = false) StatutFacture statut,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                factureService.lister(proprietaire.getId(), statut), "Factures récupérées."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FactureResponse>> obtenir(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                factureService.obtenir(id, proprietaire.getId()), "Facture récupérée."));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<FactureResponse>> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutFacture statut,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                factureService.changerStatut(id, statut, proprietaire.getId()), "Statut mis à jour."));
    }
}