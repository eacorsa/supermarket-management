package com.supermercado.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermercado.dto.CreateSaleRequest;
import com.supermercado.entity.Product;
import com.supermercado.repository.ProductRepository;
import com.supermercado.repository.SaleRepository;
import com.supermercado.repository.InventoryMovementRepository;
import com.supermercado.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:barcodepos;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "sales.tax-rate=0.15"
})
@AutoConfigureMockMvc
class BarcodePosTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @Autowired ProductRepository products;
    @Autowired SaleRepository sales;
    @Autowired InventoryMovementRepository movements;
    @Autowired SaleService saleService;
    @Autowired PlatformTransactionManager transactions;

    private String token(String user) { return jwt.generateAccessToken(user, List.of()); }

    private JsonNode call(MockHttpServletRequestBuilder request, String user, Object body, int expected) throws Exception {
        if (user != null) request.header("Authorization", "Bearer " + token(user));
        if (body != null) request.contentType("application/json").content(json.writeValueAsString(body));
        String text = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return text.isEmpty() ? json.nullNode() : json.readTree(text);
    }

    private Map<String, Object> catalog(String barcode, int stock, double price) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Producto POS"); body.put("sku", "POS-" + UUID.randomUUID());
        body.put("barcode", barcode); body.put("purchasePrice", 0); body.put("salePrice", price); body.put("stock", stock);
        return body;
    }

    private long create(String barcode, int stock, double price) throws Exception {
        return call(post("/api/products"), "admin", catalog(barcode, stock, price), 201).get("id").asLong();
    }

    private Map<String, Object> checkout(long id, int quantity, String method) {
        return new HashMap<>(Map.of("paymentMethod", method, "items", List.of(Map.of("productId", id, "quantity", quantity))));
    }

    @Test
    void barcodesRemainTextAndSupportHidFormatsAndCatalogEditing() throws Exception {
        for (String code : List.of("0123456789012", "001234567890", "Code128/Abc-42+%")) {
            Map<String, Object> body = catalog(code, 5, 12.5);
            JsonNode created = call(post("/api/products"), "admin", body, 201);
            long id = created.get("id").asLong();
            assertTrue(created.get("active").asBoolean());
            JsonNode scanned = call(get("/api/products/barcode").param("code", code), "cajero", null, 200);
            assertEquals(id, scanned.get("id").asLong());
            assertEquals(code, scanned.get("barcode").asText());
            body.put("barcode", code + "-NEW");
            call(put("/api/products/" + id), "admin", body, 200);
            mvc.perform(get("/api/products/barcode").param("code", code).header("Authorization", "Bearer " + token("cajero"))).andExpect(status().isNotFound());
            body.put("barcode", "   ");
            assertTrue(call(put("/api/products/" + id), "admin", body, 200).get("barcode").isNull());
        }
        long first = create(null, 1, 1);
        long second = create(null, 1, 1);
        assertNotEquals(first, second);
    }

    @Test
    void duplicateBarcodesAreRejectedByCatalogAndDatabase() throws Exception {
        String code = "DUP-" + UUID.randomUUID();
        long id = create(code, 5, 10);
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + token("admin")).contentType("application/json")
                .content(json.writeValueAsString(catalog(code, 5, 10)))).andExpect(status().isConflict());
        long other = create("OTHER-" + UUID.randomUUID(), 5, 10);
        mvc.perform(put("/api/products/" + other).header("Authorization", "Bearer " + token("admin")).contentType("application/json")
                .content(json.writeValueAsString(catalog(code, 5, 10)))).andExpect(status().isConflict());
        Product direct = new Product(); direct.setName("Duplicado"); direct.setSku(UUID.randomUUID().toString());
        direct.setBarcode(code); direct.setPurchasePrice(0.0); direct.setSalePrice(10.0); direct.setStock(5);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> products.saveAndFlush(direct));
        assertEquals(id, products.findByBarcode(code).orElseThrow().getId());
    }

    @Test
    void scanValidatesUnknownInactiveMissingPriceAndExhaustedInventory() throws Exception {
        mvc.perform(get("/api/products/barcode").param("code", "UNKNOWN").header("Authorization", "Bearer " + token("cajero"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/barcode").param("code", " ").header("Authorization", "Bearer " + token("cajero"))).andExpect(status().isBadRequest());
        for (String invalid : List.of("x".repeat(256), "ABC\nDEF")) {
            mvc.perform(get("/api/products/barcode").param("code", invalid).header("Authorization", "Bearer " + token("cajero"))).andExpect(status().isBadRequest());
        }
        for (String reason : List.of("inactive", "no-price", "no-stock")) {
            String code = reason + UUID.randomUUID();
            Map<String, Object> body = catalog(code, reason.equals("no-stock") ? 0 : 5, reason.equals("no-price") ? 0 : 10);
            if (reason.equals("inactive")) body.put("active", false);
            long id = call(post("/api/products"), "admin", body, 201).get("id").asLong();
            mvc.perform(get("/api/products/barcode").param("code", code).header("Authorization", "Bearer " + token("cajero"))).andExpect(status().isConflict());
            mvc.perform(post("/api/sales").header("Authorization", "Bearer " + token("cajero")).contentType("application/json")
                    .content(json.writeValueAsString(checkout(id, 1, "EFECTIVO")))).andExpect(status().isConflict());
        }
        mvc.perform(get("/api/products/barcode").param("code", "ANY")).andExpect(status().isForbidden());
        long id = create("ROLE-" + UUID.randomUUID(), 5, 10);
        mvc.perform(put("/api/products/" + id).header("Authorization", "Bearer " + token("cajero")).contentType("application/json")
                .content(json.writeValueAsString(catalog("NEW", 5, 10)))).andExpect(status().isForbidden());
    }

    @Test
    void warehouseCanLookupButCannotQuoteChargeOrReadSaleDetails() throws Exception {
        String user = "warehouse-" + UUID.randomUUID();
        call(post("/api/employees"), "admin", Map.of("username", user, "password", "Temporary-test-only-42!", "fullName", "Almacén POS", "email", "warehouse@example.test", "roles", List.of("ALMACENISTA")), 201);
        String code = "WAREHOUSE-" + UUID.randomUUID();
        long id = create(code, 5, 10);
        call(get("/api/products/barcode").param("code", code), user, null, 200);
        for (String endpoint : List.of("/api/sales", "/api/sales/quote")) {
            mvc.perform(post(endpoint).header("Authorization", "Bearer " + token(user)).contentType("application/json")
                    .content(json.writeValueAsString(checkout(id, 1, "EFECTIVO")))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/sales/1").header("Authorization", "Bearer " + token(user))).andExpect(status().isForbidden());
        mvc.perform(post("/api/sales/quote").contentType("application/json").content(json.writeValueAsString(checkout(id, 1, "EFECTIVO")))).andExpect(status().isForbidden());
    }

    @Test
    void quoteHasAuthoritativeRoundedAmountsAndNeverWritesInventoryOrSales() throws Exception {
        long id = create("QUOTE-" + UUID.randomUUID(), 5, 0.10);
        long beforeSales = sales.count(), beforeMovements = movements.count();
        JsonNode quote = call(post("/api/sales/quote"), "cajero", checkout(id, 3, "EFECTIVO"), 200);
        assertEquals(0.30, quote.get("subtotal").asDouble());
        assertEquals(0.05, quote.get("tax").asDouble());
        assertEquals(0.35, quote.get("total").asDouble());
        assertEquals(5, products.findById(id).orElseThrow().getStock());
        assertEquals(beforeSales, sales.count()); assertEquals(beforeMovements, movements.count());
        var body = checkout(id, 3, "EFECTIVO"); body.put("expectedTotal", 0.30);
        mvc.perform(post("/api/sales").header("Authorization", "Bearer " + token("cajero")).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isConflict());
        assertEquals(5, products.findById(id).orElseThrow().getStock());
    }

    @Test
    void cashCheckoutIsIdempotentAndTicketsKeepHistoricalNamesCodesAndPrices() throws Exception {
        String barcode = "TICKET-" + UUID.randomUUID();
        long id = create(barcode, 5, 10);
        var body = checkout(id, 2, "EFECTIVO");
        body.put("checkoutKey", UUID.randomUUID().toString()); body.put("expectedTotal", 23); body.put("cashReceived", 30);
        JsonNode sale = call(post("/api/sales"), "cajero", body, 201);
        long saleId = sale.get("id").asLong();
        assertEquals(7, sale.get("changeAmount").asDouble());
        JsonNode repeated = call(post("/api/sales"), "cajero", body, 201);
        assertEquals(saleId, repeated.get("id").asLong());
        assertEquals(3, products.findById(id).orElseThrow().getStock());
        var changed = catalog("RENAMED-" + UUID.randomUUID(), 3, 20);
        changed.put("name", "Nombre nuevo");
        call(put("/api/products/" + id), "admin", changed, 200);
        JsonNode ticket = call(get("/api/sales/" + saleId), "cajero", null, 200);
        assertEquals("Producto POS", ticket.get("details").get(0).get("productName").asText());
        assertEquals(barcode, ticket.get("details").get(0).get("barcode").asText());
        assertEquals(10, ticket.get("details").get(0).get("unitPrice").asDouble());
        body.put("cashReceived", 40);
        mvc.perform(post("/api/sales").header("Authorization", "Bearer " + token("cajero")).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isConflict());
        call(post("/api/sales/" + saleId + "/cancel"), "admin", null, 200);
        assertEquals(5, products.findById(id).orElseThrow().getStock());
        mvc.perform(post("/api/sales/" + saleId + "/cancel").header("Authorization", "Bearer " + token("admin"))).andExpect(status().isConflict());
    }

    @Test
    void cardAndMixedPaymentsValidateAmountsAndCalculateChange() throws Exception {
        long id = create("PAY-" + UUID.randomUUID(), 10, 10);
        JsonNode card = call(post("/api/sales"), "cajero", checkout(id, 1, "TARJETA"), 201);
        assertEquals(11.5, card.get("cardAmount").asDouble());
        assertEquals(0, card.get("cashReceived").asDouble());
        var mixed = checkout(id, 1, "MIXTO"); mixed.put("cardAmount", 5); mixed.put("cashReceived", 10);
        JsonNode sale = call(post("/api/sales"), "cajero", mixed, 201);
        assertEquals(3.5, sale.get("changeAmount").asDouble());
        for (Map<String, Object> invalid : List.of(new HashMap<>(Map.<String, Object>of("cardAmount", 0, "cashReceived", 20)),
                new HashMap<>(Map.<String, Object>of("cardAmount", 20, "cashReceived", 20)), new HashMap<>(Map.<String, Object>of("cardAmount", 5, "cashReceived", 1)))) {
            var request = checkout(id, 1, "MIXTO"); request.putAll(invalid);
            mvc.perform(post("/api/sales").header("Authorization", "Bearer " + token("cajero")).contentType("application/json")
                    .content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
        }
        assertEquals(8, products.findById(id).orElseThrow().getStock());
    }

    @Test
    void insufficientStockAndDuplicatedLinesNeverProducePartialSales() throws Exception {
        long first = create("FIRST-" + UUID.randomUUID(), 5, 10), second = create("SECOND-" + UUID.randomUUID(), 1, 20);
        long count = sales.count(), movementCount = movements.count();
        var body = checkout(first, 1, "EFECTIVO");
        body.put("items", List.of(Map.of("productId", first, "quantity", 1), Map.of("productId", second, "quantity", 2)));
        mvc.perform(post("/api/sales").header("Authorization", "Bearer " + token("cajero")).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isConflict());
        assertEquals(5, products.findById(first).orElseThrow().getStock());
        body.put("items", List.of(Map.of("productId", second, "quantity", 1), Map.of("productId", second, "quantity", 1)));
        mvc.perform(post("/api/sales").header("Authorization", "Bearer " + token("cajero")).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().isConflict());
        assertEquals(1, products.findById(second).orElseThrow().getStock());
        assertEquals(count, sales.count()); assertEquals(movementCount, movements.count());
        for (int quantity : List.of(0, -1)) {
            mvc.perform(post("/api/sales").header("Authorization", "Bearer " + token("cajero")).contentType("application/json")
                    .content(json.writeValueAsString(checkout(first, quantity, "EFECTIVO")))).andExpect(status().isBadRequest());
        }
    }

    @Test
    void simultaneousCheckoutsCannotSellTheLastUnitTwice() throws Exception {
        long id = create("CONCURRENT-" + UUID.randomUUID(), 1, 10);
        CreateSaleRequest request = json.convertValue(checkout(id, 1, "EFECTIVO"), CreateSaleRequest.class);
        CyclicBarrier ready = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> checkout = () -> {
            try {
                new TransactionTemplate(transactions).execute(status -> {
                    products.findById(id).orElseThrow(); // Both transactions retain the same version before selling.
                    try { ready.await(10, TimeUnit.SECONDS); } catch (Exception ex) { throw new IllegalStateException(ex); }
                    return saleService.createSale(request);
                });
                return true;
            } catch (org.springframework.dao.OptimisticLockingFailureException ex) { return false; }
        };
        long before = sales.count();
        try {
            Future<Boolean> a = executor.submit(checkout), b = executor.submit(checkout);
            assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
        assertEquals(0, products.findById(id).orElseThrow().getStock());
        assertEquals(before + 1, sales.count());
    }
}
