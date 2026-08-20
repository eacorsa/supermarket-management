package com.supermercado.repository;

import com.supermercado.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("SELECT s FROM Sale s WHERE s.status = 'COMPLETADA' AND s.saleDate BETWEEN :start AND :end")
    List<Sale> findCompletedSalesBetween(LocalDateTime start, LocalDateTime end);
}
