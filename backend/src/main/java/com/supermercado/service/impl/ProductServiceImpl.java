package com.supermercado.service.impl;

import com.supermercado.dto.CreateProductRequest;
import com.supermercado.dto.ProductResponse;
import com.supermercado.entity.Category;
import com.supermercado.entity.Product;
import com.supermercado.entity.Supplier;
import com.supermercado.repository.CategoryRepository;
import com.supermercado.repository.ProductRepository;
import com.supermercado.repository.SupplierRepository;
import com.supermercado.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        if (request.getSalePrice() < request.getPurchasePrice()) {
            throw new IllegalArgumentException("Sale price cannot be less than purchase price");
        }

        Product product = new Product();
        validateSku(request.getSku(), null);
        applyRequest(product, request);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public ProductResponse updateProduct(Long id, CreateProductRequest request) {
        if (request.getSalePrice() < request.getPurchasePrice()) {
            throw new IllegalArgumentException("Sale price cannot be less than purchase price");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        validateSku(request.getSku(), id);
        applyRequest(product, request);
        return toResponse(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found");
        }
        try {
            productRepository.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("No se puede eliminar el producto porque tiene compras, ventas o movimientos de inventario asociados");
        }
    }

    private void applyRequest(Product product, CreateProductRequest request) {
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setStock(request.getStock());
        product.setUnitOfMeasure(request.getUnitOfMeasure());
        product.setMinStock(request.getMinStock());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow();
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId()).orElseThrow();
            product.setSupplier(supplier);
        } else {
            product.setSupplier(null);
        }
    }

    private void validateSku(String sku, Long id) {
        productRepository.findBySku(sku).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe un producto con ese SKU");
            }
        });
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setSku(product.getSku());
        response.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        response.setSupplierId(product.getSupplier() != null ? product.getSupplier().getId() : null);
        response.setPurchasePrice(product.getPurchasePrice());
        response.setSalePrice(product.getSalePrice());
        response.setStock(product.getStock());
        response.setUnitOfMeasure(product.getUnitOfMeasure());
        response.setMinStock(product.getMinStock());
        response.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        response.setSupplierName(product.getSupplier() != null ? product.getSupplier().getName() : null);
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        response.setCreatedBy(product.getCreatedBy());
        response.setUpdatedBy(product.getUpdatedBy());
        return response;
    }
}
