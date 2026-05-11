package com.crm.modules.vente.entity;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.ResultatActivite;
import com.crm.shared.enums.TypeActiviteCommerciale;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Activité commerciale loggée sur un Lead ou une Opportunité.
 * Types : APPEL / EMAIL / REUNION / VISITE / TACHE
 * Alimente la timeline de la fiche client/lead/opportunité.
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
        name = "activites_commerciales",
        indexes = {
                @Index(name = "idx_actcom_lead",       columnList = "lead_id"),
                @Index(name = "idx_actcom_opportunite", columnList = "opportunite_id"),
        }
)
public class ActiviteCommerciale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TypeActiviteCommerciale type;

    @Column(name = "sujet", nullable = false, length = 200)
    private String sujet;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat", length = 20)
    private ResultatActivite resultat;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(name = "date_activite", nullable = false)
    private LocalDateTime dateActivite;

    // ── Relations ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunite_id")
    private Opportunite opportunite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() { this.dateCreation = LocalDateTime.now(); }
}