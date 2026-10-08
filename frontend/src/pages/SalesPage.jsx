import { useEffect, useState } from 'react';
import api from '../services/api';
import { getRoles } from '../utils/jwt';
import NavBar from '../components/NavBar';
import ProductCatalog from '../components/ProductCatalog';
import { apiError } from '../utils/apiError';

function SalesPage() {
  const [selectedSaleId, setSelectedSaleId] = useState(null);
  const canCancel = getRoles().some(role => ['ADMIN', 'GERENTE'].includes(role));
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState([]);
  const [paymentMethod, setPaymentMethod] = useState('EFECTIVO');
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [checkingOut, setCheckingOut] = useState(false);

  const loadProducts = () => {
    setLoading(true);
    setLoadError('');
    return api.get('/products').then((res) => setProducts(res.data)).catch(() => setLoadError('No se pudieron cargar los productos')).finally(() => setLoading(false));
  };

  const loadSales = () => {
    api.get('/sales').then((res) => setSales(res.data)).catch(() => alert('No se pudieron cargar las ventas'));
  };

  useEffect(() => {
    loadProducts();
    loadSales();
  }, []);

  const addToCart = (product) => {
    if (checkingOut) return;
    setCart((prev) => {
      const existing = prev.find((item) => item.productId === product.id);
      if ((existing?.quantity || 0) >= product.stock) return prev;
      if (existing) {
        return prev.map((item) => (item.productId === product.id ? { ...item, quantity: item.quantity + 1 } : item));
      }
      return [...prev, { productId: product.id, name: product.name, price: product.salePrice, quantity: 1 }];
    });
  };

  const removeFromCart = (productId) => {
    setCart((prev) => prev.filter((item) => item.productId !== productId));
  };

  const selectedSale = sales.find(sale => sale.id === selectedSaleId);
  const money = value => Number(value ?? 0).toFixed(2);
  const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);

  const handleCheckout = async () => {
    if (checkingOut) return;
    if (cart.length === 0) {
      alert('Agrega productos antes de cobrar');
      return;
    }
    try {
      setCheckingOut(true);
      await api.post('/sales', {
        paymentMethod,
        items: cart.map((item) => ({ productId: item.productId, quantity: item.quantity }))
      });
      setCart([]);
      loadProducts();
      loadSales();
      alert('Venta registrada');
    } catch (error) {
      alert(apiError(error, 'No se pudo registrar la venta'));
    } finally { setCheckingOut(false); }
  };

  const handleCancel = async (id) => {
    try {
      await api.post(`/sales/${id}/cancel`);
      loadProducts();
      loadSales();
    } catch (error) {
      alert('No se pudo anular la venta');
    }
  };

  return (
    <div>
      <NavBar />
      <div className="page-container catalog-page">
      <div className="catalog-page-heading"><div><span className="page-eyebrow">CATÁLOGO / PUNTO DE VENTA</span><h1>Encuentra, agrega y vende</h1><p>Selecciona los productos para preparar la venta.</p></div><a className="cart-indicator" href="#sale-cart">Carrito <span>{cart.reduce((sum, item) => sum + item.quantity, 0)}</span></a></div>

      <div className="pos-layout">
        <ProductCatalog products={products} loading={loading} error={loadError} onRetry={loadProducts} renderAction={product => {
          const quantity = cart.find(item => item.productId === product.id)?.quantity || 0;
          return <button className="product-add-button" disabled={checkingOut || product.stock <= quantity} onClick={() => addToCart(product)} aria-label={`Agregar ${product.name} al carrito`}><span aria-hidden="true">+</span>{product.stock <= quantity ? product.stock <= 0 ? 'Agotado' : 'Límite de stock' : quantity ? `Agregar (${quantity})` : 'Agregar'}</button>;
        }} />

        <aside id="sale-cart" className="card sale-cart" aria-label="Carrito de venta">
          <div className="cart-heading"><h3>Tu carrito</h3><span className="badge badge-success">{cart.reduce((sum, item) => sum + item.quantity, 0)} artículos</span></div>
          {cart.length === 0 && <div className="cart-empty"><span aria-hidden="true">▧</span><strong>El carrito está vacío</strong><p>Agrega productos del catálogo para comenzar.</p></div>}
          <ul className="cart-items">
            {cart.map((item) => (
              <li key={item.productId}>
                <div><strong>{item.name}</strong><span>{item.quantity} × ${money(item.price)}</span></div><strong>${money(item.price * item.quantity)}</strong>
                <button className="product-delete-button" disabled={checkingOut} aria-label={`Quitar ${item.name} del carrito`} onClick={() => removeFromCart(item.productId)}>Quitar</button>
              </li>
            ))}
          </ul>
          <div className="cart-subtotal"><span>Subtotal</span><strong>${subtotal.toFixed(2)}</strong></div>
          <p className="cart-tax-note">El impuesto se calcula al registrar la venta.</p>

          <label>
            Método de pago:{' '}
            <select disabled={checkingOut} value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
              <option value="EFECTIVO">Efectivo</option>
              <option value="TARJETA">Tarjeta</option>
              <option value="MIXTO">Mixto</option>
            </select>
          </label>
          <div style={{ marginTop: 12 }}>
            <button className="btn-accent checkout-button" disabled={checkingOut || cart.length === 0} onClick={handleCheckout}>{checkingOut ? 'Registrando…' : 'Cobrar venta'}</button>
          </div>
        </aside>
      </div>

      <h3 style={{ marginTop: '2rem' }}>Historial de ventas</h3>
      <table className="table">
        <thead>
          <tr>
            <th>Total</th>
            <th>Pago</th>
            <th>Estado</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          {sales.map((sale) => (
            <tr key={sale.id}>
              <td>${sale.total}</td>
              <td>{sale.paymentMethod}</td>
              <td><span className={`badge ${sale.status === 'COMPLETADA' ? 'badge-success' : 'badge-danger'}`}>{sale.status}</span></td>
              <td>
                <button onClick={() => setSelectedSaleId(sale.id)}>Ver detalle</button>{' '}
                {canCancel && sale.status === 'COMPLETADA' && <button className="btn-danger" onClick={() => handleCancel(sale.id)}>Anular</button>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {selectedSale && <section className="card" aria-label="Detalle de venta">
        <h3>Detalle de venta #{selectedSale.id}</h3>
        <p>Fecha: {selectedSale.saleDate ? new Date(selectedSale.saleDate).toLocaleString('es-MX') : '—'}</p>
        <p>Cliente: {selectedSale.customerName} · Cajero: {selectedSale.cashierName || '—'}</p>
        <p>Estado: {selectedSale.status} · Pago: {selectedSale.paymentMethod}</p>
        <table className="table"><thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th></tr></thead>
          <tbody>{(selectedSale.details ?? []).map((item, index) => <tr key={index}><td>{item.productName}</td><td>{item.quantity}</td><td>${money(item.unitPrice)}</td><td>${money(item.subtotal)}</td></tr>)}</tbody>
        </table>
        <p>Subtotal: ${money(selectedSale.subtotal)} · Impuestos: ${money(selectedSale.tax)} · Descuento: ${money(selectedSale.discount)}</p>
        <p><strong>Total: ${money(selectedSale.total)}</strong></p>
        <button onClick={() => setSelectedSaleId(null)}>Cerrar detalle</button>
      </section>}
      </div>
    </div>
  );
}

export default SalesPage;
