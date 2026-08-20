package com.supermercado.repository;

import com.supermercado.entity.SaleDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleDetailRepository extends JpaRepository<SaleDetail, Long> {

    @Query("SELECT sd.product.name AS productName, SUM(sd.quantity) AS totalQuantity, SUM(sd.subtotal) AS totalRevenue " +
            "FROM SaleDetail sd " +
            "WHERE sd.sale.status = 'COMPLETADA' " +
            "GROUP BY sd.product.name " +
            "ORDER BY SUM(sd.quantity) DESC")
    List<TopProductProjection> findTopSellingProducts(Pageable pageable);

    interface TopProductProjection {
        String getProductName();
        Long getTotalQuantity();
        Double getTotalRevenue();
    }
}
