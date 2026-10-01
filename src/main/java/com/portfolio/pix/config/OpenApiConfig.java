package com.portfolio.pix.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pixOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PIX Payments API")
                .description("Simulador de PSP com PIX: chaves, transferencia com controle de concorrencia e idempotencia")
                .version("1.0.0"));
    }
}
