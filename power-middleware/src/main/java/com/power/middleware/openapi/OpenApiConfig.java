package com.power.middleware.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI（Swagger UI）公共配置：JWT Bearer + 基本信息。
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI powerOpenApi(
            @Value("${spring.application.name:power}") String appName,
            @Value("${power.openapi.title:}") String title,
            @Value("${power.openapi.description:flowable-power API}") String description,
            @Value("${power.openapi.version:1.0.0}") String version) {
        String resolvedTitle = (title == null || title.isBlank()) ? appName + " API" : title;
        return new OpenAPI()
                .info(new Info()
                        .title(resolvedTitle)
                        .description(description)
                        .version(version)
                        .contact(new Contact().name("flowable-power")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录后获得的 accessToken，填入时无需再加 Bearer 前缀")));
    }
}
