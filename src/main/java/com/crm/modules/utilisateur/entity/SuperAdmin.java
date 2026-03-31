package com.crm.modules.utilisateur.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Super Admin : acteur de backoffice qui gère la plateforme via Angular.
 * Il valide / refuse / suspend les comptes entreprises.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "super_admins")
@DiscriminatorValue("SUPER_ADMIN")
@PrimaryKeyJoinColumn(name = "utilisateur_id")
public class SuperAdmin extends Utilisateur {

    // Pas d'attributs supplémentaires pour le Sprint 1.
    // Les méthodes métier (valider, refuser, suspendre) sont dans le service.
}
