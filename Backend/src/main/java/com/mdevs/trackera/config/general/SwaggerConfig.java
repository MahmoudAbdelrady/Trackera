package com.mdevs.trackera.config.general;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI trackeraOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Trackera API")
                        .description("Worklog Calculator & Tracker API Documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MDevs")
                                .email("mdevs.lab@gmail.com")));
    }
}
