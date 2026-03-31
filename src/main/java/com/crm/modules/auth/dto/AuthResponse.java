package com.crm.modules.auth.dto;

import com.crm.shared.enums.RoleUtilisateur;
import lombok.Builder;
import lombok.Data;

/**
 * Réponse retournée après une authentification réussie.
 */
@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;

    private Long userId;
    private String nom;
    private String prenom;
    private String email;
    private RoleUtilisateur role;

    // Spécifique au ProprietaireEntreprise (null pour SuperAdmin)
    private Long entrepriseId;
    private String nomEntreprise;
    private String statutCompte;
}
