package com.crm.modules.utilisateur.controller;

import com.crm.modules.utilisateur.dto.ModifierEntrepriseRequest;
import com.crm.modules.utilisateur.dto.ModifierProfilRequest;
import com.crm.modules.utilisateur.dto.ProfilProprietaireResponse;
import com.crm.modules.utilisateur.service.ProprietaireService;
import com.crm.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour la gestion du profil du propriétaire d'entreprise.
 *
 * <p>Base path : {@code /api/proprietaire}</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(
        name = "Propriétaire — Profil",
        description = "Endpoints de gestion du profil et des données entreprise du propriétaire connecté"
)
@RestController
@RequestMapping("/proprietaire")
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
@RequiredArgsConstructor
public class ProprietaireController {

    private final ProprietaireService proprietaireService;

    @Operation(summary = "Consulter son profil complet")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Profil récupéré"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Non authentifié")
    })
    @GetMapping("/profil")
    public ResponseEntity<ApiResponse<ProfilProprietaireResponse>> consulterProfil(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProfilProprietaireResponse response =
                proprietaireService.consulterProfil(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success(response, "Profil récupéré avec succès."));
    }

    @Operation(
            summary = "Modifier ses données personnelles",
            description = "Met à jour le nom, prénom et téléphone du propriétaire."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Données personnelles mises à jour"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Données invalides")
    })
    @PutMapping("/profil")
    public ResponseEntity<ApiResponse<ProfilProprietaireResponse>> modifierProfil(
            @Valid @RequestBody ModifierProfilRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProfilProprietaireResponse response =
                proprietaireService.modifierProfil(userDetails.getUsername(), request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Données personnelles mises à jour."));
    }

    @Operation(
            summary = "Modifier les données de son entreprise",
            description = "Met à jour les informations de l'entreprise. " +
                    "Une notification est envoyée au Super Admin."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Données entreprise mises à jour"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Données invalides")
    })
    @PutMapping("/entreprise")
    public ResponseEntity<ApiResponse<ProfilProprietaireResponse>> modifierEntreprise(
            @Valid @RequestBody ModifierEntrepriseRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProfilProprietaireResponse response =
                proprietaireService.modifierEntreprise(userDetails.getUsername(), request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Données entreprise mises à jour."));
    }
}