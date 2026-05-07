package com.crm.modules.reporting.service;

import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.TypeActivite;

/**
 * Contrat du service d'enregistrement des activités CRM.
 *
 * @author Riahi Dorsaf
 * @see ActiviteService
 */
public interface IActiviteService {

    /**
     * Enregistre une activité de manière asynchrone et dans une
     * transaction indépendante.
     *
     * @param type            type de l'action métier
     * @param titre           libellé principal affiché dans l'UI
     * @param description     description de l'action
     * @param entiteId        ID de l'entité concernée
     * @param entiteType      type de l'entité ("CLIENT", "CONTACT", "PRODUIT")
     * @param entiteParentId  ID de l'entité parente — null si absent.
     *                        Pour les contacts : clientId pour la navigation mobile.
     * @param proprietaire    propriétaire connecté
     */
    void enregistrer(TypeActivite type,
                     String titre,
                     String description,
                     Long entiteId,
                     String entiteType,
                     Long entiteParentId,
                     ProprietaireEntreprise proprietaire);
}