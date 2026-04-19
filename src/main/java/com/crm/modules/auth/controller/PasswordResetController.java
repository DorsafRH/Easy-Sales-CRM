package com.crm.modules.auth.controller;

import com.crm.modules.auth.dto.MotDePasseOublieRequest;
import com.crm.modules.auth.dto.ReinitialisationMotDePasseRequest;
import com.crm.modules.auth.dto.VerifierCodeRequest;
import com.crm.modules.auth.service.IPasswordResetService;
import com.crm.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur de réinitialisation de mot de passe.
 * Endpoints publics — aucun token JWT requis.
 *
 * <p>Base path : {@code /api/auth}</p>
 *
 * @author Riahi Dorsaf
 */
@Tag(
        name = "Authentification",
        description = "Endpoint de connexion partagé pour les propriétaires d'entreprise (mobile) et les super-administrateurs (backoffice Angular)"
)
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final IPasswordResetService passwordResetService;

    /**
     * Demande de réinitialisation — envoie un email si l'adresse existe.
     * Retourne toujours 200 pour ne pas révéler l'existence d'un compte.
     */
    @Operation(
            summary = "Demande de réinitialisation de mot de passe",
            description = "Envoie un email avec un code valable 15 minutes. " +
                    "Retourne toujours 200 pour des raisons de sécurité."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Email envoyé si le compte existe"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Email invalide")
    })
    @SecurityRequirements
    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<ApiResponse<Void>> demanderReinitialisation(
            @Valid @RequestBody MotDePasseOublieRequest request
    ) {
        passwordResetService.demanderReinitialisation(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Si cet email est associé à un compte, vous recevrez un lien de réinitialisation."));
    }

    /**
     * Réinitialise le mot de passe avec le token reçu par email.
     */
    @Operation(
            summary = "Réinitialisation du mot de passe",
            description = "Valide le token et met à jour le mot de passe."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Mot de passe mis à jour"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Token invalide, expiré ou mot de passe trop court")
    })
    @SecurityRequirements
    @PostMapping("/reinitialiser-mot-de-passe")
    public ResponseEntity<ApiResponse<Void>> reinitialiserMotDePasse(
            @Valid @RequestBody ReinitialisationMotDePasseRequest request
    ) {
        passwordResetService.reinitialiserMotDePasse(request);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe réinitialisé avec succès."));
    }
    /**
     * Vérifie le code à 6 chiffres reçu par email.
     * Appelé avant l'écran de réinitialisation.
     */
    @Operation(
            summary = "Vérifier le code de réinitialisation",
            description = "Vérifie que le code à 6 chiffres est valide et non expiré."
    )
    @SecurityRequirements
    @PostMapping("/verifier-code")
    public ResponseEntity<ApiResponse<Void>> verifierCode(
            @Valid @RequestBody VerifierCodeRequest request
    ) {
        passwordResetService.verifierCode(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Code valide."));
    }
}