package com.example.DACN.service;

import com.example.DACN.dto.response.ProductListItemResponse;
import com.example.DACN.dto.response.ProductListResponse;
import com.example.DACN.entity.Category;
import com.example.DACN.entity.Product;
import com.example.DACN.entity.Shop;
import com.example.DACN.mapper.ProductMapper;
import com.example.DACN.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService - Get Products Tests")
class PublicProductServiceTest {

        @Mock
        private ProductRepository productRepository;

        @Mock
        private ProductMapper productMapper;

        @InjectMocks
        private ProductService productService;

        private Product product1;
        private Product product2;
        private ProductListItemResponse productResponse1;
        private ProductListItemResponse productResponse2;
        private Shop shop;
        private Category category;

        @BeforeEach
        void setUp() {
                shop = new Shop();
                shop.setShopId(1L);
                shop.setShopName("Test Shop");

                category = new Category();
                category.setCategoryId(1L);
                category.setName("Test Category");

                product1 = new Product();
                product1.setProductId(1L);
                product1.setName("Product 1");
                product1.setDescription("Description 1");
                product1.setPrice(new BigDecimal("99.99"));
                product1.setStockQuantity(10);
                product1.setSoldCount(5);
                product1.setStatus("Active");
                product1.setCreatedAt(LocalDateTime.now());
                product1.setShop(shop);
                product1.setCategory(category);
                product1.setHasDeleted(false);

                product2 = new Product();
                product2.setProductId(2L);
                product2.setName("Product 2");
                product2.setDescription("Description 2");
                product2.setPrice(new BigDecimal("149.99"));
                product2.setStockQuantity(20);
                product2.setSoldCount(10);
                product2.setStatus("Active");
                product2.setCreatedAt(LocalDateTime.now());
                product2.setShop(shop);
                product2.setCategory(category);
                product2.setHasDeleted(false);

                productResponse1 = ProductListItemResponse.builder()
                                .productId(1L)
                                .name("Product 1")
                                .price(new BigDecimal("99.99"))
                                .shopId(1L)
                                .shopName("Test Shop")
                                .categoryId(1L)
                                .categoryName("Test Category")
                                .build();

                productResponse2 = ProductListItemResponse.builder()
                                .productId(2L)
                                .name("Product 2")
                                .price(new BigDecimal("149.99"))
                                .shopId(1L)
                                .shopName("Test Shop")
                                .categoryId(1L)
                                .categoryName("Test Category")
                                .build();
        }

        // Core Logic Tests

        @Test
        @DisplayName("Should return products list successfully")
        void testGetProductsSuccess() {
                // Given
                List<Product> products = Arrays.asList(product1, product2);
                Page<Product> productPage = new PageImpl<>(products);

                when(productRepository.findProductsWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                                .thenReturn(productPage);
                when(productMapper.toProductListItemResponse(product1)).thenReturn(productResponse1);
                when(productMapper.toProductListItemResponse(product2)).thenReturn(productResponse2);

                // When
                ProductListResponse result = productService.getProducts(null, null, null, null, "created_at", 0, 20);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getData()).hasSize(2);
                assertThat(result.getTotalPage()).isEqualTo(1);
                assertThat(result.getData().get(0).getProductId()).isEqualTo(1L);
                assertThat(result.getData().get(1).getProductId()).isEqualTo(2L);

                verify(productRepository).findProductsWithFilters(any(), any(), any(), any(), any(Pageable.class));
                verify(productMapper, times(2)).toProductListItemResponse(any(Product.class));
        }

        @Test
        @DisplayName("Should filter by price range")
        void testGetProductsWithPriceFilter() {
                // Given
                List<Product> products = Arrays.asList(product1);
                Page<Product> productPage = new PageImpl<>(products);

                when(productRepository.findProductsWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                                .thenReturn(productPage);
                when(productMapper.toProductListItemResponse(product1)).thenReturn(productResponse1);

                // When
                BigDecimal minPrice = new BigDecimal("50.00");
                BigDecimal maxPrice = new BigDecimal("150.00");
                ProductListResponse result = productService.getProducts(minPrice, maxPrice, null, null, "created_at", 0,
                                20);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getData()).hasSize(1);

                verify(productRepository).findProductsWithFilters(eq(minPrice), eq(maxPrice), isNull(), isNull(),
                                any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter by category")
        void testGetProductsWithCategoryFilter() {
                // Given
                List<Product> products = Arrays.asList(product1, product2);
                Page<Product> productPage = new PageImpl<>(products);

                when(productRepository.findProductsWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                                .thenReturn(productPage);
                when(productMapper.toProductListItemResponse(any(Product.class)))
                                .thenReturn(productResponse1, productResponse2);

                // When
                ProductListResponse result = productService.getProducts(null, null, 1L, null, "created_at", 0, 20);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getData()).hasSize(2);

                verify(productRepository).findProductsWithFilters(isNull(), isNull(), eq(1L), isNull(),
                                any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter by shop")
        void testGetProductsWithShopFilter() {
                // Given
                List<Product> products = Arrays.asList(product1, product2);
                Page<Product> productPage = new PageImpl<>(products);

                when(productRepository.findProductsWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                                .thenReturn(productPage);
                when(productMapper.toProductListItemResponse(any(Product.class)))
                                .thenReturn(productResponse1, productResponse2);

                // When
                ProductListResponse result = productService.getProducts(null, null, null, 1L, "created_at", 0, 20);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getData()).hasSize(2);

                verify(productRepository).findProductsWithFilters(isNull(), isNull(), isNull(), eq(1L),
                                any(Pageable.class));
        }

        @Test
        @DisplayName("Should sort by sold_count")
        void testGetProductsSortBySoldCount() {
                // Given
                List<Product> products = Arrays.asList(product2, product1); // product2 has higher soldCount
                Page<Product> productPage = new PageImpl<>(products);

                when(productRepository.findProductsWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                                .thenReturn(productPage);
                when(productMapper.toProductListItemResponse(any(Product.class)))
                                .thenReturn(productResponse2, productResponse1);

                // When
                ProductListResponse result = productService.getProducts(null, null, null, null, "sold_count", 0, 20);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getData()).hasSize(2);

                verify(productRepository).findProductsWithFilters(any(), any(), any(), any(),
                                argThat(pageable -> pageable.getSort().toString().contains("soldCount")));
        }

        @Test
        @DisplayName("Should return empty list when no products found")
        void testGetProductsEmptyList() {
                // Given
                Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());

                when(productRepository.findProductsWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                                .thenReturn(emptyPage);

                // When
                ProductListResponse result = productService.getProducts(null, null, null, null, "created_at", 0, 20);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getData()).isEmpty();
                assertThat(result.getTotalPage()).isEqualTo(1); // Spring returns 1 page for empty results

                verify(productMapper, never()).toProductListItemResponse(any(Product.class));
        }

        @Test
        @DisplayName("Should calculate total pages correctly")
        void testGetProductsTotalPages() {
                // Given
                List<Product> products = Arrays.asList(product1, product2);
                Page<Product> productPage = new PageImpl<>(products, Pageable.ofSize(1), 2);

                when(productRepository.findProductsWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                                .thenReturn(productPage);
                when(productMapper.toProductListItemResponse(any(Product.class)))
                                .thenReturn(productResponse1, productResponse2);

                // When
                ProductListResponse result = productService.getProducts(null, null, null, null, "created_at", 0, 1);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getTotalPage()).isEqualTo(2);
        }
}
