package com.supermercado.config;

import com.supermercado.entity.*;
import com.supermercado.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Override
    public void run(String... args) {
        createRoleIfAbsent("ADMIN");
        createRoleIfAbsent("GERENTE");
        createRoleIfAbsent("CAJERO");
        createRoleIfAbsent("ALMACENISTA");

        createUserIfAbsent("admin", "admin123", "Administrador", "admin@supermercado.com", "ADMIN");
        createUserIfAbsent("gerente", "gerente123", "Gerente General", "gerente@supermercado.com", "GERENTE");
        createUserIfAbsent("cajero", "cajero123", "Cajero Principal", "cajero@supermercado.com", "CAJERO");
        createUserIfAbsent("almacenista", "almacenista123", "Encargado de Almacén", "almacenista@supermercado.com", "ALMACENISTA");

        seedCatalogAndPurchases();
    }

    private void createRoleIfAbsent(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
    }

    private void createUserIfAbsent(String username, String rawPassword, String fullName, String email, String roleName) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRoles(Set.of(role));
        userRepository.save(user);
    }

    private void seedCatalogAndPurchases() {
        Category abarrotes = getOrCreateCategory("Abarrotes", "Productos básicos de despensa");
        Category lacteos = getOrCreateCategory("Lácteos", "Leche, quesos y derivados");
        Category bebidas = getOrCreateCategory("Bebidas", "Bebidas embotelladas y enlatadas");

        Supplier distCentral = getOrCreateSupplier("Distribuidora Central", "Juan Pérez", "555-1000", "ventas@distcentral.com");
        Supplier lacteosDelValle = getOrCreateSupplier("Lácteos del Valle", "Ana Gómez", "555-2000", "contacto@lacteosdelvalle.com");

        // Stock inicial en 0 para arroz, azúcar y leche: se completa al recibir sus compras.
        Product arroz = getOrCreateProduct("Arroz 1kg", "ARZ-001", abarrotes, distCentral, 1000.0, 1300.0, 0, "unidad", 10);
        Product azucar = getOrCreateProduct("Azúcar 1kg", "AZU-001", abarrotes, distCentral, 900.0, 1200.0, 0, "unidad", 10);
        Product leche = getOrCreateProduct("Leche entera 1L", "LEC-001", lacteos, lacteosDelValle, 800.0, 1050.0, 0, "unidad", 8);
        Product queso = getOrCreateProduct("Queso fresco 500g", "QUE-001", lacteos, lacteosDelValle, 1500.0, 2000.0, 20, "unidad", 5);
        Product gaseosa = getOrCreateProduct("Gaseosa 2L", "BEB-001", bebidas, distCentral, 1200.0, 1600.0, 5, "unidad", 5);

        if (purchaseRepository.count() > 0) {
            return;
        }

        // Compra recibida: aplica stock + movimiento de inventario, igual que PurchaseServiceImpl.receivePurchase.
        createReceivedPurchase(distCentral, List.of(
                new PurchaseSeedItem(arroz, 50, 1000.0),
                new PurchaseSeedItem(azucar, 30, 900.0)
        ));
        createReceivedPurchase(lacteosDelValle, List.of(
                new PurchaseSeedItem(leche, 40, 800.0)
        ));

        // Compra pendiente: queda por recibir, no afecta stock (útil para probar "Recibir mercadería").
        createPendingPurchase(distCentral, List.of(
                new PurchaseSeedItem(gaseosa, 20, 1200.0)
        ));
    }

    private Category getOrCreateCategory(String name, String description) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            return categoryRepository.save(category);
        });
    }

    private Supplier getOrCreateSupplier(String name, String contactName, String phone, String email) {
        return supplierRepository.findFirstByName(name).orElseGet(() -> {
            Supplier supplier = new Supplier();
            supplier.setName(name);
            supplier.setContactName(contactName);
            supplier.setPhone(phone);
            supplier.setEmail(email);
            return supplierRepository.save(supplier);
        });
    }

    private Product getOrCreateProduct(String name, String sku, Category category, Supplier supplier,
                                        double purchasePrice, double salePrice, int stock, String unit, int minStock) {
        return productRepository.findBySku(sku).orElseGet(() -> {
            Product product = new Product();
            product.setName(name);
            product.setSku(sku);
            product.setCategory(category);
            product.setSupplier(supplier);
            product.setPurchasePrice(purchasePrice);
            product.setSalePrice(salePrice);
            product.setStock(stock);
            product.setUnitOfMeasure(unit);
            product.setMinStock(minStock);
            return productRepository.save(product);
        });
    }

    private void createPendingPurchase(Supplier supplier, List<PurchaseSeedItem> items) {
        Purchase purchase = buildPurchase(supplier, items);
        purchase.setStatus("PENDING");
        purchaseRepository.save(purchase);
    }

    private void createReceivedPurchase(Supplier supplier, List<PurchaseSeedItem> items) {
        Purchase purchase = buildPurchase(supplier, items);
        purchase.setStatus("RECEIVED");
        Purchase saved = purchaseRepository.save(purchase);

        for (PurchaseDetail detail : saved.getDetails()) {
            Product product = detail.getProduct();
            product.setStock(product.getStock() + detail.getQuantity());
            productRepository.save(product);

            InventoryMovement movement = new InventoryMovement();
            movement.setProduct(product);
            movement.setType("ENTRADA");
            movement.setQuantity(detail.getQuantity());
            movement.setNote("Recepción de compra #" + saved.getId());
            inventoryMovementRepository.save(movement);
        }
    }

    private Purchase buildPurchase(Supplier supplier, List<PurchaseSeedItem> items) {
        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setPurchaseDate(LocalDateTime.now());

        double total = 0.0;
        for (PurchaseSeedItem item : items) {
            PurchaseDetail detail = new PurchaseDetail();
            detail.setPurchase(purchase);
            detail.setProduct(item.product());
            detail.setQuantity(item.quantity());
            detail.setUnitPrice(item.unitPrice());
            detail.setSubtotal(item.quantity() * item.unitPrice());
            purchase.getDetails().add(detail);
            total += detail.getSubtotal();
        }
        purchase.setTotal(total);
        return purchase;
    }

    private record PurchaseSeedItem(Product product, int quantity, double unitPrice) {
    }
}
