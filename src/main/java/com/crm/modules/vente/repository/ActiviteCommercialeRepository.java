package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.ActiviteCommerciale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Repository
public interface ActiviteCommercialeRepository extends JpaRepository<ActiviteCommerciale, Long> {

    List<ActiviteCommerciale> findByLeadIdOrderByDateActiviteDesc(Long leadId);

    List<ActiviteCommerciale> findByOpportuniteIdOrderByDateActiviteDesc(Long opportuniteId);

    List<ActiviteCommerciale> findByProprietaireIdOrderByDateActiviteDesc(Long proprietaireId);
}