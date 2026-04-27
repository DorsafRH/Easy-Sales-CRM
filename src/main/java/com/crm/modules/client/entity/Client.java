package com.crm.modules.client.entity;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité abstraite représentant un client (individuel ou entreprise).
 * Stratégie d'héritage JOINED : table commune clients + tables spécialisées.
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@Entity
@Table(name = "clients")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_client", discriminatorType = DiscriminatorType.STRING)
public abstract class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_affichage", nullable = false)
    private String nomAffichage;

    @Column(name = "email")
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "ville")
    private String ville;

    @Column(name = "pays")
    private String pays;

    @Column(name = "statut", nullable = false)
    private String statut = "ACTIF";

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    public void supprimerLogiquement() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.statut = "INACTIF";
    }

    /**
     * Calcule et met à jour le nomAffichage selon le type concret du client.
     *
     * @author Riahi Dorsaf
     */
    public abstract void recalculerNomAffichage();
}
