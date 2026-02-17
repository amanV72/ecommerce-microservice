package com.ecommerce.order.clients;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Optional;

@Configuration
public class CartServiceClientConfig {

    @Bean
    public CartServiceClient cartServiceClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder){
        RestClient restClient= builder
                .baseUrl("http://cart-service")
                .defaultStatusHandler(HttpStatusCode::is4xxClientError,(req,res)-> Optional.empty())
                .build();
        RestClientAdapter adapter= RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory= HttpServiceProxyFactory
                .builderFor(adapter)
                .build();

        CartServiceClient cartServiceClient= factory.createClient(CartServiceClient.class);

        return cartServiceClient;

    }
}
