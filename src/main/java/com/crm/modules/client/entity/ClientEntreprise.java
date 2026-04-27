package com.crm.modules.client.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Client de type personne morale (société).
 *
 * @author Riahi Dorsaf
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clients_entreprises")
@DiscriminatorValue("ENTREPRISE")
@PrimaryKeyJoinColumn(name = "client_id")
public class ClientEntreprise extends Client {

    @Column(name = "raison_sociale", nullable = false)
    private String raisonSociale;

    @Override
    public void recalculerNomAffichage() {
        this.setNomAffichage(raisonSociale);
    }
}