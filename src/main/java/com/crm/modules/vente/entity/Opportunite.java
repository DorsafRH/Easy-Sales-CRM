package com.crm.modules.vente.entity;

import com.crm.modules.client.entity.Client;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutOpportunite;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Opportunité commerciale — cœur du pipeline Kanban.
 * Liée à un Client CRM et optionnellement à un Lead source.
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
        name = "opportunites",
        indexes = {
                @Index(name = "idx_opport_proprietaire", columnList = "proprietaire_id"),
                @Index(name = "idx_opport_statut",       columnList = "statut"),
                @Index(name = "idx_opport_client",        columnList = "client_id"),
        }
)
public class Opportunite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "montant_estime", precision = 12, scale = 3)
    private BigDecimal montantEstime;

    @Column(name = "probabilite")
    private Integer probabilite;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutOpportunite statut = StatutOpportunite.PROSPECTION;

    @Column(name = "date_cloture_prevue")
    private LocalDate dateCloturePrevue;

    @Column(name = "raison_perte", columnDefinition = "TEXT")
    private String raisonPerte;

    // ── Relations ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Lead à l'origine de cette opportunité — optionnel */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

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