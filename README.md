# Sistema de Gestión para Supermercado

Aplicación web para administrar la operación de un supermercado: inventario, compras, ventas, clientes, empleados, reportes y auditoría.

## Tecnologías

- **Frontend:** React 18, Vite, React Router, Axios, React Hook Form, Yup y Recharts.
- **Backend:** Java 17, Spring Boot 3, Spring Security, Spring Data JPA y JWT.
- **Base de datos:** MySQL 8.

## Funcionalidades

- Inicio de sesión con JWT y control de acceso por roles:
- Gestión de productos, categorías, proveedores y stock mínimo.
- Registro y recepción de compras; la recepción actualiza el inventario.
- Punto de venta, cálculo de impuestos y cancelación de ventas con restauración de stock.
- Administración de clientes y empleados.
- Panel con indicadores, reportes de ventas, productos más vendidos y alertas de stock bajo.
- Auditoría de los cambios en las entidades principales.

## Estructura

```text
supermarket/
├── backend/       # API REST con Spring Boot
├── frontend/      # Aplicación React con Vite
├── docs/          # Documentación del proyecto
└── .gitignore
```

## Requisitos

- Java 17 o superior
- Maven 3.9 o superior
- Node.js 20 o superior
- MySQL 8 o superior
