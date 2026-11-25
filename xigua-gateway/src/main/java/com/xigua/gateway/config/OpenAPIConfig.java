package com.xigua.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName OpenAPIConfig
 * @Description
 * @Author wangjinfei
 * @Date 2025/3/28 9:15
 */
@OpenAPIDefinition
@Configuration
public class OpenAPIConfig {
    /*
    * http://localhost:8090/swagger-ui/index.html swagger网关统一访问路径
    * 转发到具体的服务接口路径
    */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API Gateway Service")
                .description("API Gateway Service")
                .version("1.0.0"));
    }

    @Bean
    public RouteLocator swaggerRoute(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("client-api", r -> r.path("/client/v3/api-docs")
                        .filters(f -> f.rewritePath("/client/(?<segment>.*)", "/client/${segment}"))
                        .uri("http://127.0.0.1:8084"))

                .route("center-api", r -> r.path("/center/v3/api-docs")
                        .filters(f -> f.rewritePath("/center/(?<segment>.*)", "/center/${segment}"))
                        .uri("http://127.0.0.1:8083"))

                .route("ai-api", r -> r.path("/ai/v3/api-docs")
                        .filters(f -> f.rewritePath("/ai/(?<segment>.*)", "/ai/${segment}"))
                        .uri("http://127.0.0.1:8085"))

                .route("admin-api", r -> r.path("/admin/v3/api-docs")
                        .filters(f -> f.rewritePath("/admin/(?<segment>.*)", "/admin/${segment}"))
                        .uri("http://127.0.0.1:8086"))

                .route("search-api", r -> r.path("/search/v3/api-docs")
                        .filters(f -> f.rewritePath("/search/(?<segment>.*)", "/search/${segment}"))
                        .uri("http://127.0.0.1:8087"))

                .build();
    }
}
