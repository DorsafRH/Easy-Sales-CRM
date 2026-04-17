package com.crm.modules.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notification destinée au Super Admin.
 * Consultée par polling toutes les 30 secondes depuis Angular.
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_notification", nullable = false)
    private TypeNotification type;

    @Column(name = "lue", nullable = false)
    private boolean lue = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /** Lien optionnel vers la ressource concernée (ex: /admin/entreprises/42). */
    @Column(name = "lien_ressource")
    private String lienRessource;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}