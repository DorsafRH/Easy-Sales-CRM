package com.crm.modules.entreprise.entity;

import com.crm.shared.enums.StatutCompte;
import com.crm.shared.enums.TailleEntreprise;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * EntrepriseCompte : dossier administratif de l'entreprise cliente.
 * Cycle de vie du statut : EN_ATTENTE → ACTIVE | REFUSE | SUSPENDU
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entreprise_comptes")
public class EntrepriseCompte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_entreprise", nullable = false)
    private String nomEntreprise;

    @Column(name = "matricule_fiscale", nullable = false, unique = true)
    private String matriculeFiscale;

    @Column(name = "secteur_activite")
    private String secteurActivite;

    @Enumerated(EnumType.STRING)
    @Column(name = "taille_entreprise")
    private TailleEntreprise tailleEntreprise;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "ville")
    private String ville;

    @Column(name = "pays")
    private String pays;

    @Column(name = "site_web")
    private String siteWeb;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_compte", nullable = false)
    private StatutCompte statutCompte;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    /**
     * Référence vers le SuperAdmin qui a validé ou refusé ce compte.
     * Nullable tant que la décision n'est pas prise.
     */
    @Column(name = "valide_par")
    private Long validePar;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.statutCompte = StatutCompte.EN_ATTENTE;
    }

    // ── Méthodes métier ──────────────────────────────────────────────────────

    public void valider(Long adminId) {
        this.statutCompte = StatutCompte.ACTIVE;
        this.dateValidation = LocalDateTime.now();
        this.validePar = adminId;
        this.motifRefus = null;
    }

    public void refuser(Long adminId, String motif) {
        this.statutCompte = StatutCompte.REFUSE;
        this.dateValidation = LocalDateTime.now();
        this.validePar = adminId;
        this.motifRefus = motif;
    }

    public void suspendre() {
        this.statutCompte = StatutCompte.SUSPENDU;
    }

    public void supprimerLogiquement() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
