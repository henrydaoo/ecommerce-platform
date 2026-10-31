"use client";
import { useEffect, useState } from "react";
import { api, Product } from "../../../lib/api";
export default function ProductPage({ params }: { params: { id: string } }) {
  const [p, setP] = useState<Product>();
  const [m, setM] = useState("");
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);
  useEffect(() => {
    let active = true;
    setLoading(true);
    api
      .request<Product>(`/products/${params.id}`)
      .then((product) => {
        if (active) setP(product);
      })
      .catch((cause: unknown) => {
        if (active) {
          setM(
            cause instanceof Error
              ? cause.message
              : "Unable to load this product.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [params.id]);
  if (loading) return <main className="muted">Loading product...</main>;
  if (!p) {
    return (
      <main>
        <p className="error" role="alert">
          {m}
        </p>
      </main>
    );
  }
  return (
    <main className="product-detail">
      <div className="product-detail-art" aria-hidden="true">
        {p.name.slice(0, 1).toUpperCase()}
      </div>
      <section className="product-detail-copy">
        <p className="eyebrow">THE COLLECTION</p>
        <h1>{p.name}</h1>
        <p className="muted">
          {p.description || "A considered everyday essential."}
        </p>
        <p className="price">{p.price.toLocaleString()} VND</p>
        <p className="stock">
          {p.stockQuantity > 0
            ? `${p.stockQuantity} available`
            : "Currently sold out"}
        </p>
        <div className="product-buy-row">
          <label className="quantity-input">
            <span>Quantity</span>
            <input
              type="number"
              min={1}
              max={p.stockQuantity}
              value={quantity}
              disabled={p.stockQuantity < 1 || adding}
              onChange={(event) =>
                setQuantity(
                  Math.min(
                    p.stockQuantity,
                    Math.max(1, Number(event.target.value) || 1),
                  ),
                )
              }
            />
          </label>
          <button
            className="button button-primary"
            disabled={p.stockQuantity < 1 || adding}
            onClick={async () => {
              setAdding(true);
              setM("");
              try {
                await api.add(p.id, quantity);
                setM(`${quantity} added to your cart.`);
              } catch (cause) {
                setM(
                  cause instanceof Error
                    ? cause.message
                    : "Unable to add this item.",
                );
              } finally {
                setAdding(false);
              }
            }}
          >
            {adding ? "Adding..." : "Add to cart"}
          </button>
        </div>
        {m && (
          <p
            className={m.includes("added") ? "success" : "error"}
            role="status"
          >
            {m}
          </p>
        )}
      </section>
    </main>
  );
}
