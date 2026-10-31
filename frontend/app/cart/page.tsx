"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { api, ApiError, Cart } from "../../lib/api";
import { CustomerOnly } from "../../components/CustomerOnly";

export default function CartPage() {
  return (
    <CustomerOnly>
      <CartContent />
    </CustomerOnly>
  );
}

function CartContent() {
  const [c, setC] = useState<Cart>();
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyItem, setBusyItem] = useState<number>();
  const [unauthorized, setUnauthorized] = useState(false);

  useEffect(() => {
    let active = true;
    api
      .cart()
      .then((cart) => {
        if (active) setC(cart);
      })
      .catch((cause: unknown) => {
        if (active) {
          setUnauthorized(cause instanceof ApiError && cause.status === 401);
          setError(
            cause instanceof Error ? cause.message : "Unable to load your cart.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const updateCart = async (
    itemId: number,
    action: () => Promise<Cart>,
  ) => {
    setBusyItem(itemId);
    setError("");
    try {
      setC(await action());
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : "Unable to update your cart.",
      );
    } finally {
      setBusyItem(undefined);
    }
  };

  if (loading) return <main className="muted">Loading your cart...</main>;
  if (error && !c)
    return (
      <main>
        <p className="error" role="alert">{error}</p>
        {unauthorized ? (
          <Link className="button button-primary" href="/login">
            Sign in to view your cart
          </Link>
        ) : (
          <button
            className="button button-primary"
            onClick={() => window.location.reload()}
          >
            Try again
          </button>
        )}
      </main>
    );
  return (
    <main>
      <p className="eyebrow">YOUR SELECTION</p>
      <h1>Your cart</h1>
      {error && <p className="error" role="alert">{error}</p>}
      {!c || c.items.length === 0 ? (
        <section className="empty-state">
          <p>Your cart is waiting for something good.</p>
          <Link className="button button-primary" href="/">
            Browse the collection
          </Link>
        </section>
      ) : (
        <div className="cart-layout">
          <section className="panel stack" aria-label="Cart items">
            {c.items.map((item) => (
              <article className="cart-item" key={item.id}>
                <div>
                  <h2>{item.productName}</h2>
                  <p className="muted">
                    {item.unitPrice.toLocaleString()} VND each
                  </p>
                </div>
                <div className="quantity-control" aria-label="Quantity">
                  <button
                    className="button button-quiet"
                    aria-label={`Decrease ${item.productName} quantity`}
                    disabled={busyItem === item.id || item.quantity <= 1}
                    onClick={() =>
                      updateCart(item.id, () =>
                        api.updateCartItem(item.id, item.quantity - 1),
                      )
                    }
                  >
                    −
                  </button>
                  <span>{item.quantity}</span>
                  <button
                    className="button button-quiet"
                    aria-label={`Increase ${item.productName} quantity`}
                    disabled={busyItem === item.id}
                    onClick={() =>
                      updateCart(item.id, () =>
                        api.updateCartItem(item.id, item.quantity + 1),
                      )
                    }
                  >
                    +
                  </button>
                </div>
                <strong>{item.lineTotal.toLocaleString()} VND</strong>
                <button
                  className="button button-quiet remove-button"
                  disabled={busyItem === item.id}
                  onClick={() =>
                    updateCart(item.id, () => api.removeCartItem(item.id))
                  }
                >
                  Remove
                </button>
              </article>
            ))}
          </section>
          <aside className="panel order-summary">
            <h2>Order summary</h2>
          <p className="row">
            <span>Subtotal</span>
            <strong>{c.totalAmount.toLocaleString()} VND</strong>
          </p>
            <p className="muted">Shipping and payment details are confirmed at checkout.</p>
            <Link className="button button-primary" href="/checkout">
              Continue to checkout
            </Link>
          </aside>
        </div>
      )}
    </main>
  );
}
