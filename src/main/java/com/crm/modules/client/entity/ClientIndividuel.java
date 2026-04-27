package com.crm.modules.client.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Client de type personne physique.
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clients_individuels")
@DiscriminatorValue("INDIVIDUEL")
@PrimaryKeyJoinColumn(name = "client_id")
public class ClientIndividuel extends Client {

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @Override
    public void recalculerNomAffichage() {
        this.setNomAffichage(prenom + " " + nom);
    }
}