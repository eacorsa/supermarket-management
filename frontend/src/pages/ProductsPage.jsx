import { useEffect, useRef, useState } from 'react';
import api from '../services/api';
import { getRoles } from '../utils/jwt';
import { apiError } from '../utils/apiError';
import NavBar from '../components/NavBar';
import ProductImage from '../components/ProductImage';
import ProductCatalog from '../components/ProductCatalog';

const emptyForm = {
  name: '',
  sku: '',
  imageUrl: '',
  purchasePrice: '',
  salePrice: '',
  stock: '',
  minStock: '',
  unitOfMeasure: ''
};

function ProductsPage() {
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [editorOpen, setEditorOpen] = useState(false);
  const editorRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const canManage = getRoles().some(role => ["ADMIN","GERENTE","ALMACENISTA"].includes(role));
  const [products, setProducts] = useState([]);
  const [form, setForm] = useState(emptyForm);

  const loadProducts = () => {
    setLoading(true);
    setLoadError('');
    return api.get('/products').then((res) => setProducts(res.data)).catch(() => setLoadError('No se pudieron cargar los productos')).finally(() => setLoading(false));
  };

  useEffect(() => {
    loadProducts();
  }, []);

  useEffect(() => {
    if (!editorRef.current) return;
    if (editorOpen && !editorRef.current.open) editorRef.current.showModal();
    if (!editorOpen && editorRef.current.open) editorRef.current.close();
  }, [editorOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (saving) return;
    try {
      setSaving(true);
      await api.request({ method: editingId === null ? 'post' : 'put', url: editingId === null ? '/products' : `/products/${editingId}`, data: {
        name: form.name,
        sku: form.sku,
        imageUrl: form.imageUrl.trim() || null,
        purchasePrice: Number(form.purchasePrice),
        salePrice: Number(form.salePrice),
        stock: Number(form.stock),
        minStock: form.minStock === '' ? null : Number(form.minStock),
        unitOfMeasure: form.unitOfMeasure || null,
        categoryId: form.categoryId ?? null,
        supplierId: form.supplierId ?? null
      } });
      setEditingId(null);
      setForm(emptyForm);
      setEditorOpen(false);
      loadProducts();
    } catch (error) {
      alert(apiError(error, 'No se pudo guardar el producto'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este producto? Los productos con historial no se pueden eliminar.')) return;
    try {
      await api.delete(`/products/${id}`);
      if (editingId === id) { setEditingId(null); setForm(emptyForm); }
      loadProducts();
    } catch (error) { alert(apiError(error, 'No se pudo eliminar el producto')); }
  };

  return (
    <div>
      <NavBar />
      <div className="page-container catalog-page">
      <div className="catalog-page-heading"><div><span className="page-eyebrow">CATÁLOGO / INVENTARIO</span><h1>Productos e inventario</h1><p>Administra tus productos, precios e imágenes.</p></div>{canManage && <button className="btn-accent" onClick={() => { setEditingId(null); setForm(emptyForm); setEditorOpen(true); }}>+ Nuevo producto</button>}</div>

      {canManage && <dialog className="product-editor-dialog" aria-labelledby="product-editor-title" ref={editorRef} onCancel={e => { e.preventDefault(); if (!saving) setEditorOpen(false); }}>
        <div className="editor-heading"><h2 id="product-editor-title">{editingId === null ? 'Nuevo producto' : 'Editar producto'}</h2><button type="button" className="btn-secondary" aria-label="Cerrar formulario" disabled={saving} onClick={() => setEditorOpen(false)}>×</button></div>
        <form onSubmit={handleSubmit} className="form-row product-editor-form">
        <fieldset disabled={saving} className="product-editor-fields">
        <label>Nombre
        <input placeholder="Nombre" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        </label><label>SKU
        <input placeholder="SKU" value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} required />
        </label><label>Precio de compra
        <input type="number" min="0" step="0.01" placeholder="Precio compra" value={form.purchasePrice} onChange={(e) => setForm({ ...form, purchasePrice: e.target.value })} required />
        </label><label>Precio de venta
        <input type="number" min="0" step="0.01" placeholder="Precio venta" value={form.salePrice} onChange={(e) => setForm({ ...form, salePrice: e.target.value })} required />
        </label><label>Stock
        <input type="number" min="0" placeholder="Stock inicial" value={form.stock} onChange={(e) => setForm({ ...form, stock: e.target.value })} required />
        </label><label>Stock mínimo
        <input type="number" min="0" placeholder="Stock mínimo" value={form.minStock} onChange={(e) => setForm({ ...form, minStock: e.target.value })} />
        </label><label>Unidad de medida
        <input placeholder="Unidad de medida" value={form.unitOfMeasure} onChange={(e) => setForm({ ...form, unitOfMeasure: e.target.value })} />
        </label></fieldset>
        <div className="product-image-editor">
          <ProductImage imageUrl={form.imageUrl} name={form.name} sku={form.sku} />
          <div className="product-image-fields">
            <label htmlFor="product-image-url">Imagen del producto (opcional)</label>
            <input id="product-image-url" type="url" maxLength={2048} pattern="[hH][tT][tT][pP][sS]?://.*" placeholder="https://ejemplo.com/producto.jpg" value={form.imageUrl} disabled={saving} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} aria-describedby="product-image-help" />
            <small id="product-image-help">Pega el enlace directo a una imagen pública. Sin enlace se muestra una ilustración de respaldo.</small>
            {form.imageUrl && <button type="button" className="btn-secondary" disabled={saving} onClick={() => setForm({ ...form, imageUrl: '' })}>Quitar imagen</button>}
          </div>
        </div>
        <button type="submit" disabled={saving}>{saving ? 'Guardando…' : editingId === null ? 'Agregar' : 'Guardar cambios'}</button>
        <button type="button" className="btn-secondary" disabled={saving} onClick={() => setEditorOpen(false)}>Cancelar</button>
      </form></dialog>}

      <ProductCatalog products={products} loading={loading} error={loadError} onRetry={loadProducts} renderAction={canManage ? product => <><button className="btn-secondary" disabled={saving} onClick={() => { setEditingId(product.id); setForm({ ...product, imageUrl: product.imageUrl ?? '', minStock: product.minStock ?? '', unitOfMeasure: product.unitOfMeasure ?? '' }); setEditorOpen(true); }}>Editar</button><button className="product-delete-button" aria-label={`Eliminar ${product.name}`} disabled={saving} onClick={() => handleDelete(product.id)}>Eliminar</button></> : undefined} />
      </div>
    </div>
  );
}

export default ProductsPage;
