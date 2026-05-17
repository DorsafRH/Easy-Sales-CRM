package com.crm.modules.auth.controller;

import com.crm.modules.auth.dto.AuthResponse;
import com.crm.modules.auth.dto.LoginRequest;
import com.crm.modules.auth.service.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur d'authentification partagé pour tous les rôles.
 *
 * <p>Expose un endpoint unique de connexion utilisé à la fois par
 * l'application mobile (React Native) et le backoffice Angular.
 * Le rôle de l'utilisateur est déterminé automatiquement depuis la base
 * de données via le discriminateur JPA, et est retourné dans la réponse
 * pour que le client puisse adapter son comportement.</p>
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
public class AuthController {

    private final IAuthService authService;

    /**
     * Authentifie un utilisateur (propriétaire ou super-admin) et retourne un token JWT.
     *
     * <p>Le champ {@code role} présent dans la réponse permet au client de déterminer
     * vers quelle interface rediriger l'utilisateur après connexion.</p>
     *
     * @param request les credentials de connexion (e-mail et mot de passe)
     * @return une réponse contenant le token JWT et les informations de l'utilisateur
     */
    @Operation(
            summary = "Connexion utilisateur",
            description = "Authentifie un propriétaire d'entreprise ou un super-administrateur. " +
                    "Retourne un token JWT et le rôle de l'utilisateur connecté."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Connexion réussie — token JWT retourné",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects"),
            @ApiResponse(responseCode = "400", description = "Données de connexion invalides")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<com.crm.shared.response.ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                com.crm.shared.response.ApiResponse.success(response, "Connexion réussie.")
        );
    }
}
