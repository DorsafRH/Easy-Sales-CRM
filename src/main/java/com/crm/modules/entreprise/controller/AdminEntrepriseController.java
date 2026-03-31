package com.crm.modules.entreprise.controller;

import com.crm.modules.entreprise.dto.EntrepriseCompteResponse;
import com.crm.modules.entreprise.dto.ValiderEntrepriseRequest;
import com.crm.modules.entreprise.service.EntrepriseService;
import com.crm.shared.enums.StatutCompte;
import com.crm.shared.response.ApiResponse;
import com.crm.shared.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour le backoffice Angular (SuperAdmin uniquement).
 * Base path : /api/admin/entreprises
 *
 * DEV-20 : GET  /admin/entreprises/en-attente          → Liste comptes en attente
 * DEV-20 : GET  /admin/entreprises                     → Liste toutes les entreprises (avec filtres)
 * DEV-21 : POST /admin/entreprises/{id}/decision       → Valider ou refuser
 * DEV-53 : GET  /admin/entreprises/{id}                → Détails d'une entreprise
 * DEV-54 : DELETE /admin/entreprises/{id}              → Suppression logique
 */
@RestController
@RequestMapping("/admin/entreprises")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminEntrepriseController {

    private final EntrepriseService entrepriseService;

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-20 : Liste des comptes en attente de validation
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/en-attente")
    public ResponseEntity<ApiResponse<PageResponse<EntrepriseCompteResponse>>> listerEnAttente(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<EntrepriseCompteResponse> result =
                entrepriseService.listerComptesEnAttente(page, size);
        return ResponseEntity.ok(
                ApiResponse.success(result, "Comptes en attente récupérés avec succès.")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-20 (extension) : Liste toutes les entreprises avec filtres optionnels
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EntrepriseCompteResponse>>> listerTous(
            @RequestParam(required = false) StatutCompte statut,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<EntrepriseCompteResponse> result =
                entrepriseService.listerEntreprises(statut, keyword, page, size);
        return ResponseEntity.ok(
                ApiResponse.success(result, "Entreprises récupérées avec succès.")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-53 : Consulter les détails d'une entreprise
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EntrepriseCompteResponse>> consulterDetails(
            @PathVariable Long id
    ) {
        EntrepriseCompteResponse response = entrepriseService.consulterDetails(id);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Détails de l'entreprise récupérés avec succès.")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-21 : Valider ou refuser un compte entreprise
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<EntrepriseCompteResponse>> traiterDemande(
            @PathVariable Long id,
            @Valid @RequestBody ValiderEntrepriseRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EntrepriseCompteResponse response =
                entrepriseService.traiterDemande(id, request, userDetails.getUsername());

        String message = Boolean.TRUE.equals(request.getValider())
                ? "Compte entreprise validé avec succès. Un email de confirmation a été envoyé."
                : "Compte entreprise refusé. Un email de notification a été envoyé.";

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-54 : Suppression logique d'une entreprise
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> supprimerLogiquement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        entrepriseService.supprimerLogiquement(id, userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success("Entreprise supprimée avec succès.")
        );
    }
}
