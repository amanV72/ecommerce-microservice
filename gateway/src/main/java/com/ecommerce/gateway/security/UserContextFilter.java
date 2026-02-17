package com.ecommerce.gateway.security;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;

@Configuration
public class UserContextFilter {

    @Bean
    public GlobalFilter userIdHeaderFilter(){
        return (exchange, chain) ->
                ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .filter(auth -> auth instanceof JwtAuthenticationToken)
                        .cast(JwtAuthenticationToken.class)
                        .map(JwtAuthenticationToken::getToken)
                        .map(JwtClaimAccessor::getSubject)
                        .flatMap(userId ->{
                            ServerWebExchange mutatedExchange= exchange.mutate()
                                    .request(r->r.headers(h->h.set("X-User-ID",userId)))
                                    .build();
                            return chain.filter(mutatedExchange);
                        })
                        .switchIfEmpty(chain.filter(exchange));
    }
}
