package com.crm.modules.utilisateur.repository;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Riahi Dorsaf
 */
@Repository
public interface ProprietaireRepository extends JpaRepository<ProprietaireEntreprise, Long> {

    Optional<ProprietaireEntreprise> findByEmail(String email);

    /**
     * Retrouve le propriétaire lié à un EntrepriseCompte (relation 1-1 inverse).
     * Utilisé dans EntrepriseService pour enrichir les réponses.
     */
    @Query("SELECT p FROM ProprietaireEntreprise p " +
            "WHERE p.entrepriseCompte.id = :entrepriseId")
    Optional<ProprietaireEntreprise> findByEntrepriseCompteId(
            @Param("entrepriseId") Long entrepriseId);

    /**
     * Vérifie si un propriétaire avec cet email existe
     * et que son entreprise associée n'est pas supprimée.
     * Permet la réinscription après suppression logique du compte.
     * Justifie le @Query car la navigation sur relation imbriquée
     * n'est pas supportée par les méthodes dérivées Spring Data.
     */
    @Query("""
        SELECT COUNT(p) > 0 FROM ProprietaireEntreprise p
        WHERE p.email = :email
          AND (p.entrepriseCompte IS NULL
               OR p.entrepriseCompte.isDeleted = false)
    """)
    boolean existsByEmailAndCompteNonSupprime(@Param("email") String email);
}