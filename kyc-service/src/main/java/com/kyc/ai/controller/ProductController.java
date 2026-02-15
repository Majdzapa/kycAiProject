package com.kyc.ai.controller;

import com.kyc.ai.entity.Product;
import com.kyc.ai.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing banking products and their risk levels")
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    @Operation(summary = "List all products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product")
    public Product createProduct(@RequestBody Product product) {
        if (productRepository.existsByName(product.getName())) {
            throw new IllegalArgumentException("Product with this name already exists");
        }
        // Set default risk score if not provided based on level
        if (product.getRiskScore() == null) {
            switch (product.getBaseRiskLevel()) {
                case LOW -> product.setRiskScore(10);
                case MEDIUM -> product.setRiskScore(40);
                case HIGH -> product.setRiskScore(70);
                case CRITICAL -> product.setRiskScore(90);
            }
        }
        return productRepository.save(product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
