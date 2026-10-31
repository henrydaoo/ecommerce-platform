"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api, Category, Product } from "../../../lib/api";

export default function CategoryPage({ params }: { params: { slug: string } }) {
  const [category, setCategory] = useState<Category>();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    api
      .categories()
      .then(async (categories) => {
        const match = categories.find((item) => item.slug === params.slug);
        if (!match) {
          throw new Error("This category could not be found.");
        }
        const response = await api.products("", match.id);
        if (active) {
          setCategory(match);
          setProducts(response.content);
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(
            cause instanceof Error
              ? cause.message
              : "Unable to load this category.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [params.slug]);

  return (
    <main>
      <Link className="back-link" href="/">
        ← All collections
      </Link>
      {loading ? (
        <p className="muted" role="status">
          Loading category...
        </p>
      ) : error ? (
        <section className="empty-state">
          <p className="error" role="alert">
            {error}
          </p>
          <Link className="button button-primary" href="/">
            Browse all collections
          </Link>
        </section>
      ) : (
        <>
          <header className="category-page-heading">
            <p className="eyebrow">THE COLLECTION</p>
            <h1>{category?.name}</h1>
            <p className="muted">
              Thoughtful picks from the {category?.name.toLowerCase()} range.
            </p>
          </header>
          {products.length === 0 ? (
            <p className="empty-state">
              No products are available in this collection yet.
            </p>
          ) : (
            <section className="grid" aria-label={`${category?.name} products`}>
              {products.map((product) => (
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
        </>
      )}
    </main>
  );
}
