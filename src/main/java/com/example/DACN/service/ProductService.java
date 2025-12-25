package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.dto.request.CreateProductRequest;
import com.example.DACN.dto.request.UpdateProductRequest;
import com.example.DACN.dto.request.UpdateProductStatusRequest;
import com.example.DACN.dto.response.CreateProductResponse;
import com.example.DACN.dto.response.DeleteProductResponse;
import com.example.DACN.dto.response.ProductDetailResponse;
import com.example.DACN.dto.response.ProductListItemResponse;
import com.example.DACN.dto.response.ProductListResponse;
import com.example.DACN.dto.response.UpdateProductResponse;
import com.example.DACN.dto.response.UpdateProductStatusResponse;
import com.example.DACN.entity.*;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.exception.UnauthorizedException;
import com.example.DACN.mapper.ProductMapper;
import com.example.DACN.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductMapper productMapper;

    private static final int MAX_IMAGES = 9;

    @Transactional
    public CreateProductResponse createProduct(String email, CreateProductRequest request, List<MultipartFile> images)
            throws IOException {
        log.info("Creating product for user: {}", email);

        // Validate user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate user is a seller
        if (!user.getRole().getRoleName().equals(RoleConstants.SELLER)) {
            throw new UnauthorizedException("Only sellers can create products");
        }

        // Validate seller has a shop
        Shop shop = shopRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller does not have a shop"));

        // Validate shop is approved
        if (!shop.getIsApproved()) {
            throw new IllegalStateException("Shop must be approved before creating products");
        }

        // Validate category exists and is not deleted
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getHasDeleted()) {
            throw new IllegalStateException("Cannot create product with deleted category");
        }

        // Validate images
        if (images != null && !images.isEmpty()) {
            if (images.size() > MAX_IMAGES) {
                throw new IllegalArgumentException("Maximum " + MAX_IMAGES + " images allowed");
            }
        }

        // Upload images to Cloudinary
        List<String> imageUrls = null;
        if (images != null && !images.isEmpty()) {
            imageUrls = cloudinaryService.uploadProductImages(images);
        }

        // Create product entity
        Product product = new Product();
        product.setShop(shop);
        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setSoldCount(0);
        product.setStatus("Active");
        product.setHasDeleted(false);

        // Save product
        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getProductId());

        // Create product images
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                ProductImage productImage = new ProductImage();
                productImage.setProduct(savedProduct);
                productImage.setImageUrl(imageUrls.get(i));
                productImage.setDisplayOrder(i);
                productImageRepository.save(productImage);
            }
            log.info("Created {} product images", imageUrls.size());
        }

        return productMapper.toCreateProductResponse(savedProduct);
    }

    @Transactional
    public UpdateProductResponse updateProduct(String email, Long productId, UpdateProductRequest request,
            List<MultipartFile> images)
            throws IOException {
        log.info("Updating product {} for user: {}", productId, email);

        // Validate user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate user is a seller
        if (!user.getRole().getRoleName().equals(RoleConstants.SELLER)) {
            throw new UnauthorizedException("Only sellers can update products");
        }

        // Validate seller has a shop
        Shop shop = shopRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller does not have a shop"));

        // Validate product exists and is not deleted
        Product product = productRepository.findByProductIdAndHasDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Validate product belongs to seller's shop
        if (!product.getShop().getShopId().equals(shop.getShopId())) {
            throw new UnauthorizedException("You can only update your own products");
        }

        // Validate category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            if (category.getHasDeleted()) {
                throw new IllegalStateException("Cannot update product with deleted category");
            }

            product.setCategory(category);
        }

        // Validate images
        if (images != null && !images.isEmpty()) {
            if (images.size() > MAX_IMAGES) {
                throw new IllegalArgumentException("Maximum " + MAX_IMAGES + " images allowed");
            }
        }

        // Handle image updates
        if (images != null && !images.isEmpty()) {
            // Fetch existing images
            List<ProductImage> existingImages = productImageRepository
                    .findByProductProductIdOrderByDisplayOrderAsc(productId);

            // Delete old images from Cloudinary
            for (ProductImage existingImage : existingImages) {
                cloudinaryService.deleteImage(existingImage.getImageUrl());
            }

            // Delete old image records from database
            productImageRepository.deleteByProductProductId(productId);

            // Upload new images to Cloudinary
            List<String> imageUrls = cloudinaryService.uploadProductImages(images);

            // Create new product images
            for (int i = 0; i < imageUrls.size(); i++) {
                ProductImage productImage = new ProductImage();
                productImage.setProduct(product);
                productImage.setImageUrl(imageUrls.get(i));
                productImage.setDisplayOrder(i);
                productImageRepository.save(productImage);
            }
            log.info("Updated {} product images", imageUrls.size());
        }

        // Update product fields
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        // Save updated product
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated with ID: {}", updatedProduct.getProductId());

        return productMapper.toUpdateProductResponse(updatedProduct);
    }

    @Transactional
    public DeleteProductResponse deleteProduct(String email, Long productId) {
        log.info("Deleting product {} for user: {}", productId, email);

        // Validate user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate user is a seller
        if (!user.getRole().getRoleName().equals(RoleConstants.SELLER)) {
            throw new UnauthorizedException("Only sellers can delete products");
        }

        // Validate seller has a shop
        Shop shop = shopRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller does not have a shop"));

        // Validate product exists and is not already deleted
        Product product = productRepository.findByProductIdAndHasDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Validate product belongs to seller's shop
        if (!product.getShop().getShopId().equals(shop.getShopId())) {
            throw new UnauthorizedException("You can only delete your own products");
        }

        // Soft delete the product
        product.setHasDeleted(true);
        Product deletedProduct = productRepository.save(product);
        log.info("Product soft deleted with ID: {}", deletedProduct.getProductId());

        return productMapper.toDeleteProductResponse(deletedProduct);
    }

    public ProductListResponse getProducts(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long categoryId,
            Long shopId,
            String sortBy,
            int page,
            int size) {

        log.info(
                "Getting products with filters - minPrice: {}, maxPrice: {}, categoryId: {}, shopId: {}, sortBy: {}, page: {}, size: {}",
                minPrice, maxPrice, categoryId, shopId, sortBy, page, size);

        // Create sort based on sortBy parameter
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // Default sort by created_at
        if ("sold_count".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "soldCount");
        }

        // Create pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get products with filters
        Page<Product> productPage = productRepository.findProductsWithFilters(
                minPrice, maxPrice, categoryId, shopId, "Active", pageable);

        // Map to response
        List<ProductListItemResponse> productList = productPage.getContent().stream()
                .map(productMapper::toProductListItemResponse)
                .collect(Collectors.toList());

        int totalPages = productPage.getTotalPages();

        log.info("Found {} products, total pages: {}", productList.size(), totalPages);

        return ProductListResponse.builder()
                .data(productList)
                .totalPage(totalPages)
                .build();
    }

    public ProductDetailResponse getProductById(Long productId) {
        log.info("Getting product details for ID: {}", productId);

        // Find product by ID and ensure it's not deleted
        Product product = productRepository.findByProductIdAndHasDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Map to response
        ProductDetailResponse response = productMapper.toProductDetailResponse(product);

        // Get product images and extract URLs
        List<String> imageUrls = productImageRepository
                .findByProductProductIdOrderByDisplayOrderAsc(productId)
                .stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());

        response.setImages(imageUrls);

        log.info("Found product: {} with {} images", product.getName(), imageUrls.size());

        return response;
    }

    public ProductListResponse getProductsByShopId(Long shopId, int page, int size) {
        log.info("Getting products for shop ID: {}, page: {}, size: {}", shopId, page, size);

        // Create pageable with default sort by created_at descending
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get products for the shop
        Page<Product> productPage = productRepository.findByShopShopIdAndHasDeletedFalse(shopId, pageable);

        // Map to response
        List<ProductListItemResponse> productList = productPage.getContent().stream()
                .map(productMapper::toProductListItemResponse)
                .collect(Collectors.toList());

        int totalPages = productPage.getTotalPages();

        log.info("Found {} products for shop {}, total pages: {}", productList.size(), shopId, totalPages);

        return ProductListResponse.builder()
                .data(productList)
                .totalPage(totalPages)
                .build();
    }

    public UpdateProductStatusResponse updateProductStatus(Long productId, UpdateProductStatusRequest request) {
        log.info("Updating status for product ID: {}", productId);

        // Find product by ID
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (Boolean.TRUE.equals(product.getHasDeleted())) {
            throw new ResourceNotFoundException("Product not found");
        }

        // Update status
        product.setStatus(request.getStatus());

        // Save updated product
        Product updatedProduct = productRepository.save(product);
        log.info("Product status updated to {} for ID: {}", updatedProduct.getStatus(), updatedProduct.getProductId());

        return productMapper.toUpdateProductStatusResponse(updatedProduct);
    }
}
