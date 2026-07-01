package com.crm.modules.marketing.security;

import com.crm.shared.exception.CallbackAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Garde d'authentification machine-à-machine pour les endpoints marketing
 * automatisés (appelés par n8n : qualification Messenger + collecte de
 * statistiques), sans JWT.
 *
 * <p>Vérifie l'en-tête {@code X-Callback-Secret} contre le secret dédié
 * {@code messenger.callback-secret} (externalisé). La comparaison est à temps
 * constant pour ne pas exposer la longueur/préfixe du secret via le timing.</p>
 *
 * <p>Secret distinct de {@code reporting.callback-secret} (moindre privilège :
 * le secret reporting et le secret marketing restent indépendants).</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Component
public class MarketingCallbackGuard {

    public static final String HEADER = "X-Callback-Secret";

    private final byte[] secretAttendu;

    public MarketingCallbackGuard(@Value("${messenger.callback-secret}") String secret) {
        this.secretAttendu = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Vérifie le secret fourni par l'appelant.
     *
     * @param secretFourni valeur de l'en-tête {@code X-Callback-Secret}
     * @throws CallbackAuthException si le secret est absent ou invalide
     */
    public void verifier(String secretFourni) {
        if (!StringUtils.hasText(secretFourni)
                || !MessageDigest.isEqual(secretAttendu, secretFourni.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Appel marketing automatisé rejeté : secret partagé absent ou invalide.");
            throw new CallbackAuthException("Secret d'appel absent ou invalide.");
        }
    }
}
