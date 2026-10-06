package com.sistemadelivery.main.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI apiDelivery() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Delivery API")
                        .description("""
                                API REST de comercio electrónico y delivery rápido.

                                **Autenticación:** obtenga el token en `POST /api/v1/auth/login`
                                y envíelo en cada petición protegida como
                                `Authorization: Bearer <token>`.

                                **Roles:** ADMIN, CLIENTE y REPARTIDOR.
                                """)
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT devuelto por /api/v1/auth/login")));
    }
}
