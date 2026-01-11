package com.ecommerce.order.clients;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Optional;

@Configuration
public class ProductServiceClientConfig {



    @Bean
    public ProductServiceClient restClientInterfaceProduct(RestClient.Builder restClientBuilder){
        RestClient restClient=restClientBuilder
                .baseUrl("http://product-service")
                .defaultStatusHandler(HttpStatusCode::is4xxClientError,(request,response)-> Optional.empty())
                .build();
        RestClientAdapter restClientAdapter= RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory=HttpServiceProxyFactory
                .builderFor(restClientAdapter)
                .build();

        ProductServiceClient productServiceClient= factory.createClient(ProductServiceClient.class);

        return productServiceClient;


    }
}
