package com.crm.modules.agenda.entity;

import com.crm.modules.client.entity.Client;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.StatutReunion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant une réunion planifiée avec un client.
 *
 * <p><b>Participants :</b> stockés dans la table {@code reunion_participants}
 * via {@code @ElementCollection}. Peut contenir le client principal,
 * ses contacts, et des invités externes.</p>
 *
 * <p><b>Lien réunion :</b> générique — peut être un lien Jitsi Meet,
 * Google Meet, Teams ou tout autre outil de visio. Généré automatiquement
 * si {@code enLigne = true} et aucun lien fourni.</p>
 *
 * <p><b>Rappels :</b> liste d'entiers représentant le nombre de minutes
 * avant la réunion. Utilisés côté mobile pour planifier les notifications
 * locales via {@code expo-notifications}.</p>
 *
 * <p><b>Double booking :</b> vérifié côté service avant toute création
 * ou modification — une exception est levée si un conflit horaire existe.</p>
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
        name = "reunions",
        indexes = {
                @Index(name = "idx_reunion_proprietaire_date",
                        columnList = "proprietaire_id, date_heure"),
                @Index(name = "idx_reunion_client",
                        columnList = "client_id"),
        }
)
public class Reunion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─────────────────────────────────────────────────────────
    //  Informations de base
    // ─────────────────────────────────────────────────────────

    /**
     * Titre de la réunion (ex: "Présentation offre commerciale").
     */
    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    /**
     * Date et heure de début de la réunion.
     */
    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    /**
     * Durée de la réunion en minutes.
     */
    @Column(name = "duree_minutes", nullable = false)
    private int dureeMinutes;

    /**
     * Lieu physique — optionnel (ex: "Bureau client", "Siège social").
     */
    @Column(name = "lieu", length = 200)
    private String lieu;

    /**
     * Notes libres sur la réunion.
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ─────────────────────────────────────────────────────────
    //  Réunion en ligne
    // ─────────────────────────────────────────────────────────

    /**
     * Indique si la réunion se tient en ligne.
     * Si {@code true} et qu'aucun lien n'est fourni, un lien Jitsi Meet
     * est généré automatiquement à la création.
     */
    // Après
    @Column(name = "en_ligne", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean enLigne = false;

    /**
     * Lien de la réunion en ligne — générique.
     *
     * <p>Exemples :</p>
     * <ul>
     *   <li>Jitsi Meet : {@code https://meet.jit.si/EasySalesCRM-abc123}</li>
     *   <li>Google Meet : {@code https://meet.google.com/xxx-xxxx-xxx}</li>
     *   <li>Teams : {@code https://teams.microsoft.com/...}</li>
     * </ul>
     */
    @Column(name = "lien_reunion", length = 500)
    private String lienReunion;

    // ─────────────────────────────────────────────────────────
    //  Statut
    // ─────────────────────────────────────────────────────────

    /**
     * Statut de la réunion.
     * Par défaut {@link StatutReunion#PLANIFIEE} à la création.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutReunion statut = StatutReunion.PLANIFIEE;

    // ─────────────────────────────────────────────────────────
    //  Participants
    // ─────────────────────────────────────────────────────────

    /**
     * Liste des participants à la réunion.
     *
     * <p>Peut inclure :</p>
     * <ul>
     *   <li>Le client principal ({@link com.crm.shared.enums.TypeParticipant#CLIENT})</li>
     *   <li>Des contacts du client ({@link com.crm.shared.enums.TypeParticipant#CONTACT})</li>
     *   <li>Des invités externes ({@link com.crm.shared.enums.TypeParticipant#EXTERNE})</li>
     * </ul>
     */
    @ElementCollection
    @CollectionTable(
            name = "reunion_participants",
            joinColumns = @JoinColumn(name = "reunion_id")
    )
    @Builder.Default
    private List<ReunionParticipant> participants = new ArrayList<>();

    // ─────────────────────────────────────────────────────────
    //  Rappels
    // ─────────────────────────────────────────────────────────

    /**
     * Liste des rappels en minutes avant la réunion.
     * Utilisé par le mobile pour planifier les notifications locales.
     *
     * <p>Exemples : {@code 0} (à l'heure), {@code 30} (30 min avant),
     * {@code 1440} (1 jour avant).</p>
     */
    @ElementCollection
    @CollectionTable(
            name = "reunion_rappels",
            joinColumns = @JoinColumn(name = "reunion_id")
    )
    @Column(name = "minutes_avant")
    @Builder.Default
    private List<Integer> rappelsMinutes = new ArrayList<>();

    // ─────────────────────────────────────────────────────────
    //  Relations
    // ─────────────────────────────────────────────────────────

    /**
     * Client principal de la réunion — obligatoire.
     * Cloisonnement multi-tenant via {@code proprietaire}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /**
     * Propriétaire connecté — cloisonnement multi-tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private ProprietaireEntreprise proprietaire;

    // ─────────────────────────────────────────────────────────
    //  Dates
    // ─────────────────────────────────────────────────────────

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}