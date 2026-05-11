package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.Opportunite;
import com.crm.shared.enums.StatutOpportunite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Riahi Dorsaf
 */
@Repository
public interface OpportuniteRepository
        extends JpaRepository<Opportunite, Long>, JpaSpecificationExecutor<Opportunite> {

    Optional<Opportunite> findByIdAndProprietaireId(Long id, Long proprietaireId);

    List<Opportunite> findByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    List<Opportunite> findByProprietaireIdAndStatutOrderByMontantEstimeDesc(
            Long proprietaireId, StatutOpportunite statut);

    long countByProprietaireId(Long proprietaireId);

    long countByProprietaireIdAndStatut(Long proprietaireId, StatutOpportunite statut);
}