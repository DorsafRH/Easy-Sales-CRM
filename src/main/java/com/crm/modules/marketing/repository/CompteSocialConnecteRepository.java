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

    /**
     * Résolution du propriétaire (multi-tenant) à partir de l'identifiant de page
     * Meta reçu dans un webhook n8n — clé : {@code identifiantExterne == pageId}.
     * {@code findFirst} : robuste même si une page a été connectée plusieurs fois
     * (doublons résiduels) — on prend une connexion valide quelconque (même propriétaire).
     */
    Optional<CompteSocialConnecte> findFirstByIdentifiantExterneAndTypeReseau(
            String identifiantExterne, TypeReseau typeReseau);

    /**
     * Toutes les connexions d'une page donnée pour un propriétaire — utilisé par
     * l'OAuth pour faire un upsert + dédoublonnage à la reconnexion.
     */
    List<CompteSocialConnecte> findByProprietaireIdAndIdentifiantExterneAndTypeReseau(
            Long proprietaireId, String identifiantExterne, TypeReseau typeReseau);

    List<CompteSocialConnecte> findByTypeReseau(TypeReseau typeReseau);
}
