package com.crm.modules.vente.entity;

import com.crm.modules.client.entity.Client;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.SourceLead;
import com.crm.shared.enums.StatutLead;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lead (prospect) — première étape du pipeline commercial.
 * Peut être converti en Opportunité avec ou sans création d'un Client CRM.
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
        name = "leads",
        indexes = {
                @Index(name = "idx_lead_proprietaire", columnList = "proprietaire_id"),
                @Index(name = "idx_lead_statut",       columnList = "statut"),
        }
)
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Informations du prospect ──────────────────────────────
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @Column(name = "entreprise", length = 200)
    private String entreprise;

    @Column(name = "poste", length = 100)
    private String poste;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private SourceLead source;

    @Column(name = "description_besoin", columnDefinition = "TEXT")
    private String descriptionBesoin;

    // ── Statut pipeline ───────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutLead statut = StatutLead.NOUVEAU;

    /** Score de priorité 0-100 — calculé localement côté service */
    @Column(name = "score")
    @Builder.Default
    private Integer score = 0;

    @Column(name = "raison_perte", columnDefinition = "TEXT")
    private String raisonPerte;

    // ── Client CRM associé (après conversion) ────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    // ── Relations ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;

    // ── Dates ─────────────────────────────────────────────────
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() { this.dateCreation = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.dateModification = LocalDateTime.now(); }
}