package com.crm.shared.enums;

/**
 * Période d'un rapport commercial automatisé.
 *
 * <p>Chaque valeur désigne la <b>période complète écoulée</b> (jamais la période
 * en cours) afin que le bilan porte sur des données figées :</p>
 * <ul>
 *   <li>{@link #SEMAINE} → la semaine précédente (lundi 00:00 → dimanche 23:59:59) ;</li>
 *   <li>{@link #MOIS} → le mois calendaire précédent ;</li>
 *   <li>{@link #ANNEE} → l'année civile précédente.</li>
 * </ul>
 *
 * <p>La fenêtre temporelle (et la fenêtre précédente, pour la variation %) est
 * calculée dans {@code RapportCommercialService}.</p>
 *
 * @author Riahi Dorsaf
 */
public enum PeriodeRapport {
    SEMAINE,
    MOIS,
    ANNEE
}
