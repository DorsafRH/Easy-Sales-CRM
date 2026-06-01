package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.Lead;
import com.crm.shared.enums.StatutLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Zéro @Query — méthodes dérivées Spring Data uniquement.
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface LeadRepository
        extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    Optional<Lead> findByIdAndProprietaireId(Long id, Long proprietaireId);

    List<Lead> findByProprietaireIdAndStatutOrderByDateCreationDesc(
            Long proprietaireId, StatutLead statut);

    List<Lead> findByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    long countByProprietaireId(Long proprietaireId);

    long countByProprietaireIdAndStatut(Long proprietaireId, StatutLead statut);

    /**
     * Compte les leads dont le statut n'est PAS dans la liste fournie.
     * Utilisé pour comptabiliser les leads "actifs" (hors CONVERTI et PERDU).
     */
    long countByProprietaireIdAndStatutNotIn(
            Long proprietaireId, Collection<StatutLead> statuts);
}