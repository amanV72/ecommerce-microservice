package com.ecommerce.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(
                10,
                20,
                1);
    }

    //    @Bean
//    public KeyResolver hostNameKeyResolver(){
//        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
//    }
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange ->
                Mono.justOrEmpty(exchange.getRequest()
                                .getHeaders()
                                .getFirst("X-User-ID"))
                        .switchIfEmpty(Mono.just("anonymous"));
    }


    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.retry(retryConfig -> retryConfig
                                        .setRetries(10)
                                        .setMethods(HttpMethod.GET)
                                )
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("ecomBreaker")
                                        .setFallbackUri("forward:/fallback/products")
                                ))
                        .uri("lb://PRODUCT-SERVICE")
                )
                .route("inventory-service",r->r
                        .path("/api/inventory/**")
//                        .filters(f-> f.circuitBreaker(config -> config
//                                .setName("ecomBreaker")
//                                .setFallbackUri("forward:/fallback/inventory")
//                        ))
                        .uri("lb://INVENTORY-SERVICE")
                )
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("ecomBreaker")
                                .setFallbackUri("forward:/fallback/user")
                        ))
                        .uri("lb://USER-SERVICE")
                )
                .route("payment-service",r->r
                        .path("/api/payments/**")
                        .uri("lb://PAYMENT-SERVICE")
                )
                .route("cart-service", r -> r
                        .path("/api/cart/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("ecomBreaker")
                                .setFallbackUri("forward:/fallback/order")
                        ))
                        .uri("lb://CART-SERVICE")
                )
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.circuitBreaker(config -> config
                                .setName("ecomBreaker")
                                .setFallbackUri("forward:/fallback/order")
                        ))
                        .uri("lb://ORDER-SERVICE")
                )
                .route("eureka-server", r -> r
                        .path("/eureka/main")
                        .filters(f -> f.rewritePath(
                                "/eureka/main",
                                "/"))
                        .uri("http://localhost:8761")
                )
                .route("eureka-server-static", r -> r
                        .path("/eureka/**")
                        .uri("http://localhost:8761")
                ).build();
    }
}
