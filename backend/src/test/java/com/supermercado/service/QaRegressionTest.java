package com.supermercado.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:qa;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class QaRegressionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int expected) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType("application/json").content(json.writeValueAsString(body));
        String result = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return result.isEmpty() ? json.nullNode() : json.readTree(result);
    }

    private String login(String name, String password) throws Exception {
        return call(post("/api/auth/login"), null, Map.of("username", name, "password", password), 200).get("accessToken").asText();
    }

    private Map<String, Object> product(String sku, double price) {
        return Map.of("name", "Producto QA", "sku", sku, "purchasePrice", 10, "salePrice", price, "stock", 10, "minStock", 2);
    }

    @Test
    void cashierCanSellAndReadHistoricalDetailButCannotModifyCatalog() throws Exception {
        String admin = login("admin", "admin123");
        String cashier = login("cajero", "cajero123");
        String sku = "QA-" + UUID.randomUUID();
        long id = call(post("/api/products"), admin, product(sku, 20), 201).get("id").asLong();
        call(get("/api/products"), cashier, null, 200);
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + cashier).contentType("application/json").content(json.writeValueAsString(product("DENIED", 20)))).andExpect(status().isForbidden());
        mvc.perform(put("/api/products/" + id).header("Authorization", "Bearer " + cashier).contentType("application/json").content(json.writeValueAsString(product(sku, 30)))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/products/" + id).header("Authorization", "Bearer " + cashier)).andExpect(status().isForbidden());
        JsonNode sale = call(post("/api/sales"), cashier, Map.of("paymentMethod", "EFECTIVO", "items", List.of(Map.of("productId", id, "quantity", 2))), 201);
        long saleId = sale.get("id").asLong();
        assertEquals(40, sale.get("subtotal").asDouble());
        assertEquals(46, sale.get("total").asDouble());
        var changed = new java.util.HashMap<>(product(sku, 30));
        changed.put("stock", 8);
        call(put("/api/products/" + id), admin, changed, 200);
        JsonNode history = call(get("/api/sales"), cashier, null, 200);
        JsonNode found = null;
        for (JsonNode entry : history) if (entry.get("id").asLong() == saleId) found = entry;
        assertNotNull(found);
        assertEquals(20, found.get("details").get(0).get("unitPrice").asDouble());
        assertEquals(46, found.get("total").asDouble());
        mvc.perform(delete("/api/products/" + id).header("Authorization", "Bearer " + admin)).andExpect(status().isConflict());
        mvc.perform(post("/api/sales/" + saleId + "/cancel").header("Authorization", "Bearer " + cashier)).andExpect(status().isForbidden());
        call(post("/api/sales/" + saleId + "/cancel"), admin, null, 200);
        JsonNode products = call(get("/api/products"), admin, null, 200);
        for (JsonNode entry : products) if (entry.get("id").asLong() == id) assertEquals(10, entry.get("stock").asInt());
    }

    @Test
    void productCrudRejectsDuplicateSkuAndPreservesAssociations() throws Exception {
        String admin = login("admin", "admin123");
        JsonNode seeded = null;
        for (JsonNode entry : call(get("/api/products"), admin, null, 200)) {
            if (!entry.get("categoryId").isNull() && !entry.get("supplierId").isNull()) { seeded = entry; break; }
        }
        assertNotNull(seeded);
        var body = new java.util.HashMap<>(product("QA-" + UUID.randomUUID(), 20));
        body.put("categoryId", seeded.get("categoryId").asLong());
        body.put("supplierId", seeded.get("supplierId").asLong());
        JsonNode created = call(post("/api/products"), admin, body, 201);
        long id = created.get("id").asLong();
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + admin).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        body.put("name", "Actualizado QA");
        JsonNode updated = call(put("/api/products/" + id), admin, body, 200);
        assertEquals("Actualizado QA", updated.get("name").asText());
        assertEquals(created.get("categoryId"), updated.get("categoryId"));
        assertEquals(created.get("supplierId"), updated.get("supplierId"));
        call(delete("/api/products/" + id), admin, null, 204);
    }

    @Test
    void customerEditingRespectsRoles() throws Exception {
        String admin = login("admin", "admin123");
        String cashier = login("cajero", "cajero123");
        var body = Map.of("name", "Cliente QA", "email", "qa@example.com");
        long id = call(post("/api/customers"), cashier, body, 201).get("id").asLong();
        mvc.perform(put("/api/customers/" + id).header("Authorization", "Bearer " + cashier).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isForbidden());
        JsonNode updated = call(put("/api/customers/" + id), admin, Map.of("name", "Cliente editado", "email", "qa@example.com"), 200);
        assertEquals("Cliente editado", updated.get("name").asText());
        call(delete("/api/customers/" + id), admin, null, 204);
    }

    @Test
    void employeeEditingPreservesOrReplacesPasswordAndEnforcesAdminRole() throws Exception {
        String admin = login("admin", "admin123");
        String username = "qa" + UUID.randomUUID().toString().substring(0, 8);
        long id = call(post("/api/employees"), admin, Map.of("username", username, "password", "original123", "fullName", "Empleado QA", "email", "qa@example.com", "roles", List.of("CAJERO")), 201).get("id").asLong();
        var body = new java.util.HashMap<String, Object>(Map.of("fullName", "Empleado editado", "email", "updated@example.com", "password", "", "roles", List.of("CAJERO")));
        JsonNode updated = call(put("/api/employees/" + id), admin, body, 200);
        assertEquals("Empleado editado", updated.get("fullName").asText());
        assertFalse(updated.has("password"));
        String employee = login(username, "original123");
        mvc.perform(put("/api/employees/" + id).header("Authorization", "Bearer " + employee).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isForbidden());
        body.put("password", "replacement123");
        body.put("roles", List.of("ALMACENISTA"));
        call(put("/api/employees/" + id), admin, body, 200);
        assertFalse(login(username, "replacement123").isBlank());
        mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("username", username, "password", "original123")))).andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
        call(delete("/api/employees/" + id), admin, null, 204);
    }

    @Test
    void receivingPurchaseAddsStockOnceAndCashierCannotReceive() throws Exception {
        String admin = login("admin", "admin123");
        String warehouse = login("almacenista", "almacenista123");
        String cashier = login("cajero", "cajero123");
        JsonNode seeded = null;
        for (JsonNode entry : call(get("/api/products"), admin, null, 200)) {
            if (!entry.get("supplierId").isNull()) { seeded = entry; break; }
        }
        assertNotNull(seeded);
        long productId = seeded.get("id").asLong();
        int stock = seeded.get("stock").asInt();
        long purchaseId = call(post("/api/purchases"), warehouse, Map.of("supplierId", seeded.get("supplierId").asLong(), "items", List.of(Map.of("productId", productId, "quantity", 3, "unitPrice", 10))), 201).get("id").asLong();
        mvc.perform(post("/api/purchases/" + purchaseId + "/receive").header("Authorization", "Bearer " + cashier)).andExpect(status().isForbidden());
        call(post("/api/purchases/" + purchaseId + "/receive"), warehouse, null, 200);
        mvc.perform(post("/api/purchases/" + purchaseId + "/receive").header("Authorization", "Bearer " + warehouse)).andExpect(status().isConflict());
        for (JsonNode entry : call(get("/api/products"), admin, null, 200)) {
            if (entry.get("id").asLong() == productId) assertEquals(stock + 3, entry.get("stock").asInt());
        }
    }
}
