package com.crm.modules.entreprise.controller;

import com.crm.modules.entreprise.dto.EntrepriseCompteResponse;
import com.crm.modules.entreprise.dto.InscriptionEntrepriseRequest;
import com.crm.modules.entreprise.service.IEntrepriseService;
import com.crm.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour les endpoints de l'application mobile (React Native).
 *
 * <p>Base path : {@code /api/entreprises}</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(
        name = "Entreprises (Mobile)",
        description = "Endpoints mobiles : inscription et consultation du statut du compte"
)
@RestController
@RequestMapping("/entreprises")
@RequiredArgsConstructor
public class EntrepriseController {

    private final IEntrepriseService entrepriseService;

    /**
     * Soumet une demande de création de compte entreprise (sans token).
     *
     * @param request les informations du propriétaire et de l'entreprise
     * @return le compte créé avec statut 201
     */
    @Operation(
            summary = "Inscription d'une entreprise",
            description = "Crée un compte avec le statut EN_ATTENTE. Aucun token JWT requis."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Compte créé, en attente de validation",
                    content = @Content(schema = @Schema(implementation = EntrepriseCompteResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Données invalides"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "E-mail ou matricule déjà utilisé")
    })
    @SecurityRequirements
    @PostMapping("/inscription")
    public ResponseEntity<ApiResponse<EntrepriseCompteResponse>> inscrire(
            @Valid @RequestBody InscriptionEntrepriseRequest request
    ) {
        EntrepriseCompteResponse response = entrepriseService.inscrireEntreprise(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response,
                        "Votre demande a été soumise. Elle est en attente de validation."));
    }

    /**
     * Retourne le statut du compte du propriétaire connecté.
     *
     * @param userDetails le principal Spring Security (token JWT)
     * @return les informations du compte et son statut
     */
    @Operation(
            summary = "Consulter le statut de son compte",
            description = "Retourne le statut du compte entreprise du propriétaire connecté. Token JWT requis."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Statut récupéré avec succès",
                    content = @Content(schema = @Schema(implementation = EntrepriseCompteResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Token JWT manquant ou invalide"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Propriétaire introuvable")
    })
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
