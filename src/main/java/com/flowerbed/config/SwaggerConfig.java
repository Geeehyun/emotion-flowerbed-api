package com.flowerbed.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("일기 화단 API")
                        .description("감정 기반 꽃 화단 꾸미기 서비스 API 문서")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Flowerbed Team")
                                .email("wkdwl578@google.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Local Server")
                ));
    }
}
