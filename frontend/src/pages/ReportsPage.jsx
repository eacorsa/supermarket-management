import { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import api from '../services/api';
import NavBar from '../components/NavBar';

function toIsoDate(date) {
  return date.toISOString().split('T')[0];
}

function ReportsPage() {
  const today = new Date();
  const monthAgo = new Date();
  monthAgo.setDate(today.getDate() - 30);

  const [startDate, setStartDate] = useState(toIsoDate(monthAgo));
  const [endDate, setEndDate] = useState(toIsoDate(today));
  const [summary, setSummary] = useState(null);
  const [topProducts, setTopProducts] = useState([]);
  const [lowStock, setLowStock] = useState([]);

  const loadSummary = () => {
    api.get('/reports/sales-summary', { params: { startDate, endDate } })
      .then((res) => setSummary(res.data))
      .catch(() => alert('No se pudo cargar el resumen de ventas'));
  };

  const loadTopProducts = () => {
    api.get('/reports/top-products', { params: { limit: 5 } })
      .then((res) => setTopProducts(res.data))
      .catch(() => alert('No se pudo cargar el ranking de productos'));
  };

  const loadLowStock = () => {
    api.get('/reports/low-stock')
      .then((res) => setLowStock(res.data))
      .catch(() => alert('No se pudo cargar el stock crítico'));
  };

  useEffect(() => {
    loadTopProducts();
    loadLowStock();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    loadSummary();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [startDate, endDate]);

  return (
    <div>
      <NavBar />
      <div className="page-container">
      <h2>Reportes y dashboard</h2>

      <div className="card form-row">
        <label>Desde: <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} /></label>
        <label>Hasta: <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} /></label>
      </div>

      {summary && (
        <div className="stats-row">
          <div className="stat-card"><strong>Ventas</strong><span>{summary.totalSalesCount}</span></div>
          <div className="stat-card"><strong>Ingresos</strong><span>${summary.totalRevenue.toFixed(2)}</span></div>
          <div className="stat-card"><strong>Impuestos</strong><span>${summary.totalTax.toFixed(2)}</span></div>
        </div>
      )}

      <h3>Ventas por día</h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={summary?.dailyBreakdown ?? []}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="date" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Bar dataKey="revenue" name="Ingresos" fill="#4f46e5" />
          <Bar dataKey="salesCount" name="N° ventas" fill="#22c55e" />
        </BarChart>
      </ResponsiveContainer>

      <h3 style={{ marginTop: '2rem' }}>Producto más vendido</h3>
      <ResponsiveContainer width="100%" height={280}>
        <BarChart data={topProducts} layout="vertical">
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis type="number" />
          <YAxis type="category" dataKey="productName" width={150} />
          <Tooltip />
          <Bar dataKey="totalQuantity" name="Unidades vendidas" fill="#f97316" />
        </BarChart>
      </ResponsiveContainer>

      <h3 style={{ marginTop: '2rem' }}>Stock crítico</h3>
      {lowStock.length === 0 ? (
        <p>No hay productos en stock crítico.</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Producto</th>
              <th>Stock actual</th>
              <th>Stock mínimo</th>
            </tr>
          </thead>
          <tbody>
            {lowStock.map((product) => (
              <tr key={product.productId} className="row-warning">
                <td>{product.name}</td>
                <td>{product.stock}</td>
                <td>{product.minStock}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      </div>
    </div>
  );
}

export default ReportsPage;
