"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { api, Category, Product } from "../lib/api";
export default function Home() {
  const [p, setP] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    api
      .categories()
      .then((result) => {
        if (active) setCategories(result);
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(
            cause instanceof Error
              ? cause.message
              : "Unable to load categories.",
          );
        }
      })
      .finally(() => {
        if (active) setLoadingCategories(false);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    api
      .products(q)
      .then((x) => {
        if (active) setP(x.content);
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(
            cause instanceof Error
              ? cause.message
              : "Unable to load products.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [q]);

  return (
    <main>
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">EVERYDAY, THOUGHTFULLY MADE</p>
          <h1>Good things for the way you live.</h1>
          <p>
            A small collection of useful essentials, chosen for quality,
            simplicity, and the little details.
          </p>
          <a className="button button-primary" href="#catalog">
            Explore the collection
          </a>
        </div>
      </section>
      <section className="category-section" aria-labelledby="category-title">
        <div className="section-heading">
          <div>
            <p className="eyebrow">SHOP BY INTEREST</p>
            <h2 id="category-title">Categories</h2>
          </div>
          <p className="muted">Browse a collection that fits your day.</p>
        </div>
        {categories.length > 0 ? (
          <div className="category-grid">
            {categories.map((category) => (
              <Link
                className="category-card"
                href={`/categories/${encodeURIComponent(category.slug)}`}
                key={category.id}
              >
                <span aria-hidden="true">
                  {category.name.slice(0, 1).toUpperCase()}
                </span>
                <strong>{category.name}</strong>
                <small>Explore collection</small>
              </Link>
            ))}
          </div>
        ) : (
          loadingCategories ? (
            <p className="muted" role="status">
              Loading categories...
            </p>
          ) : !error ? (
            <p className="muted">No collections have been added yet.</p>
          ) : null
        )}
      </section>
      <section id="catalog">
        <div className="catalog-heading">
          <div>
            <p className="eyebrow">THE COLLECTION</p>
            <h2>Find your next favorite</h2>
          </div>
          <label className="search-input">
            <span className="sr-only">Search products</span>
            <input
              placeholder="Search the collection"
              value={q}
              onChange={(event) => setQ(event.target.value)}
            />
          </label>
        </div>
        {error ? (
          <p className="error" role="alert">
            {error}
          </p>
        ) : loading ? (
          <p className="muted" role="status">
            Loading products...
          </p>
        ) : p.length === 0 ? (
          <p className="empty-state">No products match your search yet.</p>
        ) : (
          <section className="grid" aria-label="Products">
            {p.map((product) => (
              <Link
                className="card product-card"
                href={`/products/${product.id}`}
                key={product.id}
              >
                <span className="product-art" aria-hidden="true">
                  {product.name.slice(0, 1).toUpperCase()}
                </span>
                <h3>{product.name}</h3>
                <p className="muted">{product.description}</p>
                <span className="product-meta">
                  <span className="price">
                    {product.price.toLocaleString()} VND
                  </span>
                  <small className="stock">
                    {product.stockQuantity > 0
                      ? `${product.stockQuantity} available`
                      : "Sold out"}
                  </small>
                </span>
              </Link>
            ))}
          </section>
        )}
      </section>
    </main>
  );
}
