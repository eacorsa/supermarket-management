export const money = value => Number(value ?? 0).toFixed(2);

export function addCartProduct(cart, product) {
  if (product.active === false) throw new Error(`Producto inactivo: ${product.name}`);
  if (!Number.isFinite(product.salePrice) || product.salePrice <= 0) throw new Error(`Producto sin precio de venta: ${product.name}`);
  const existing = cart.find(item => item.productId === product.id);
  const quantity = (existing?.quantity || 0) + 1;
  if (!Number.isSafeInteger(product.stock) || quantity > product.stock) throw new Error(`Inventario insuficiente para ${product.name}`);
  const item = { productId: product.id, name: product.name, barcode: product.barcode, price: product.salePrice, stock: product.stock, quantity };
  return existing ? cart.map(entry => entry.productId === product.id ? item : entry) : [...cart, item];
}

export function setCartQuantity(cart, productId, quantity) {
  const item = cart.find(entry => entry.productId === productId);
  if (!item) return cart;
  if (!Number.isSafeInteger(quantity) || quantity <= 0) throw new Error('La cantidad debe ser un entero mayor que cero');
  if (quantity > item.stock) throw new Error(`Solo hay ${item.stock} unidades de ${item.name}`);
  return cart.map(entry => entry.productId === productId ? { ...entry, quantity } : entry);
}

// Every Enter is a scan, even for the same barcode. Requests are ordered, never debounced.
export function createScanQueue({ lookup, onProduct, onError, onPending }) {
  let tail = Promise.resolve();
  let pending = 0;
  let disposed = false;
  const controller = new AbortController();
  return {
    enqueue(code) {
      if (disposed) return Promise.resolve();
      pending += 1;
      onPending(pending);
      const job = tail.then(async () => {
        try {
          if (!disposed) {
            const product = await lookup(code, controller.signal);
            if (!disposed) onProduct(product);
          }
        } catch (error) {
          if (!disposed) onError(error, code);
        } finally {
          pending -= 1;
          if (!disposed) onPending(pending);
        }
      });
      tail = job.catch(() => {});
      return job;
    },
    dispose() { disposed = true; controller.abort(); }
  };
}
