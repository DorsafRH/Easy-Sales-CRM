package com.crm.modules.utilisateur.entity;

import com.crm.modules.entreprise.entity.EntrepriseCompte;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Propriétaire Entreprise : utilisateur principal de l'application mobile.
 * Lié à un EntrepriseCompte en relation 1-1.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "proprietaires_entreprise")
@DiscriminatorValue("PROPRIETAIRE")
@PrimaryKeyJoinColumn(name = "utilisateur_id")
public class ProprietaireEntreprise extends Utilisateur {

    @Column(name = "telephone")
    private String telephone;

    /**
     * Relation 1-1 avec EntrepriseCompte.
     * Le ProprietaireEntreprise est le propriétaire (owner) de la relation.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_compte_id", referencedColumnName = "id")
    private EntrepriseCompte entrepriseCompte;
}
