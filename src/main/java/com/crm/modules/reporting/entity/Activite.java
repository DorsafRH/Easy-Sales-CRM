package com.crm.modules.reporting.entity;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.TypeActivite;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité représentant une action métier enregistrée dans le journal d'activité.
 *
 * <p>{@code entiteParentId} stocke l'identifiant de l'entité parente quand
 * nécessaire — ex : clientId pour un contact, afin de permettre la navigation
 * directe vers la fiche contact depuis le Dashboard.</p>
 *
 * @author Riahi Dorsaf
 */
@Entity
@Table(
        name = "activites",
        indexes = {
                @Index(name = "idx_activite_proprietaire_date",
                        columnList = "proprietaire_id, date_creation DESC"),
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TypeActivite type;

    /**
     * Titre principal affiché dans l'UI (nom du client, du produit…).
     */
    @Column(nullable = false, length = 200)
    private String titre;

    /**
     * Description de l'action (ex: "Nouveau client ajouté").
     */
    @Column(nullable = false, length = 200)
    private String description;

    /**
     * ID de l'entité concernée (clientId, contactId, produitId…).
     */
    @Column(nullable = false)
    private Long entiteId;

    /**
     * Type de l'entité ("CLIENT", "CONTACT", "PRODUIT").
     */
    @Column(nullable = false, length = 50)
    private String entiteType;

    /**
     * ID de l'entité parente — optionnel.
     * Utilisé pour les contacts : stocke le clientId pour permettre
     * la navigation directe vers ContactDetail depuis le Dashboard.
     */
    @Column(nullable = true)
    private Long entiteParentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}