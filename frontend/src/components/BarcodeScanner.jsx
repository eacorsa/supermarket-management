import { useEffect, useRef } from 'react';
import api from '../services/api';
import { apiError } from '../utils/apiError';
import { createScanQueue } from '../utils/pos';

export default function BarcodeScanner({ onProduct, onMessage, onPending, disabled, pending }) {
  const input = useRef(null);
  const queue = useRef(null);
  const callbacks = useRef({ onProduct, onMessage, onPending });
  callbacks.current = { onProduct, onMessage, onPending };

  useEffect(() => { if (!disabled) input.current?.focus(); }, [disabled]);

  useEffect(() => {
    input.current?.focus();
    queue.current = createScanQueue({
      lookup: async (code, signal) => (await api.get('/products/barcode', { params: { code }, timeout: 10000, signal })).data,
      onProduct: product => callbacks.current.onProduct(product),
      onError: (error, code) => callbacks.current.onMessage({ type: 'error', text: `${code}: ${error.response ? apiError(error, 'No fue posible consultar el producto') : error.message && !error.isAxiosError ? error.message : 'Sin comunicación con el servidor. Comprueba la conexión y vuelve a escanear.'}` }),
      onPending: count => callbacks.current.onPending(count)
    });
    const shortcut = event => {
      if (event.key === 'F2') { event.preventDefault(); input.current?.focus(); }
    };
    window.addEventListener('keydown', shortcut);
    return () => { queue.current.dispose(); window.removeEventListener('keydown', shortcut); };
  }, []);

  return <form className="barcode-scanner card" onSubmit={event => {
    event.preventDefault();
    if (disabled) return;
    const code = input.current.value.trim();
    input.current.value = '';
    input.current.focus();
    if (!code) { onMessage({ type: 'error', text: 'Introduce o escanea un código de barras' }); return; }
    queue.current.enqueue(code);
  }}>
    <label htmlFor="pos-barcode">Escanear producto <small>USB / teclado · F2 para volver al lector</small></label>
    <div><input id="pos-barcode" ref={input} type="text" maxLength={255} autoComplete="off" autoCapitalize="off" spellCheck={false} disabled={disabled} placeholder="Escanea o escribe el código y pulsa Enter" aria-describedby="barcode-help" /><button type="submit" disabled={disabled}>Agregar código</button></div>
    <small id="barcode-help">EAN-13, UPC-A, Code 128 y otros códigos del catálogo. {pending > 0 ? `${pending} lectura(s) en proceso…` : 'Listo para escanear.'}</small>
  </form>;
}
