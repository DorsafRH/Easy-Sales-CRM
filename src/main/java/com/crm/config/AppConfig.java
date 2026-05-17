package com.crm.config;

import com.crm.config.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration globale de l'application.
 * - @EnableAsync   → active l'envoi d'emails asynchrone dans EmailService
 * - @EnableConfigurationProperties → lie JwtProperties à application.yml
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {
}
