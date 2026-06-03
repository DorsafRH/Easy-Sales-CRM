package com.crm.modules.marketing.repository;

import com.crm.modules.marketing.entity.CompteSocialConnecte;
import com.crm.shared.enums.TypeReseau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Zéro @Query — méthodes dérivées Spring Data uniquement.
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface CompteSocialConnecteRepository
        extends JpaRepository<CompteSocialConnecte, Long> {

    List<CompteSocialConnecte> findByProprietaireIdOrderByDateConnexionDesc(Long proprietaireId);

    Optional<CompteSocialConnecte> findByIdAndProprietaireId(Long id, Long proprietaireId);

    Optional<CompteSocialConnecte> findByProprietaireIdAndTypeReseau(
            Long proprietaireId, TypeReseau typeReseau);
}
