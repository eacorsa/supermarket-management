import { useEffect, useState } from 'react';
import api from '../services/api';
import NavBar from '../components/NavBar';

const emptyForm = {
  name: '',
  sku: '',
  purchasePrice: '',
  salePrice: '',
  stock: '',
  minStock: '',
  unitOfMeasure: ''
};

function ProductsPage() {
  const [products, setProducts] = useState([]);
  const [form, setForm] = useState(emptyForm);

  const loadProducts = () => {
    api.get('/products').then((res) => setProducts(res.data)).catch(() => alert('No se pudieron cargar los productos'));
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/products', {
        name: form.name,
        sku: form.sku,
        purchasePrice: Number(form.purchasePrice),
        salePrice: Number(form.salePrice),
        stock: Number(form.stock),
        minStock: form.minStock === '' ? null : Number(form.minStock),
        unitOfMeasure: form.unitOfMeasure || null
      });
      setForm(emptyForm);
      loadProducts();
    } catch (error) {
      alert(error.response?.data?.message || 'No se pudo crear el producto');
    }
  };

  return (
    <div>
      <NavBar />
      <div className="page-container">
      <h2>Productos e inventario</h2>

      <form onSubmit={handleSubmit} className="card form-row">
        <input placeholder="Nombre" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        <input placeholder="SKU" value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} required />
        <input type="number" min="0" step="0.01" placeholder="Precio compra" value={form.purchasePrice} onChange={(e) => setForm({ ...form, purchasePrice: e.target.value })} required />
        <input type="number" min="0" step="0.01" placeholder="Precio venta" value={form.salePrice} onChange={(e) => setForm({ ...form, salePrice: e.target.value })} required />
        <input type="number" min="0" placeholder="Stock inicial" value={form.stock} onChange={(e) => setForm({ ...form, stock: e.target.value })} required />
        <input type="number" min="0" placeholder="Stock mínimo" value={form.minStock} onChange={(e) => setForm({ ...form, minStock: e.target.value })} />
        <input placeholder="Unidad de medida" value={form.unitOfMeasure} onChange={(e) => setForm({ ...form, unitOfMeasure: e.target.value })} />
        <button type="submit">Agregar</button>
      </form>

      <table className="table">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>SKU</th>
            <th>Stock</th>
            <th>Precio venta</th>
          </tr>
        </thead>
        <tbody>
          {products.map((product) => (
            <tr key={product.id}>
              <td>{product.name}</td>
              <td>{product.sku}</td>
              <td>{product.stock}</td>
              <td>${product.salePrice}</td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </div>
  );
}

export default ProductsPage;
