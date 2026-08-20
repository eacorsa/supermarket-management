package com.supermercado.repository;

import com.supermercado.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);

    @Query("SELECT p FROM Product p WHERE p.minStock IS NOT NULL AND p.stock <= p.minStock")
    List<Product> findLowStockProducts();
}
