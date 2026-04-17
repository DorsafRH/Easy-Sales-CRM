package com.crm.modules.auth.repository;

import com.crm.modules.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Riahi Dorsaf
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Invalide tous les tokens actifs d'un utilisateur avant d'en créer un nouveau.
     * Justifie le @Query : mise à jour en masse sur relation imbriquée.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.utilise = true " +
            "WHERE t.utilisateur.id = :utilisateurId AND t.utilise = false")
    void invaliderTokensExistants(@Param("utilisateurId") Long utilisateurId);
}