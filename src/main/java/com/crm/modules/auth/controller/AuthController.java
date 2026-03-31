package com.crm.modules.auth.controller;

import com.crm.modules.auth.dto.AuthResponse;
import com.crm.modules.auth.dto.LoginRequest;
import com.crm.modules.auth.service.AuthService;
import com.crm.modules.auth.service.IAuthService;
import com.crm.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur d'authentification partagé.
 *
 * POST /api/auth/login  → DEV-XX Connexion ProprietaireEntreprise (mobile)
 *                       → DEV-XX Connexion SuperAdmin (Angular)
 *
 * Le rôle est déterminé automatiquement depuis la base (discriminator).
 * Le client distingue les deux cas via le champ "role" dans la réponse.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    /**
     * Endpoint unique de connexion pour tous les rôles.
     * L'application mobile et le backoffice Angular appellent tous les deux ce endpoint.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Connexion réussie.")
        );
    }
}
