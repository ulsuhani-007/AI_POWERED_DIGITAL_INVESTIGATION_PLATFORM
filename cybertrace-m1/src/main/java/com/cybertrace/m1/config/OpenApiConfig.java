package com.cybertrace.m1.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CyberTrace AI - Digital Investigation Platform API")
                        .version("1.0.0")
                        .description("Forensic Investigation Backend & Orchestration API. " +
                                "Provides case management, evidence integrity verification (SHA-256), " +
                                "normalized timeline persistence, and Module 2/3 orchestration.")
                        .contact(new Contact().name("CyberTrace Forensic Team").email("investigation@cybertrace.local"))
                        .license(new License().name("Proprietary").url("https://cybertrace.local/license")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("UUID / Secure Token")
                                        .description("Enter Bearer token returned from POST /api/auth/login")
                        )
                );
    }
}

