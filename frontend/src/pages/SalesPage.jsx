import { useEffect, useState } from 'react';
import api from '../services/api';
import { getRoles } from '../utils/jwt';
import NavBar from '../components/NavBar';

function SalesPage() {
  const [selectedSaleId, setSelectedSaleId] = useState(null);
  const canCancel = getRoles().some(role => ['ADMIN', 'GERENTE'].includes(role));
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState([]);
  const [paymentMethod, setPaymentMethod] = useState('EFECTIVO');
  const [sales, setSales] = useState([]);

  const loadProducts = () => {
    api.get('/products').then((res) => setProducts(res.data)).catch(() => alert('No se pudieron cargar los productos'));
  };

  const loadSales = () => {
    api.get('/sales').then((res) => setSales(res.data)).catch(() => alert('No se pudieron cargar las ventas'));
  };

  useEffect(() => {
    loadProducts();
    loadSales();
  }, []);

  const addToCart = (product) => {
    setCart((prev) => {
      const existing = prev.find((item) => item.productId === product.id);
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
    if (cart.length === 0) {
      alert('Agrega productos antes de cobrar');
      return;
    }
    try {
      await api.post('/sales', {
        paymentMethod,
        items: cart.map((item) => ({ productId: item.productId, quantity: item.quantity }))
      });
      setCart([]);
      loadProducts();
      loadSales();
      alert('Venta registrada');
    } catch (error) {
      alert(error.response?.data ?? 'No se pudo registrar la venta');
    }
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
      <div className="page-container">
      <h2>Ventas (POS)</h2>

      <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap' }}>
        <div className="card" style={{ flex: 1, minWidth: 280 }}>
          <h3>Productos</h3>
          <ul>
            {products.map((product) => (
              <li key={product.id} style={{ marginBottom: 6 }}>
                {product.name} - ${product.salePrice} (stock: {product.stock})
                <button className="btn-secondary" style={{ marginLeft: 8 }} onClick={() => addToCart(product)}>Agregar</button>
              </li>
            ))}
          </ul>
        </div>

        <div className="card" style={{ flex: 1, minWidth: 280 }}>
          <h3>Carrito</h3>
          <ul>
            {cart.map((item) => (
              <li key={item.productId}>
                {item.name} x {item.quantity} = ${item.price * item.quantity}
                <button className="btn-danger" style={{ marginLeft: 8 }} onClick={() => removeFromCart(item.productId)}>Quitar</button>
              </li>
            ))}
          </ul>
          <p>Subtotal: ${subtotal.toFixed(2)}</p>

          <label>
            Método de pago:{' '}
            <select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
              <option value="EFECTIVO">Efectivo</option>
              <option value="TARJETA">Tarjeta</option>
              <option value="MIXTO">Mixto</option>
            </select>
          </label>
          <div style={{ marginTop: 12 }}>
            <button className="btn-accent" onClick={handleCheckout}>Cobrar</button>
          </div>
        </div>
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
