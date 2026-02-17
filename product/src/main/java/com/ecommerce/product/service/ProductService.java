package com.ecommerce.product.service;


import com.ecommerce.product.dto.ProductEventResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repositories.ProductRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepo productRepo;

    private final StreamBridge streamBridge;


    public void productRequestToProduct(Product product, ProductRequest productRequest) {
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setCategory(productRequest.getCategory());
        product.setPrice(productRequest.getPrice());
      //  product.setStockQuantity(productRequest.getStockQuantity());
        product.setImageUrl(productRequest.getImageUrl());
    }

    public ProductResponse productToProductResponse(Product product) {
        ProductResponse productResponse = new ProductResponse();
        productResponse.setName(product.getName());
        productResponse.setCategory(product.getCategory());
        productResponse.setDescription(product.getDescription());
        productResponse.setPrice(product.getPrice());
     //   productResponse.setStockQuantity(product.getStockQuantity());
        productResponse.setActive(product.getActive());
        productResponse.setId(String.valueOf(product.getId()));
        productResponse.setImageUrl(product.getImageUrl());

        return productResponse;
    }


    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = new Product();
        productRequestToProduct(product, productRequest);
        Product savedProduct = productRepo.save(product);
        ProductEventResponse event= new ProductEventResponse(savedProduct.getId());
        streamBridge.send("productCreated-out-0",event);
        return productToProductResponse(savedProduct);

    }

    public Optional<ProductResponse> updateProduct(Long id, ProductRequest productRequest) {
        return productRepo.findById(id)
                .map(existingProduct -> {
                    productRequestToProduct(existingProduct, productRequest);
                    Product savedProduct = productRepo.save(existingProduct);
                    return productToProductResponse(savedProduct);

                });
    }

    public List<ProductResponse> fetchAllProduct() {
        return productRepo.findByActiveTrue().stream()
                .map(this::productToProductResponse)
                .collect(Collectors.toList());

    }

    public boolean deleteProduct(Long id) {

        return productRepo.findById(id).map(product -> {
            product.setActive(false);
            productRepo.save(product);
            return true;
        }).orElse(false);
    }

    public List<ProductResponse> findProducts(String keyword) {
        return productRepo.searchProducts(keyword).stream().map(this::productToProductResponse).collect(Collectors.toList());
    }

    public Optional<ProductResponse> getProductById(String id) {
        return productRepo.findByIdAndActiveTrue(Long.valueOf(id)).map(this::productToProductResponse);
    }
}
