package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.Facture;
import com.crm.shared.enums.StatutFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Riahi Dorsaf
 */
@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByIdAndProprietaireId(Long id, Long proprietaireId);

    List<Facture> findByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    long countByProprietaireIdAndStatut(Long proprietaireId, StatutFacture statut);

    Optional<Facture> findTopByProprietaireIdAndNumeroStartingWithOrderByNumeroDesc(
            Long proprietaireId, String prefixe);
}