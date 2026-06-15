package com.crm.modules.vente.repository;

import com.crm.modules.vente.entity.Facture;
import com.crm.shared.enums.StatutFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Riahi Dorsaf
 */
@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByIdAndProprietaireId(Long id, Long proprietaireId);

    List<Facture> findByProprietaireIdOrderByDateCreationDesc(Long proprietaireId);

    long countByProprietaireIdAndStatut(Long proprietaireId, StatutFacture statut);

    Optional<Facture> findTopByProprietaireIdAndNumeroStartingWithOrderByNumeroDesc(
            Long proprietaireId, String prefixe);

    List<Facture> findByProprietaireIdAndStatut(Long proprietaireId, StatutFacture statut);

    Optional<Facture> findByDevisOrigineId(Long devisId);

    /**
     * Indique si une facture a déjà été générée à partir du devis donné.
     * Sert à masquer le bouton « Convertir en facture » côté mobile et à
     * empêcher toute reconversion côté serveur.
     */
    boolean existsByDevisOrigineId(Long devisId);

    /**
     * Retourne les factures d'un propriétaire avec un statut donné et datePaiement
     * dans une fenêtre temporelle — utilisé pour calculer le CA sur une période.
     */
    List<Facture> findByProprietaireIdAndStatutAndDatePaiementBetween(
            Long proprietaireId, StatutFacture statut,
            LocalDateTime debut, LocalDateTime fin);

    /**
     * Retourne les factures d'un propriétaire dont le statut est dans la liste fournie.
     * Utilisé pour les factures impayées du rapport (EMISE / LIVREE / EN_RETARD).
     */
    List<Facture> findByProprietaireIdAndStatutIn(
            Long proprietaireId, Collection<StatutFacture> statuts);
}