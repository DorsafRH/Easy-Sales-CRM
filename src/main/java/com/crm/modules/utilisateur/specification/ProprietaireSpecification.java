package com.crm.modules.utilisateur.specification;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import org.springframework.data.jpa.domain.Specification;

/**
 * Classe utilitaire de spécifications JPA pour l'entité {@link ProprietaireEntreprise}.
 *
 * <p>Fournit des prédicats réutilisables pour construire des requêtes dynamiques
 * sans JPQL ni méthodes dérivées Spring Data.</p>
 *
 * @author Riahi Dorsaf
 *
 */
public class ProprietaireSpecification {

    /** Constructeur privé : classe utilitaire non instanciable. */
    private ProprietaireSpecification() {}

    /**
     * Filtre les propriétaires liés à un {@code EntrepriseCompte} donné.
     *
     * <p>Permet de naviguer la relation inverse 1-1 {@code ProprietaireEntreprise → EntrepriseCompte}
     * sans écrire de requête JPQL explicite.</p>
     *
     * @param entrepriseId l'identifiant du compte entreprise associé
     * @return spécification vérifiant {@code entrepriseCompte.id = entrepriseId}
     */
    public static Specification<ProprietaireEntreprise> hasEntrepriseCompteId(Long entrepriseId) {
        return (root, query, cb) ->
                cb.equal(root.get("entrepriseCompte").get("id"), entrepriseId);
    }
}
