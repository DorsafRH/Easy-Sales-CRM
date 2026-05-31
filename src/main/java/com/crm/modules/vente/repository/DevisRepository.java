package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.Devis;
import com.crm.shared.enums.StatutDevis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
/**
 * @author Riahi Dorsaf
 */
@Repository
public interface DevisRepository
        extends JpaRepository<Devis, Long>, JpaSpecificationExecutor<Devis> {

    Optional<Devis> findByIdAndProprietaireId(Long id, Long proprietaireId);

    List<Devis> findByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    long countByProprietaireIdAndStatut(Long proprietaireId, StatutDevis statut);

    Optional<Devis> findTopByProprietaireIdAndNumeroStartingWithOrderByNumeroDesc(
            Long proprietaireId, String prefixe);

    List<Devis> findByOpportuniteIdAndProprietaireId(Long opportuniteId, Long proprietaireId);

    boolean existsByOpportuniteIdAndStatutNotIn(Long opportuniteId, List<StatutDevis> statuts);
}