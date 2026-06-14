package com.crm.modules.marketing.entity;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.TypeReseau;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Compte de réseau social connecté par un propriétaire via OAuth Meta.
 * Stocke le token de page utilisé pour publier via l'API Graph.
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "comptes_sociaux_connectes",
        indexes = {
                @Index(name = "idx_compte_social_proprietaire", columnList = "proprietaire_id"),
                @Index(name = "idx_compte_social_type", columnList = "type_reseau"),
        }
)
public class CompteSocialConnecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_reseau", nullable = false, length = 30)
    private TypeReseau typeReseau;

    @Column(name = "nom_compte", nullable = false, length = 200)
    private String nomCompte;

    @Column(name = "identifiant_externe", length = 200)
    private String identifiantExterne;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "statut_connexion", length = 30)
    private String statutConnexion;

    @Column(name = "date_connexion")
    private LocalDateTime dateConnexion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;
}
