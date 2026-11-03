import { useEffect, useRef, useState } from 'react';
import api from '../services/api';
import { decodeJwtPayload, getRoles } from '../utils/jwt';
import { apiError } from '../utils/apiError';
import { addCartProduct, setCartQuantity, money } from '../utils/pos';
import NavBar from '../components/NavBar';
import ProductCatalog from '../components/ProductCatalog';
import BarcodeScanner from '../components/BarcodeScanner';
import SaleTicket from '../components/SaleTicket';

function readPending(key) {
  try {
    const value = JSON.parse(sessionStorage.getItem(key));
    return value?.body?.checkoutKey && Array.isArray(value.cart) ? { ...value, uncertain: true } : null;
  } catch { return null; }
}

function SalesPage() {
  const owner = decodeJwtPayload(localStorage.getItem('accessToken') || '')?.sub || 'anonymous';
  const storageKey = `pos.pendingCheckout.${owner}`;
  const [initial] = useState(() => readPending(storageKey));
  const [pendingCheckout, setPendingCheckout] = useState(initial);
  const [cart, setCart] = useState(initial?.cart || []);
  const cartRef = useRef(cart);
  const busyRef = useRef(false);
  const scansRef = useRef(0);
  const pendingRef = useRef(initial);
  const [message, setMessage] = useState(initial ? { type: 'error', text: 'Hay un cobro por confirmar. Reintenta el mismo cobro para recuperar su resultado sin duplicarlo.' } : null);
  const [pendingScans, setPendingScans] = useState(0);
  const [paymentMethod, setPaymentMethod] = useState(initial?.body.paymentMethod || 'EFECTIVO');
  const [cashReceived, setCashReceived] = useState(initial?.body.cashReceived ?? '');
  const [cardAmount, setCardAmount] = useState(initial?.body.cardAmount ?? '');
  const [products, setProducts] = useState([]);
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [historyError, setHistoryError] = useState('');
  const [checkingOut, setCheckingOut] = useState(false);
  const [quoteState, setQuoteState] = useState(null);
  const [quoteError, setQuoteError] = useState('');
  const [quoting, setQuoting] = useState(false);
  const [quoteRefresh, setQuoteRefresh] = useState(0);
  const [selectedSale, setSelectedSale] = useState(null);
  const [receipt, setReceipt] = useState(null);
  const canCancel = getRoles().some(role => ['ADMIN', 'GERENTE'].includes(role));
  const locked = checkingOut || !!pendingCheckout;
  const items = cart.map(item => ({ productId: item.productId, quantity: item.quantity }));
  const quoteKey = JSON.stringify({ items, paymentMethod });
  const quote = pendingCheckout?.quote || (quoteState?.key === quoteKey ? quoteState.sale : null);
  const quoteMoney = field => quote ? `$${money(quote[field])}` : cart.length ? '—' : '$0.00';

  const replaceCart = next => {
    cartRef.current = next;
    setCart(next);
    setQuoteState(null);
  };

  const loadProducts = () => {
    setLoading(true);
    setLoadError('');
    return api.get('/products', { timeout: 15000 }).then(res => {
      setProducts(res.data);
      if (!pendingRef.current && cartRef.current.length) {
        replaceCart(cartRef.current.map(item => {
          const current = res.data.find(product => product.id === item.productId);
          return current ? { ...item, stock: current.stock, price: current.salePrice } : item;
        }));
        setQuoteRefresh(value => value + 1);
      }
    }).catch(() => setLoadError('No se pudieron cargar los productos')).finally(() => setLoading(false));
  };

  const loadSales = () => {
    setHistoryError('');
    return api.get('/sales', { timeout: 15000 }).then(res => setSales([...res.data].sort((a, b) => b.id - a.id)))
      .catch(() => setHistoryError('No se pudo cargar el historial. Puedes volver a intentarlo.'));
  };

  useEffect(() => { loadProducts(); loadSales(); }, []);

  useEffect(() => {
    if (!cart.length || pendingCheckout) { setQuoting(false); setQuoteError(''); return; }
    const controller = new AbortController();
    let cancelled = false;
    setQuoting(true);
    setQuoteError('');
    const timer = setTimeout(() => {
      api.post('/sales/quote', { paymentMethod, items }, { signal: controller.signal, timeout: 15000 })
        .then(res => { if (!cancelled) setQuoteState({ key: quoteKey, sale: res.data }); })
        .catch(error => { if (!cancelled) { setQuoteState(null); setQuoteError(apiError(error, 'No fue posible calcular el total. Revisa tu conexión.')); } })
        .finally(() => { if (!cancelled) setQuoting(false); });
    }, 150);
    return () => { cancelled = true; clearTimeout(timer); controller.abort(); };
  }, [quoteKey, pendingCheckout, quoteRefresh]);

  const addToCart = product => {
    if (busyRef.current || pendingRef.current) throw new Error('Termina de confirmar el cobro antes de agregar productos');
    replaceCart(addCartProduct(cartRef.current, product));
    setMessage({ type: 'success', text: `${product.name} agregado al carrito` });
  };
  const handleCatalogAdd = product => {
    try { addToCart(product); } catch (error) { setMessage({ type: 'error', text: error.message }); }
  };
  const changeQuantity = (id, quantity) => {
    if (locked) return;
    try { replaceCart(setCartQuantity(cartRef.current, id, quantity)); }
    catch (error) { setMessage({ type: 'error', text: error.message }); }
  };
  const removeFromCart = id => { if (!locked) replaceCart(cartRef.current.filter(item => item.productId !== id)); };

  const cardPaid = paymentMethod === 'TARJETA' ? quote?.total || 0 : paymentMethod === 'MIXTO' ? Number(cardAmount) : 0;
  const cashDue = Math.round(((quote?.total || 0) - cardPaid) * 100) / 100;
  const cashPaid = paymentMethod === 'TARJETA' ? 0 : Number(cashReceived);
  const paymentValid = !!quote && (paymentMethod !== 'MIXTO' || cardAmount !== '' && cardPaid > 0 && cardPaid < quote.total)
    && (paymentMethod === 'TARJETA' || cashReceived !== '' && Number.isFinite(cashPaid) && Math.round(cashPaid * 100) >= Math.round(cashDue * 100));

  const handleCheckout = async () => {
    if (busyRef.current || scansRef.current > 0) return;
    if (!pendingRef.current && (!cart.length || !quote || quoting || !paymentValid)) return;
    let attempt = pendingRef.current;
    if (!attempt) {
      attempt = {
        body: { paymentMethod, items, expectedTotal: quote.total, cashReceived: paymentMethod === 'TARJETA' ? 0 : cashPaid, cardAmount: cardPaid, checkoutKey: crypto.randomUUID() },
        cart: cartRef.current, quote, uncertain: false
      };
    }
    // Save the exact request before sending. A timeout must not become a second sale on retry/reload.
    try { sessionStorage.setItem(storageKey, JSON.stringify(attempt)); }
    catch { setMessage({ type: 'error', text: 'No se pudo guardar la referencia del cobro en esta pestaña. Habilita el almacenamiento de sesión antes de cobrar.' }); return; }
    pendingRef.current = attempt;
    setPendingCheckout(attempt);
    busyRef.current = true;
    setCheckingOut(true);
    try {
      const { data } = await api.post('/sales', attempt.body, { timeout: 20000 });
      try { sessionStorage.removeItem(storageKey); } catch { /* A saved retry remains idempotent. */ }
      pendingRef.current = null;
      setPendingCheckout(null);
      replaceCart([]);
      setCashReceived('');
      setCardAmount('');
      setReceipt(data);
      setMessage({ type: 'success', text: `Venta #${data.id} registrada. Total: $${money(data.total)}. Cambio: $${money(data.changeAmount)}.` });
      loadProducts(); loadSales();
    } catch (error) {
      const status = error.response?.status;
      const uncertain = attempt.uncertain || !status || status >= 500;
      if (uncertain) {
        const saved = { ...attempt, uncertain: true };
        pendingRef.current = saved;
        setPendingCheckout(saved);
        try { sessionStorage.setItem(storageKey, JSON.stringify(saved)); } catch { /* Keep the in-memory request. */ }
        setMessage({ type: 'error', text: 'No se pudo confirmar la respuesta del cobro. El carrito queda protegido: pulsa Reintentar mismo cobro para recuperar la venta sin duplicarla.' });
      } else {
        try { sessionStorage.removeItem(storageKey); } catch { /* No sale was accepted. */ }
        pendingRef.current = null;
        setPendingCheckout(null);
        setQuoteState(null);
        setQuoteRefresh(value => value + 1);
        setMessage({ type: 'error', text: apiError(error, 'No fue posible registrar la venta') });
        loadProducts();
      }
    } finally { busyRef.current = false; setCheckingOut(false); }
  };

  const showSale = async (id, print = false) => {
    try {
      const { data } = await api.get(`/sales/${id}`, { timeout: 10000 });
      if (print) setReceipt(data); else setSelectedSale(data);
    } catch (error) { setMessage({ type: 'error', text: apiError(error, 'No se pudo consultar la venta') }); }
  };
  const handleCancel = async id => {
    if (!window.confirm(`¿Anular la venta #${id} y devolver sus productos al inventario?`)) return;
    try {
      const { data } = await api.post(`/sales/${id}/cancel`);
      if (selectedSale?.id === id) setSelectedSale(data);
      if (receipt?.id === id) setReceipt(data);
      setMessage({ type: 'success', text: `Venta #${id} anulada` });
      loadProducts(); loadSales();
    } catch (error) { setMessage({ type: 'error', text: apiError(error, 'No se pudo anular la venta') }); }
  };

  return <div>
    <NavBar />
    <div className="page-container catalog-page">
      <div className="catalog-page-heading"><div><span className="page-eyebrow">CATÁLOGO / PUNTO DE VENTA</span><h1>Escanea, agrega y cobra</h1><p>Usa tu lector USB o escribe un código de barras.</p></div><a className="cart-indicator" href="#sale-cart">Carrito <span>{cart.reduce((sum, item) => sum + item.quantity, 0)}</span></a></div>
      <BarcodeScanner onProduct={addToCart} onMessage={setMessage} pending={pendingScans} onPending={count => { scansRef.current = count; setPendingScans(count); }} disabled={locked} />
      <div className={`pos-message ${message?.type || ''}`} role="status" aria-live="polite" aria-atomic="true">{message?.text || 'Listo para registrar productos.'}</div>
      <div className="pos-layout">
        <ProductCatalog products={products} loading={loading} error={loadError} onRetry={loadProducts} renderAction={product => {
          const quantity = cart.find(item => item.productId === product.id)?.quantity || 0;
          return <button className="product-add-button" disabled={locked || product.active === false || product.salePrice == null || product.salePrice <= 0 || product.stock <= quantity} onClick={() => handleCatalogAdd(product)} aria-label={`Agregar ${product.name} al carrito`}><span aria-hidden="true">+</span>{product.active === false ? 'Inactivo' : product.stock <= quantity ? 'Sin existencias' : 'Agregar'}</button>;
        }} />
        <aside id="sale-cart" className="card sale-cart" aria-label="Carrito de venta">
          <div className="cart-heading"><h3>Tu carrito</h3><span className="badge badge-success">{cart.reduce((sum, item) => sum + item.quantity, 0)} artículos</span></div>
          {cart.length === 0 && <div className="cart-empty"><strong>El carrito está vacío</strong><p>Escanea o agrega productos del catálogo.</p></div>}
          <ul className="cart-items">{cart.map(item => {
            const line = quote?.details?.find(detail => detail.productId === item.productId);
            return <li key={item.productId}>
              <div><strong>{item.name}</strong><span>{item.barcode || 'Sin código'} · ${money(line?.unitPrice ?? item.price)} c/u</span></div><strong>${money(line?.subtotal ?? item.price * item.quantity)}</strong>
              <div className="cart-quantity"><button disabled={locked || item.quantity <= 1} aria-label={`Reducir cantidad de ${item.name}`} onClick={() => changeQuantity(item.productId, item.quantity - 1)}>−</button><input type="number" min="1" step="1" max={item.stock} value={item.quantity} disabled={locked} aria-label={`Cantidad de ${item.name}`} onChange={event => changeQuantity(item.productId, Number(event.target.value))} /><button disabled={locked || item.quantity >= item.stock} aria-label={`Incrementar cantidad de ${item.name}`} onClick={() => changeQuantity(item.productId, item.quantity + 1)}>+</button></div>
              <button className="product-delete-button" disabled={locked} onClick={() => removeFromCart(item.productId)} aria-label={`Quitar ${item.name}`}>Quitar</button>
            </li>;
          })}</ul>
          <div className="cart-subtotal"><span>Subtotal</span><strong>{quoteMoney('subtotal')}</strong></div>
          <div className="cart-subtotal"><span>Impuesto</span><strong>{quoteMoney('tax')}</strong></div>
          <div className="cart-subtotal pos-total"><span>Total</span><strong>{quoteMoney('total')}</strong></div>
          {quoting && <p role="status">Calculando importes…</p>}
          {quoteError && <div className="pos-message error"><p>{quoteError}</p><button className="btn-secondary" onClick={() => { setQuoteRefresh(value => value + 1); loadProducts(); }}>Actualizar importes</button></div>}
          <label>Método de pago<select disabled={locked} value={paymentMethod} onChange={event => setPaymentMethod(event.target.value)}><option value="EFECTIVO">Efectivo</option><option value="TARJETA">Tarjeta</option><option value="MIXTO">Mixto</option></select></label>
          {paymentMethod === 'MIXTO' && <label>Importe con tarjeta<input type="number" min="0.01" step="0.01" value={cardAmount} disabled={locked} onChange={event => setCardAmount(event.target.value)} /></label>}
          {paymentMethod !== 'TARJETA' && <><label>Efectivo recibido<input type="number" min="0" step="0.01" value={cashReceived} disabled={locked} onChange={event => setCashReceived(event.target.value)} /></label><p>Cambio: {quote ? `$${money(Math.max(0, cashPaid - cashDue))}` : '—'}</p></>}
          <button className="btn-accent checkout-button" disabled={checkingOut || pendingScans > 0 || !pendingCheckout && (quoting || !quote || !paymentValid || !cart.length)} onClick={handleCheckout}>{checkingOut ? 'Registrando…' : pendingCheckout ? 'Reintentar mismo cobro' : 'Cobrar venta'}</button>
        </aside>
      </div>
      <SaleTicket sale={receipt} onClose={() => setReceipt(null)} />
      <div className="catalog-page-heading sales-history-heading"><h2>Historial de ventas</h2><button className="btn-secondary" onClick={loadSales}>Actualizar historial</button></div>
      {historyError && <p role="alert">{historyError}</p>}
      <div className="table-scroll"><table className="table"><thead><tr><th>Venta / Fecha</th><th>Total</th><th>Pago</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{sales.map(sale => <tr key={sale.id}><td>#{sale.id}<br />{new Date(sale.saleDate).toLocaleString('es-MX')}</td><td>${money(sale.total)}</td><td>{sale.paymentMethod}</td><td>{sale.status}</td><td><button onClick={() => showSale(sale.id)}>Ver detalle</button>{' '}<button className="btn-secondary" onClick={() => showSale(sale.id, true)}>Ver ticket</button>{' '}{canCancel && sale.status === 'COMPLETADA' && <button className="btn-danger" disabled={locked} onClick={() => handleCancel(sale.id)}>Anular</button>}</td></tr>)}</tbody></table></div>
      {selectedSale && <section className="card" aria-label="Detalle de venta"><h3>Detalle de venta #{selectedSale.id}</h3><p>Cliente: {selectedSale.customerName} · Cajero: {selectedSale.cashierName || '—'}</p><p>Estado: {selectedSale.status} · Pago: {selectedSale.paymentMethod}</p><div className="table-scroll"><table className="table"><thead><tr><th>Producto / Código</th><th>Cantidad</th><th>Precio</th><th>Subtotal</th></tr></thead><tbody>{selectedSale.details.map((item, index) => <tr key={index}><td>{item.productName}<br />{item.barcode || item.sku}</td><td>{item.quantity}</td><td>${money(item.unitPrice)}</td><td>${money(item.subtotal)}</td></tr>)}</tbody></table></div><p>Subtotal: ${money(selectedSale.subtotal)} · Impuesto: ${money(selectedSale.tax)} · Descuento: ${money(selectedSale.discount)}</p><p><strong>Total: ${money(selectedSale.total)}</strong></p><button onClick={() => setSelectedSale(null)}>Cerrar detalle</button></section>}
    </div>
  </div>;
}

export default SalesPage;
