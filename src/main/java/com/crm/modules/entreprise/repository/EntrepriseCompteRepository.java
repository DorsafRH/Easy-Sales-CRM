package com.crm.modules.entreprise.repository;

import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.shared.enums.StatutCompte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntrepriseCompteRepository
        extends JpaRepository<EntrepriseCompte, Long>,
        JpaSpecificationExecutor<EntrepriseCompte> {

    boolean existsByMatriculeFiscaleAndIsDeletedFalse(String matriculeFiscale);

    Page<EntrepriseCompte> findByStatutCompteAndIsDeletedFalse(
            StatutCompte statut, Pageable pageable);

    Optional<EntrepriseCompte> findByIdAndIsDeletedFalse(Long id);
}