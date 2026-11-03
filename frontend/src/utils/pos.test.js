import { test } from 'node:test';
import assert from 'node:assert/strict';
import { addCartProduct, setCartQuantity, createScanQueue } from './pos.js';

const product = { id: 1, name: 'Leche', barcode: '001234567890', salePrice: 12.5, stock: 2, active: true };

test('scanning twice increments the same line and preserves leading zeros', () => {
  const cart = addCartProduct(addCartProduct([], product), product);
  assert.equal(cart.length, 1);
  assert.equal(cart[0].quantity, 2);
  assert.equal(cart[0].barcode, '001234567890');
  assert.throws(() => addCartProduct(cart, product), /Inventario/);
});

test('cart rejects inactive, missing price, exhausted stock and invalid quantities', () => {
  for (const patch of [{ active: false }, { salePrice: null }, { salePrice: 0 }, { salePrice: NaN }, { stock: 0 }]) {
    assert.throws(() => addCartProduct([], { ...product, ...patch }));
  }
  const cart = addCartProduct([], product);
  for (const quantity of [0, -1, 1.5, 3, NaN]) assert.throws(() => setCartQuantity(cart, 1, quantity));
  assert.equal(setCartQuantity(cart, 1, 2)[0].quantity, 2);
  assert.equal(cart[0].quantity, 1);
});

test('rapid scans are serialized without dropping repeated codes', async () => {
  let active = 0;
  let cart = [];
  const order = [];
  const pending = [];
  const queue = createScanQueue({
    lookup: async code => {
      active += 1;
      assert.equal(active, 1);
      await new Promise(resolve => setTimeout(resolve, 5));
      order.push(code);
      active -= 1;
      return product;
    },
    onProduct: p => { cart = addCartProduct(cart, p); },
    onError: error => { throw error; },
    onPending: count => pending.push(count)
  });
  await Promise.all([queue.enqueue('001234567890'), queue.enqueue('001234567890')]);
  assert.deepEqual(order, ['001234567890', '001234567890']);
  assert.equal(cart[0].quantity, 2);
  assert.equal(pending.at(-1), 0);
  queue.dispose();
});

test('unknown code or failed communication does not stop the next scan', async () => {
  const errors = [];
  const found = [];
  const queue = createScanQueue({
    lookup: async code => { if (code !== 'OK') throw new Error('Sin conexión'); return product; },
    onProduct: p => found.push(p), onError: (_, code) => errors.push(code), onPending() {}
  });
  await Promise.all([queue.enqueue('UNKNOWN'), queue.enqueue('OK')]);
  assert.deepEqual(errors, ['UNKNOWN']);
  assert.equal(found.length, 1);
});

test('a stock error from the cart is reported without stopping further scans', async () => {
  let cart = [];
  let errors = 0;
  const queue = createScanQueue({ lookup: async () => ({ ...product, stock: 1 }), onProduct: p => { cart = addCartProduct(cart, p); }, onError: () => errors++, onPending() {} });
  await Promise.all([queue.enqueue('A'), queue.enqueue('A')]);
  assert.equal(cart[0].quantity, 1);
  assert.equal(errors, 1);
});

test('unmount aborts pending requests and ignores late results', async () => {
  let resolveLookup;
  let signal;
  let updates = 0;
  const queue = createScanQueue({ lookup: (_, s) => { signal = s; return new Promise(resolve => { resolveLookup = resolve; }); }, onProduct: () => updates++, onError: () => updates++, onPending() {} });
  const job = queue.enqueue('TEST');
  await Promise.resolve();
  queue.dispose();
  assert.equal(signal.aborted, true);
  resolveLookup(product);
  await job;
  assert.equal(updates, 0);
});
