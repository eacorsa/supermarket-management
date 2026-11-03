import { createPortal } from 'react-dom';
import { money } from '../utils/pos';

function TicketContent({ sale }) {
  return <>
    <h2>Supermercado</h2><p>Ticket de compra #{sale.id}</p>
    <p>{new Date(sale.saleDate).toLocaleString('es-MX')}<br />Cajero: {sale.cashierName || '—'}<br />Cliente: {sale.customerName}</p>
    {sale.status === 'ANULADA' && <strong>VENTA ANULADA</strong>}
    <table><thead><tr><th>Producto</th><th>Cant.</th><th>Importe</th></tr></thead><tbody>{(sale.details || []).map((item, index) => <tr key={index}><td>{item.productName}<small>{item.barcode || item.sku}<br />${money(item.unitPrice)} c/u</small></td><td>{item.quantity}</td><td>${money(item.subtotal)}</td></tr>)}</tbody></table>
    <dl><div><dt>Subtotal</dt><dd>${money(sale.subtotal)}</dd></div><div><dt>Impuesto</dt><dd>${money(sale.tax)}</dd></div><div><dt>Descuento</dt><dd>${money(sale.discount)}</dd></div><div className="ticket-total"><dt>Total</dt><dd>${money(sale.total)}</dd></div></dl>
    <p>Pago: {sale.paymentMethod}</p>
    {sale.cashReceived != null && <dl><div><dt>Efectivo recibido</dt><dd>${money(sale.cashReceived)}</dd></div><div><dt>Tarjeta</dt><dd>${money(sale.cardAmount)}</dd></div><div><dt>Cambio</dt><dd>${money(sale.changeAmount)}</dd></div></dl>}
    <p>Gracias por tu compra</p>
  </>;
}

export default function SaleTicket({ sale, onClose }) {
  if (!sale) return null;
  return <section className="card receipt-panel" aria-label={`Ticket de compra ${sale.id}`}>
    <div className="ticket-content"><TicketContent sale={sale} /></div>
    <div className="ticket-actions"><button onClick={() => window.print()}>Imprimir ticket</button><button className="btn-secondary" onClick={onClose}>Cerrar ticket</button></div>
    {createPortal(<div className="pos-receipt-print"><TicketContent sale={sale} /></div>, document.body)}
  </section>;
}
