import { useState } from 'react';

const sampleImages = {
  'ARZ-001': 'rice',
  'AZU-001': 'sugar',
  'LEC-001': 'milk',
  'QUE-001': 'cheese',
  'BEB-001': 'soda'
};

export default function ProductImage({ imageUrl, name, sku, className = '' }) {
  const [failedUrl, setFailedUrl] = useState(null);
  const fallback = `/images/products/${sampleImages[sku] || 'placeholder'}.svg`;
  const trimmedUrl = imageUrl?.trim();
  const validUrl = /^https?:\/\//i.test(trimmedUrl || '');
  const src = validUrl && failedUrl !== trimmedUrl ? trimmedUrl : fallback;

  return <img
    className={`product-image ${className}`}
    src={src}
    alt={name ? `Imagen de ${name}` : 'Vista previa del producto'}
    width="80"
    height="80"
    loading="lazy"
    referrerPolicy="no-referrer"
    onError={() => { if (src !== fallback) setFailedUrl(trimmedUrl); }}
  />;
}
