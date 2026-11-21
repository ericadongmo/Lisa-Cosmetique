package cm.lisacosmetique.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lisaCosmetiqueOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Lisa Cosmétique et Bien-être — API")
                .description("API REST de la boutique en ligne")
                .version("v1"));
    }
}
