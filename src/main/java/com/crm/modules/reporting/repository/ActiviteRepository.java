package com.crm.modules.reporting.repository;

import com.crm.modules.reporting.entity.Activite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA pour l'entité {@link Activite}.
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface ActiviteRepository extends JpaRepository<Activite, Long> {

    /**
     * Retourne les N activités les plus récentes d'un propriétaire.
     * Utilisé pour la section "Activité récente" du Dashboard.
     */
    List<Activite> findTop10ByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    /**
     * Retourne toutes les activités d'un propriétaire paginées.
     * Utilisé pour l'écran "Voir tout".
     */
    Page<Activite> findByProprietaireIdOrderByDateCreationDesc(
            Long proprietaireId, Pageable pageable);
}