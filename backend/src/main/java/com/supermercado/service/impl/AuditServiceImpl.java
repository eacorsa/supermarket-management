package com.supermercado.service.impl;

import com.supermercado.dto.AuditRecordResponse;
import com.supermercado.entity.Auditable;
import com.supermercado.entity.Category;
import com.supermercado.entity.Customer;
import com.supermercado.entity.Product;
import com.supermercado.entity.Purchase;
import com.supermercado.entity.Sale;
import com.supermercado.entity.Supplier;
import com.supermercado.repository.CategoryRepository;
import com.supermercado.repository.CustomerRepository;
import com.supermercado.repository.ProductRepository;
import com.supermercado.repository.PurchaseRepository;
import com.supermercado.repository.SaleRepository;
import com.supermercado.repository.SupplierRepository;
import com.supermercado.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleRepository saleRepository;

    @Override
    public List<AuditRecordResponse> getAuditLog(String entityType) {
        Stream<AuditRecordResponse> records = Stream.of(
                productRepository.findAll().stream()
                        .map(p -> toRecord("PRODUCTO", p.getId(), p.getName() + " (" + p.getSku() + ")", p)),
                categoryRepository.findAll().stream()
                        .map(c -> toRecord("CATEGORIA", c.getId(), c.getName(), c)),
                supplierRepository.findAll().stream()
                        .map(s -> toRecord("PROVEEDOR", s.getId(), s.getName(), s)),
                customerRepository.findAll().stream()
                        .map(c -> toRecord("CLIENTE", c.getId(), c.getName(), c)),
                purchaseRepository.findAll().stream()
                        .map(p -> toRecord("COMPRA", p.getId(), "Compra #" + p.getId() + " - " + p.getStatus(), p)),
                saleRepository.findAll().stream()
                        .map(s -> toRecord("VENTA", s.getId(), "Venta #" + s.getId() + " - " + s.getStatus(), s))
        ).flatMap(s -> s);

        return records
                .filter(r -> entityType == null || entityType.isBlank() || r.getEntityType().equalsIgnoreCase(entityType))
                .sorted(Comparator.comparing(
                        (AuditRecordResponse r) -> r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private AuditRecordResponse toRecord(String entityType, Long id, String description, Auditable auditable) {
        return new AuditRecordResponse(entityType, id, description,
                auditable.getCreatedAt(), auditable.getCreatedBy(),
                auditable.getUpdatedAt(), auditable.getUpdatedBy());
    }
}
