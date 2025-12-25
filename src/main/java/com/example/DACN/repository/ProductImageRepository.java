package com.example.DACN.repository;

import com.example.DACN.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductProductIdOrderByDisplayOrderAsc(Long productId);

    void deleteByProductProductId(Long productId);
}
