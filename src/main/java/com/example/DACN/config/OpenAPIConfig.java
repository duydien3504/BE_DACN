package com.example.DACN.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

        @Value("${openapi.dev-url:http://localhost:8080}")
        private String devUrl;

        @Value("${openapi.prod-url:}")
        private String prodUrl;

        @Bean
        public OpenAPI openAPI() {
                Server devServer = new Server();
                devServer.setUrl(devUrl);
                devServer.setDescription("Development Server");

                Contact contact = new Contact();
                contact.setEmail("your-email@example.com");
                contact.setName("DACN Team");
                contact.setUrl("https://github.com/your-repo");

                License license = new License()
                                .name("MIT License")
                                .url("https://choosealicense.com/licenses/mit/");

                Info info = new Info()
                                .title("DACN API Documentation")
                                .version("1.0.0")
                                .contact(contact)
                                .description("API documentation for DACN application with JWT authentication")
                                .license(license);

                SecurityScheme securityScheme = new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Enter JWT token only (without 'Bearer ' prefix - it will be added automatically)");

                SecurityRequirement securityRequirement = new SecurityRequirement()
                                .addList("Bearer Authentication");

                OpenAPI openAPI = new OpenAPI()
                                .info(info)
                                .servers(List.of(devServer))
                                .addSecurityItem(securityRequirement)
                                .components(new Components().addSecuritySchemes("Bearer Authentication",
                                                securityScheme));

                // Add production server if configured
                if (prodUrl != null && !prodUrl.isEmpty()) {
                        Server prodServer = new Server();
                        prodServer.setUrl(prodUrl);
                        prodServer.setDescription("Production Server");
                        openAPI.servers(List.of(devServer, prodServer));
                }

                return openAPI;
        }
}
