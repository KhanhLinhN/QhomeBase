package com.QhomeBase.financebillingservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI financeBillingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QhomeBase Finance & Billing APIs")
                        .description("Billing cycle, invoice, and payment APIs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QhomeBase Finance Team")
                                .email("support@qhomebase.com"))
                        .license(new License()
                                .name("Private")
                                .url("https://qhomebase.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("QhomeBase API Portal")
                        .url("https://docs.qhomebase.com"));
    }

    @Bean
    public GroupedOpenApi billingApi() {
        return GroupedOpenApi.builder()
                .group("finance-billing")
                .pathsToMatch("/api/**")
                .build();
    }
}
package com.QhomeBase.financebillingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.api-gateway-url:http://localhost:8989/finance-billing}")
    private String apiGatewayUrl;

    @Bean
    public OpenAPI financeServiceOpenAPI() {
        Server apiGatewayServer = new Server();
        apiGatewayServer.setUrl(apiGatewayUrl);
        apiGatewayServer.setDescription("API Gateway URL");

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8085");
        localServer.setDescription("Local Development Server");

        return new OpenAPI()
                .info(new Info()
                        .title("Finance Billing Service API")
                        .description("API for managing invoices, billing cycles, and parking fees")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QhomeBase Team")
                                .email("support@qhomebase.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(apiGatewayServer, localServer));
    }
}




