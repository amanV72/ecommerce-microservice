package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductEventResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repositories.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private  ProductRepo productRepo;
    @Mock
    private  StreamBridge streamBridge;

    @InjectMocks
    private ProductService productService;

    private ProductRequest productRequest;
    private Product product;
  //  private ProductResponse productResponse;

    @BeforeEach
    void setUp() {

        productRequest = new ProductRequest();

        productRequest.setName("Laptop");
        productRequest.setDescription("Gaming Laptop");
        productRequest.setCategory("Electronics");
        productRequest.setPrice(new BigDecimal("999.99"));
        productRequest.setImageUrl("image.jpg");

        product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setDescription("Gaming Laptop");
        product.setCategory("Electronics");
        product.setPrice(99999L);
        product.setImageUrl("image.jpg");
        product.setActive(true);
    }

    @Nested
    class CreateProductTests{
        @Test
        void shouldCreateProductSuccessfully() {

            Product savedProduct = product;

            when(productRepo.save(any(Product.class)))
                    .thenReturn(savedProduct);

            when(streamBridge.send(
                    eq("productCreated-out-0"),
                    any(ProductEventResponse.class)
            )).thenReturn(true);

            ProductResponse response =
                    productService.createProduct(productRequest);

            assertNotNull(response);

            assertEquals("Laptop", response.getName());
            assertEquals(99999L, response.getPrice());
            assertEquals("1", response.getId());

            verify(productRepo, times(1))
                    .save(any(Product.class));

            verify(streamBridge, times(1))
                    .send(
                            eq("productCreated-out-0"),
                            any(ProductEventResponse.class)
                    );
        }

        @Test
        void shouldFailWhenSavingProductFails() {

            when(productRepo.save(any(Product.class)))
                    .thenThrow(new RuntimeException("Database error"));

            assertThrows(
                    RuntimeException.class,
                    () -> productService.createProduct(productRequest)
            );

            verify(productRepo).save(any(Product.class));

            verify(streamBridge, never())
                    .send(anyString(), any());
        }

        @Test
        void shouldFailWhenProductEventCannotBeSent() {

            when(productRepo.save(any(Product.class)))
                    .thenReturn(product);

            when(streamBridge.send(
                    eq("productCreated-out-0"),
                    any(ProductEventResponse.class)
            )).thenThrow(
                    new RuntimeException("Kafka publishing failed")
            );

            assertThrows(
                    RuntimeException.class,
                    () -> productService.createProduct(productRequest)
            );

            verify(productRepo).save(any(Product.class));

            verify(streamBridge).send(
                    eq("productCreated-out-0"),
                    any(ProductEventResponse.class)
            );
        }
    }

    @Nested
    class UpdateProductTests{
        @Test
        void shouldUpdateProductSuccessfully() {

            Long id = 1L;

            when(productRepo.findById(id))
                    .thenReturn(Optional.of(product));

            when(productRepo.save(any(Product.class)))
                    .thenReturn(product);

            Optional<ProductResponse> response =
                    productService.updateProduct(id, productRequest);

            assertTrue(response.isPresent());

            assertEquals("Laptop", response.get().getName());

            verify(productRepo).findById(id);

            verify(productRepo).save(any(Product.class));
        }

        @Test
        void shouldReturnEmptyWhenUpdatingNonExistingProduct() {

            Long id = 100L;

            when(productRepo.findById(id))
                    .thenReturn(Optional.empty());

            Optional<ProductResponse> response =
                    productService.updateProduct(id, productRequest);

            assertTrue(response.isEmpty());

            verify(productRepo).findById(id);

            verify(productRepo, never())
                    .save(any(Product.class));
        }
    }

    @Nested
    class FetchProductTests{
        @Test
        void shouldFetchAllActiveProducts() {

            Product product2 = new Product();

            product2.setId(2L);
            product2.setName("Phone");
            product2.setCategory("Electronics");
            product2.setDescription("Smartphone");
            product2.setPrice(50000L);
            product2.setImageUrl("phone.jpg");
            product2.setActive(true);

            when(productRepo.findByActiveTrue())
                    .thenReturn(List.of(product, product2));

            List<ProductResponse> response =
                    productService.fetchAllProduct();

            assertEquals(2, response.size());

            assertEquals("Laptop", response.get(0).getName());
            assertEquals("Phone", response.get(1).getName());

            verify(productRepo).findByActiveTrue();
        }

        @Test
        void shouldReturnEmptyListWhenNoActiveProductsExist() {

            when(productRepo.findByActiveTrue())
                    .thenReturn(List.of());

            List<ProductResponse> response =
                    productService.fetchAllProduct();

            assertNotNull(response);
            assertTrue(response.isEmpty());

            verify(productRepo).findByActiveTrue();
        }

        @Test
        void shouldFindProductsByKeyword() {

            when(productRepo.searchProducts("laptop"))
                    .thenReturn(List.of(product));

            List<ProductResponse> response =
                    productService.findProducts("laptop");

            assertEquals(1, response.size());

            assertEquals(
                    "Laptop",
                    response.get(0).getName()
            );

            verify(productRepo).searchProducts("laptop");
        }

        @Test
        void shouldReturnEmptyListWhenNoProductsMatchKeyword() {

            when(productRepo.searchProducts("xyz"))
                    .thenReturn(List.of());

            List<ProductResponse> response =
                    productService.findProducts("xyz");

            assertNotNull(response);
            assertTrue(response.isEmpty());

            verify(productRepo).searchProducts("xyz");
        }

        @Test
        void shouldGetProductByIdSuccessfully() {

            when(productRepo.findByIdAndActiveTrue(1L))
                    .thenReturn(Optional.of(product));

            Optional<ProductResponse> response =
                    productService.getProductById("1");

            assertTrue(response.isPresent());

            assertEquals(
                    "Laptop",
                    response.get().getName()
            );

            assertEquals(
                    "1",
                    response.get().getId()
            );

            verify(productRepo)
                    .findByIdAndActiveTrue(1L);
        }

        @Test
        void shouldReturnEmptyWhenProductDoesNotExist() {

            when(productRepo.findByIdAndActiveTrue(100L))
                    .thenReturn(Optional.empty());

            Optional<ProductResponse> response =
                    productService.getProductById("100");

            assertTrue(response.isEmpty());

            verify(productRepo)
                    .findByIdAndActiveTrue(100L);
        }
    }

    @Nested
    class DeleteProductTests{
        @Test
        void shouldDeleteProductSuccessfully() {

            Long id = 1L;

            when(productRepo.findById(id))
                    .thenReturn(Optional.of(product));

            boolean result =
                    productService.deleteProduct(id);

            assertTrue(result);

            assertFalse(product.getActive());

            verify(productRepo).findById(id);

            verify(productRepo).save(product);
        }

        @Test
        void shouldReturnFalseWhenDeletingNonExistingProduct() {

            Long id = 100L;

            when(productRepo.findById(id))
                    .thenReturn(Optional.empty());

            boolean result =
                    productService.deleteProduct(id);

            assertFalse(result);

            verify(productRepo).findById(id);

            verify(productRepo, never())
                    .save(any(Product.class));
        }
    }





}