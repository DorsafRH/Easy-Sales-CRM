package com.crm.modules.entreprise.controller;

import com.crm.modules.entreprise.dto.EntrepriseCompteResponse;
import com.crm.modules.entreprise.dto.InscriptionEntrepriseRequest;
import com.crm.modules.entreprise.service.EntrepriseService;
import com.crm.modules.entreprise.service.IEntrepriseService;
import com.crm.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour les endpoints mobiles (React Native).
 * Base path : /api/entreprises
 *
 * DEV-18 : POST /entreprises/inscription    → Créer un compte entreprise
 * DEV-19 : GET  /entreprises/mon-compte/statut → Consulter son statut
 */
@RestController
@RequestMapping("/entreprises")
@RequiredArgsConstructor
public class EntrepriseController {

    private final IEntrepriseService entrepriseService;

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-18 : Création d'un compte entreprise (mobile, sans token)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/inscription")
    public ResponseEntity<ApiResponse<EntrepriseCompteResponse>> inscrire(
            @Valid @RequestBody InscriptionEntrepriseRequest request
    ) {
        EntrepriseCompteResponse response = entrepriseService.inscrireEntreprise(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response,
                        "Votre demande de création de compte a été soumise. " +
                        "Elle est en attente de validation par un administrateur."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEV-19 : Afficher le statut du compte (mobile, token requis)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/mon-compte/statut")
    public ResponseEntity<ApiResponse<EntrepriseCompteResponse>> consulterStatut(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EntrepriseCompteResponse response =
                entrepriseService.consulterMonStatut(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success(response, "Statut du compte récupéré avec succès.")
        );
    }
}
