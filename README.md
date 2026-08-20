# Sistema de Gestión para Supermercado

Aplicación web para administrar la operación de un supermercado: inventario, compras, ventas, clientes, empleados, reportes y auditoría.

## Tecnologías

- **Frontend:** React 18, Vite, React Router, Axios, React Hook Form, Yup y Recharts.
- **Backend:** Java 17, Spring Boot 3, Spring Security, Spring Data JPA y JWT.
- **Base de datos:** MySQL 8.

## Funcionalidades

- Inicio de sesión con JWT y control de acceso por roles: `ADMIN`, `GERENTE`, `CAJERO` y `ALMACENISTA`.
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

## Puesta en marcha

### 1. Crear la base de datos

En MySQL, crea una base de datos llamada `supermercado`:

```sql
CREATE DATABASE supermercado;
```

El backend crea o actualiza las tablas al iniciar gracias a la configuración de JPA (`ddl-auto: update`).

### 2. Configurar y ejecutar el backend

La configuración se encuentra en `backend/src/main/resources/application.yml`. Por defecto se conecta a MySQL local con el usuario `root` y contraseña `root`.

Puedes cambiar las credenciales con variables de entorno antes de arrancar:

```powershell
$env:DB_USERNAME = "tu_usuario"
$env:DB_PASSWORD = "tu_contrasena"
$env:JWT_SECRET = "una-clave-secreta-segura-de-al-menos-32-caracteres"
```

Después, inicia la API:

```bash
cd backend
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080` y la documentación interactiva en `http://localhost:8080/swagger-ui.html`.

### 3. Ejecutar el frontend

En otra terminal:

```bash
cd frontend
npm install
npm run dev
```

Abre `http://localhost:3000` en el navegador. El frontend se comunica con la API en `http://localhost:8080/api`.

## Credenciales iniciales

Al primer inicio, la aplicación crea el siguiente usuario administrador:

| Usuario | Contraseña | Rol |
| --- | --- | --- |
| `admin` | `admin123` | `ADMIN` |

> Cambia esta contraseña y el secreto JWT antes de desplegar la aplicación en un entorno real.

## Endpoints principales

| Recurso | Ruta base |
| --- | --- |
| Autenticación | `/api/auth` |
| Productos | `/api/products` |
| Clientes | `/api/customers` |
| Empleados | `/api/employees` |
| Compras | `/api/purchases` |
| Ventas | `/api/sales` |
| Reportes | `/api/reports` |
| Auditoría | `/api/audit` |

Consulta Swagger UI para ver los métodos, parámetros y cuerpos de solicitud disponibles.

## Pruebas

Para ejecutar las pruebas del backend:

```bash
cd backend
mvn test
```

## Seguridad

No hay registro público. Los empleados y sus roles son administrados por usuarios con rol `ADMIN`. Las credenciales de la base de datos y el secreto JWT deben proporcionarse mediante variables de entorno en producción.
