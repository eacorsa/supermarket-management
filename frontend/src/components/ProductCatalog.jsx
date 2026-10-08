import { useState } from 'react';
import ProductImage from './ProductImage';

export default function ProductCatalog({ products, loading, error, onRetry, renderAction }) {
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [stockFilter, setStockFilter] = useState('all');
  const [sort, setSort] = useState('name');
  const categories = [...new Set(products.map(p => p.categoryName || 'Sin categoría'))].sort((a, b) => a.localeCompare(b, 'es'));
  const normalize = value => String(value).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  const filtered = products.filter(product => {
    const matchesSearch = normalize(`${product.name} ${product.sku}`).includes(normalize(search.trim()));
    const matchesCategory = !category || (product.categoryName || 'Sin categoría') === category;
    const matchesStock = stockFilter === 'all' || (stockFilter === 'available' ? product.stock > 0 : product.stock <= 0);
    return matchesSearch && matchesCategory && matchesStock;
  }).sort((a, b) => sort === 'price-asc' ? a.salePrice - b.salePrice : sort === 'price-desc' ? b.salePrice - a.salePrice : a.name.localeCompare(b.name, 'es'));

  return <div className="catalog-layout">
    <aside className="catalog-sidebar" aria-label="Filtros del catálogo">
      <h3>Categorías</h3>
      <div className="category-list">
        <button className={`category-option ${category === '' ? 'selected' : ''}`} aria-pressed={category === ''} onClick={() => setCategory('')}><span className="category-symbol" aria-hidden="true">▦</span><span>Todos los productos</span><span className="category-count">{products.length}</span></button>
        {categories.map(name => <button key={name} className={`category-option ${category === name ? 'selected' : ''}`} aria-pressed={category === name} onClick={() => setCategory(name)}><span className="category-symbol" aria-hidden="true">◇</span><span>{name}</span><span className="category-count">{products.filter(p => (p.categoryName || 'Sin categoría') === name).length}</span></button>)}
      </div>
      <div className="availability-filter">
        <label htmlFor="catalog-stock">Disponibilidad</label>
        <select id="catalog-stock" value={stockFilter} onChange={e => setStockFilter(e.target.value)}><option value="all">Todos</option><option value="available">Con existencias</option><option value="empty">Agotados</option></select>
      </div>
      <div className="catalog-note"><span aria-hidden="true">✦</span><strong>Todo a la vista</strong><p>Consulta precios y existencias de tu inventario en un solo lugar.</p></div>
    </aside>
    <section className="catalog-content" aria-label="Catálogo de productos">
      <div className="catalog-search"><span aria-hidden="true">⌕</span><input type="search" aria-label="Buscar productos por nombre o SKU" placeholder="¿Qué producto estás buscando?" value={search} onChange={e => setSearch(e.target.value)} /></div>
      <div className="catalog-toolbar"><span aria-live="polite"><strong>{filtered.length}</strong> {filtered.length === 1 ? 'producto encontrado' : 'productos encontrados'}</span><label>Ordenar por <select value={sort} onChange={e => setSort(e.target.value)}><option value="name">Nombre</option><option value="price-asc">Menor precio</option><option value="price-desc">Mayor precio</option></select></label></div>
      {loading ? <div className="catalog-empty" role="status">Cargando productos…</div> : error ? <div className="catalog-empty" role="alert"><p>{error}</p><button onClick={onRetry}>Volver a intentar</button></div> : filtered.length === 0 ? <div className="catalog-empty"><h3>No hay productos para mostrar</h3><p>Prueba con otro nombre, categoría o disponibilidad.</p><button className="btn-secondary" onClick={() => { setSearch(''); setCategory(''); setStockFilter('all'); }}>Limpiar filtros</button></div> : <div className="product-grid">
        {filtered.map(product => <article className="product-card" key={product.id}>
          <div className="product-card-visual">
            <span className={`product-stock-badge ${product.stock <= 0 ? 'out-of-stock' : product.minStock != null && product.stock <= product.minStock ? 'low-stock' : ''}`}>{product.stock <= 0 ? 'Agotado' : product.minStock != null && product.stock <= product.minStock ? 'Stock bajo' : 'Disponible'}</span>
            <ProductImage imageUrl={product.imageUrl} name={product.name} sku={product.sku} />
          </div>
          <div className="product-card-body"><strong className="product-price">${Number(product.salePrice).toFixed(2)}</strong><h3>{product.name}</h3><p className="product-unit">{product.unitOfMeasure || 'Unidad'} · {product.stock} en stock</p><span className="product-sku">SKU: {product.sku}</span>{renderAction && <div className="product-card-actions">{renderAction(product)}</div>}</div>
        </article>)}
      </div>}
    </section>
  </div>;
}
