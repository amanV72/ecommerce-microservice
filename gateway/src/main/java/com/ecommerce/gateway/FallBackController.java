package com.ecommerce.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
public class FallBackController {
    @GetMapping("/fallback/products")
    public ResponseEntity<List<String>> productsFallback(){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonList("Product Service is Under Maintenance"));
    }

    @GetMapping("/fallback/user")
    public ResponseEntity<List<String>> userFallback(){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonList("User Service is Under Maintenance"));
    }

    @GetMapping("/fallback/order")
    public ResponseEntity<List<String>> orderFallback(){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonList("Order Service is Under Maintenance"));
    }
}
