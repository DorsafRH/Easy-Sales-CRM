package com.crm.modules.catalogue.repository;

import com.crm.modules.catalogue.entity.Categorie;
import com.crm.modules.catalogue.specification.CategorieSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour l'entité {@link Categorie}.
 *
 * <p>{@link JpaSpecificationExecutor} activé pour le filtrage dynamique
 * via {@link CategorieSpecification}.</p>
 *
 * @author Riahi Dorsaf
 */
@Repository
public interface CategorieRepository
        extends JpaRepository<Categorie, Long>,
        JpaSpecificationExecutor<Categorie> {

    Optional<Categorie> findByIdAndProprietaireId(Long id, Long proprietaireId);

    boolean existsByNomIgnoreCaseAndProprietaireId(String nom, Long proprietaireId);
}