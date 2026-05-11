package com.crm.modules.vente.controller;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.vente.dto.*;
import com.crm.modules.vente.service.IDevisService;
import com.crm.shared.enums.StatutDevis;
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
 * @author Riahi Dorsaf
 */
@Tag(name = "Ventes — Devis")
@RestController
@RequestMapping("/devis")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
public class DevisController {

    private final IDevisService devisService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DevisResponse>>> lister(
            @RequestParam(required = false) StatutDevis statut,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                devisService.lister(proprietaire.getId(), statut), "Devis récupérés."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DevisResponse>> obtenir(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                devisService.obtenir(id, proprietaire.getId()), "Devis récupéré."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DevisResponse>> creer(
            @Valid @RequestBody DevisRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                devisService.creer(request, proprietaire.getId()), "Devis créé."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DevisResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody DevisRequest request,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                devisService.modifier(id, request, proprietaire.getId()), "Devis modifié."));
    }

    @Operation(summary = "Changer le statut du devis (envoyer, accepter, refuser)")
    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<DevisResponse>> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutDevis statut,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.ok(ApiResponse.success(
                devisService.changerStatut(id, statut, proprietaire.getId()), "Statut mis à jour."));
    }

    @Operation(summary = "Convertir un devis accepté en facture")
    @PostMapping("/{id}/convertir-en-facture")
    public ResponseEntity<ApiResponse<FactureResponse>> convertirEnFacture(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                devisService.convertirEnFacture(id, proprietaire.getId()), "Facture créée."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal ProprietaireEntreprise proprietaire) {
        devisService.supprimer(id, proprietaire.getId());
        return ResponseEntity.ok(ApiResponse.success("Devis supprimé."));
    }
}