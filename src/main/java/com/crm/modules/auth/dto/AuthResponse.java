package com.crm.modules.auth.dto;

import com.crm.shared.enums.RoleUtilisateur;
import lombok.Builder;
import lombok.Data;


/**
 * DTO de réponse retourné après une authentification réussie.
 *
 * <p>Contient les tokens JWT (access + refresh), le type de token,
 * ainsi que les informations de base de l'utilisateur connecté.
 * Les champs {@code entrepriseId}, {@code nomEntreprise} et {@code statutCompte}
 * ne sont renseignés que pour les propriétaires d'entreprise ({@code ROLE_PROPRIETAIRE}) ;
 * ils sont {@code null} pour les super-administrateurs.</p>
 *
 * @author Riahi Dorsaf
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
