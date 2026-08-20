import { useEffect, useState } from 'react';
import api from '../services/api';
import NavBar from '../components/NavBar';

function PurchasesPage() {
  const [purchases, setPurchases] = useState([]);

  const loadPurchases = () => {
    api.get('/purchases').then((res) => setPurchases(res.data)).catch(() => alert('No se pudieron cargar las compras'));
  };

  useEffect(() => {
    loadPurchases();
  }, []);

  const handleReceive = async (id) => {
    try {
      await api.post(`/purchases/${id}/receive`);
      loadPurchases();
    } catch (error) {
      alert('No se pudo recibir la mercadería');
    }
  };

  return (
    <div>
      <NavBar />
      <div className="page-container">
      <h2>Órdenes de compra</h2>
      <table className="table">
        <thead>
          <tr>
            <th>Proveedor</th>
            <th>Total</th>
            <th>Estado</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          {purchases.map((purchase) => (
            <tr key={purchase.id}>
              <td>{purchase.supplierName}</td>
              <td>${purchase.total}</td>
              <td><span className={`badge ${purchase.status === 'PENDING' ? 'badge-warning' : 'badge-success'}`}>{purchase.status}</span></td>
              <td>
                {purchase.status === 'PENDING' && (
                  <button onClick={() => handleReceive(purchase.id)}>Recibir mercadería</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </div>
  );
}

export default PurchasesPage;
