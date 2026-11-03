package com.supermercado.service.impl;

import com.supermercado.dto.*;
import com.supermercado.entity.*;
import com.supermercado.repository.*;
import com.supermercado.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {
    private static final Set<String> VALID_PAYMENT_METHODS = Set.of("EFECTIVO", "TARJETA", "MIXTO");
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Value("${sales.tax-rate}")
    private double taxRate;

    @Override
    @Transactional(readOnly = true)
    public SaleResponse quoteSale(CreateSaleRequest request) {
        return toResponse(prepareSale(request));
    }

    @Override
    @Transactional
    public SaleResponse createSale(CreateSaleRequest request) {
        String fingerprint = fingerprint(request);
        if (request.getCheckoutKey() != null) {
            Optional<Sale> previous = saleRepository.findByCheckoutKey(request.getCheckoutKey());
            if (previous.isPresent()) {
                if (!fingerprint.equals(previous.get().getCheckoutFingerprint()))
                    throw new IllegalStateException("La referencia de cobro ya fue utilizada con otros datos");
                return toResponse(previous.get());
            }
        }

        Sale sale = prepareSale(request);
        if (request.getExpectedTotal() != null && money(request.getExpectedTotal()).compareTo(money(sale.getTotal())) != 0)
            throw new IllegalStateException("El precio o impuesto cambió. Revisa el nuevo total antes de cobrar");
        applyPayment(sale, request);
        sale.setCheckoutKey(request.getCheckoutKey());
        sale.setCheckoutFingerprint(request.getCheckoutKey() == null ? null : fingerprint);
        getCurrentUsername().flatMap(userRepository::findByUsername).ifPresent(sale::setCashier);

        for (SaleDetail detail : sale.getDetails()) {
            Product product = detail.getProduct();
            product.setStock(product.getStock() - detail.getQuantity());
            productRepository.save(product);
            recordMovement(product, "SALIDA", detail.getQuantity(), "Venta");
        }
        return toResponse(saleRepository.save(sale));
    }

    /** Quote and checkout use the same prices, stock checks and decimal rounding. */
    private Sale prepareSale(CreateSaleRequest request) {
        String method = request.getPaymentMethod() == null ? "" : request.getPaymentMethod().strip().toUpperCase(Locale.ROOT);
        if (!VALID_PAYMENT_METHODS.contains(method)) throw new IllegalArgumentException("Método de pago no válido");
        Map<Long, Integer> quantities = quantities(request);
        BigDecimal discount = money(request.getDiscount());
        if (discount.signum() < 0) throw new IllegalArgumentException("El descuento no puede ser negativo");
        if (!Double.isFinite(taxRate) || taxRate < 0) throw new IllegalStateException("La tasa de impuesto no es válida");

        Sale sale = new Sale();
        sale.setPaymentMethod(method);
        sale.setStatus("COMPLETADA");
        sale.setDiscount(discount.doubleValue());
        if (request.getCustomerId() != null) {
            sale.setCustomer(customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado")));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> item : quantities.entrySet()) {
            Product product = productRepository.findById(item.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + item.getKey()));
            if (!product.isActive()) throw new IllegalStateException("Producto inactivo: " + product.getName());
            if (product.getSalePrice() == null || !Double.isFinite(product.getSalePrice()) || product.getSalePrice() <= 0)
                throw new IllegalStateException("Producto sin precio de venta válido: " + product.getName());
            if (product.getStock() == null || product.getStock() < item.getValue())
                throw new IllegalStateException("Inventario insuficiente para: " + product.getName());
            BigDecimal unitPrice = money(product.getSalePrice());
            if (unitPrice.signum() <= 0) throw new IllegalStateException("Producto sin precio de venta válido: " + product.getName());
            SaleDetail detail = new SaleDetail();
            detail.setSale(sale);
            detail.setProduct(product);
            detail.setProductName(product.getName());
            detail.setProductSku(product.getSku());
            detail.setProductBarcode(product.getBarcode());
            detail.setQuantity(item.getValue());
            detail.setUnitPrice(unitPrice.doubleValue());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getValue())).setScale(2, RoundingMode.HALF_UP);
            detail.setSubtotal(lineTotal.doubleValue());
            subtotal = subtotal.add(lineTotal);
            sale.getDetails().add(detail);
        }
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(taxRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).subtract(discount);
        if (total.signum() < 0) throw new IllegalArgumentException("El descuento supera el subtotal con impuesto");
        sale.setSubtotal(subtotal.doubleValue());
        sale.setTax(tax.doubleValue());
        sale.setTotal(total.doubleValue());
        return sale;
    }

    private Map<Long, Integer> quantities(CreateSaleRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) throw new IllegalArgumentException("Agrega al menos un producto");
        Map<Long, Integer> result = new TreeMap<>();
        for (SaleItemRequest item : request.getItems()) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0)
                throw new IllegalArgumentException("Las cantidades deben ser enteros positivos");
            long quantity = (long) result.getOrDefault(item.getProductId(), 0) + item.getQuantity();
            if (quantity > Integer.MAX_VALUE) throw new IllegalArgumentException("Cantidad fuera de rango");
            result.put(item.getProductId(), (int) quantity);
        }
        return result;
    }

    private void applyPayment(Sale sale, CreateSaleRequest request) {
        BigDecimal total = money(sale.getTotal());
        BigDecimal card = "TARJETA".equals(sale.getPaymentMethod()) ? total : "MIXTO".equals(sale.getPaymentMethod()) ? money(request.getCardAmount()) : BigDecimal.ZERO;
        if (card.signum() < 0 || card.compareTo(total) > 0) throw new IllegalArgumentException("El importe de tarjeta no es válido");
        if ("MIXTO".equals(sale.getPaymentMethod()) && (card.signum() <= 0 || card.compareTo(total) >= 0))
            throw new IllegalArgumentException("El pago mixto requiere una parte en efectivo y otra en tarjeta");
        BigDecimal cashDue = total.subtract(card);
        BigDecimal cash = "TARJETA".equals(sale.getPaymentMethod()) ? BigDecimal.ZERO : request.getCashReceived() == null ? cashDue : money(request.getCashReceived());
        if (cash.signum() < 0 || cash.compareTo(cashDue) < 0) throw new IllegalArgumentException("El efectivo recibido es insuficiente");
        sale.setCashReceived(cash.doubleValue());
        sale.setCardAmount(card.doubleValue());
        sale.setChangeAmount(cash.subtract(cashDue).doubleValue());
    }

    private BigDecimal money(Double value) {
        if (value == null) return BigDecimal.ZERO.setScale(2);
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Importe monetario no válido");
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /** A retry must describe exactly the same checkout, including its authenticated cashier. */
    private String fingerprint(CreateSaleRequest request) {
        String data = getCurrentUsername().orElse("") + "|" + request.getCustomerId() + "|" + request.getPaymentMethod()
                + "|" + request.getDiscount() + "|" + request.getExpectedTotal() + "|" + request.getCashReceived()
                + "|" + request.getCardAmount() + "|" + quantities(request);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No fue posible identificar el cobro", ex);
        }
    }

    @Override
    @Transactional
    public SaleResponse cancelSale(Long saleId) {
        Sale sale = findSale(saleId);
        if ("ANULADA".equals(sale.getStatus())) throw new IllegalStateException("La venta ya está anulada");
        for (SaleDetail detail : sale.getDetails()) {
            Product product = detail.getProduct();
            product.setStock(product.getStock() + detail.getQuantity());
            productRepository.save(product);
            recordMovement(product, "ENTRADA", detail.getQuantity(), "Anulación de venta #" + sale.getId());
        }
        sale.setStatus("ANULADA");
        return toResponse(saleRepository.save(sale));
    }

    private void recordMovement(Product product, String type, int quantity, String note) {
        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setNote(note);
        inventoryMovementRepository.save(movement);
    }

    private Sale findSale(Long id) {
        return saleRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponse getSale(Long id) { return toResponse(findSale(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponse> getAllSales() {
        return saleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated() ? Optional.empty() : Optional.of(authentication.getName());
    }

    private SaleResponse toResponse(Sale sale) {
        SaleResponse response = new SaleResponse();
        response.setId(sale.getId());
        response.setCheckoutKey(sale.getCheckoutKey());
        response.setCashReceived(sale.getCashReceived());
        response.setCardAmount(sale.getCardAmount());
        response.setChangeAmount(sale.getChangeAmount());
        response.setCustomerName(sale.getCustomer() != null ? sale.getCustomer().getName() : "Consumidor final");
        response.setCashierName(sale.getCashier() != null ? sale.getCashier().getFullName() : null);
        response.setSaleDate(sale.getSaleDate());
        response.setSubtotal(sale.getSubtotal());
        response.setTax(sale.getTax());
        response.setDiscount(sale.getDiscount());
        response.setTotal(sale.getTotal());
        response.setPaymentMethod(sale.getPaymentMethod());
        response.setStatus(sale.getStatus());
        response.setDetails(sale.getDetails().stream().map(detail -> {
            SaleDetailResponse line = new SaleDetailResponse();
            line.setProductId(detail.getProduct().getId());
            line.setProductName(detail.getProductName() != null ? detail.getProductName() : detail.getProduct().getName());
            line.setSku(detail.getProductSku() != null ? detail.getProductSku() : detail.getProduct().getSku());
            line.setBarcode(detail.getProductBarcode());
            line.setQuantity(detail.getQuantity());
            line.setUnitPrice(detail.getUnitPrice());
            line.setSubtotal(detail.getSubtotal());
            return line;
        }).collect(Collectors.toList()));
        return response;
    }
}
