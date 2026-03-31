package com.crm.modules.entreprise.repository;

import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.shared.enums.StatutCompte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntrepriseCompteRepository extends JpaRepository<EntrepriseCompte, Long> {

    boolean existsByMatriculeFiscale(String matriculeFiscale);

    // DEV-20 : liste des comptes en attente (non supprimés)
    Page<EntrepriseCompte> findByStatutCompteAndIsDeletedFalse(StatutCompte statut, Pageable pageable);

    // DEV-53 : détails d'une entreprise non supprimée
    Optional<EntrepriseCompte> findByIdAndIsDeletedFalse(Long id);

    // Toutes les entreprises non supprimées (avec filtre optionnel par statut)
    @Query("""
        SELECT e FROM EntrepriseCompte e
        WHERE e.isDeleted = false
          AND (:statut IS NULL OR e.statutCompte = :statut)
    """)
    Page<EntrepriseCompte> findAllActiveWithFilter(
            @Param("statut") StatutCompte statut,
            Pageable pageable
    );

    // Recherche par nom ou matricule
    @Query("""
        SELECT e FROM EntrepriseCompte e
        WHERE e.isDeleted = false
          AND (LOWER(e.nomEntreprise) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(e.matriculeFiscale) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<EntrepriseCompte> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
