package com.ecommerce.cart.clients;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Optional;

@Configuration
public class InventoryServiceClientConfig {

    @Bean
    public InventoryServiceClient restClientInterfaceInventory(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder){

        RestClient client= builder
                .baseUrl("http://inventory-service")
                .defaultStatusHandler(HttpStatusCode::is4xxClientError,(request, response) -> Optional.empty())
                .build();
        RestClientAdapter adapter=RestClientAdapter.create(client);

        HttpServiceProxyFactory factory= HttpServiceProxyFactory
                .builderFor(adapter)
                .build();
        InventoryServiceClient inventoryServiceClient= factory.createClient(InventoryServiceClient.class);

        return inventoryServiceClient;

    }
}
