package com.crm.modules.auth.entity;

import com.crm.modules.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Token de réinitialisation de mot de passe.
 * Durée de validité : 15 minutes.
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "utilise", nullable = false)
    private boolean utilise = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    public boolean estExpire() {
        return LocalDateTime.now().isAfter(this.dateExpiration);
    }

    public boolean estValide() {
        return !utilise && !estExpire();
    }
}