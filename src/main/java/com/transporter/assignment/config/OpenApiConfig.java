package com.transporter.assignment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI transporterAssignmentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Transporter Assignment & Cost Optimization API")
                .description("Backend API for lane quotation submission and optimal transporter assignment.")
                .version("1.0.0")
                .contact(new Contact().name("Transporter Assignment Support")));
    }
}
