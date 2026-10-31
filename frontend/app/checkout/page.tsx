"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, Cart } from "../../lib/api";
import { CustomerOnly } from "../../components/CustomerOnly";

export default function Checkout() {
  return (
    <CustomerOnly>
      <CheckoutContent />
    </CustomerOnly>
  );
}

function CheckoutContent() {
  const router = useRouter();
  const [cart, setCart] = useState<Cart>();
  const [method, setMethod] = useState<"COD" | "VNPAY">("COD");
  const [error, setError] = useState("");
  const [loadingCart, setLoadingCart] = useState(true);
  const [placingOrder, setPlacingOrder] = useState(false);

  useEffect(() => {
    let active = true;
    api
      .cart()
      .then((result) => {
        if (active) setCart(result);
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(
            cause instanceof Error
              ? cause.message
              : "Unable to load your cart.",
          );
        }
      })
      .finally(() => {
        if (active) setLoadingCart(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const placeOrder = async () => {
    setPlacingOrder(true);
    setError("");
    try {
      const order = await api.checkout(method);
      if (order.paymentUrl) window.location.assign(order.paymentUrl);
      else router.push(`/orders/${order.id}`);
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : "Unable to place your order.",
      );
      setPlacingOrder(false);
    }
  };

  if (loadingCart) return <main className="muted">Preparing checkout...</main>;
  if (!cart || cart.items.length === 0) {
    return (
      <main>
        <p className="eyebrow">CHECKOUT</p>
        <h1>Your basket is empty</h1>
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        <Link className="button button-primary" href="/">
          Continue shopping
        </Link>
      </main>
    );
  }

  return (
    <main>
      <Link className="back-link" href="/cart">
        ← Back to cart
      </Link>
      <div className="checkout-heading">
        <p className="eyebrow">FINAL STEP</p>
        <h1>Review &amp; pay</h1>
        <p className="muted">
          Choose how you would like to pay for your order.
        </p>
      </div>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <div className="checkout-layout">
        <section className="panel checkout-payment">
          <h2>Payment method</h2>
          <label
            className={`payment-option ${method === "COD" ? "selected" : ""}`}
          >
            <input
              type="radio"
              name="payment-method"
              value="COD"
              checked={method === "COD"}
              onChange={() => setMethod("COD")}
            />
            <span className="payment-option-icon" aria-hidden="true">
              ₫
            </span>
            <span>
              <strong>Cash on delivery</strong>
              <small>Pay when your order arrives.</small>
            </span>
          </label>
          <label
            className={`payment-option ${method === "VNPAY" ? "selected" : ""}`}
          >
            <input
              type="radio"
              name="payment-method"
              value="VNPAY"
              checked={method === "VNPAY"}
              onChange={() => setMethod("VNPAY")}
            />
            <span
              className="payment-option-icon payment-vnpay"
              aria-hidden="true"
            >
              V
            </span>
            <span>
              <strong>VNPay</strong>
              <small>Continue to the secure VNPay payment page.</small>
            </span>
          </label>
          <p className="muted checkout-note">
            Stock is reserved when the order is placed. For VNPay, the order is
            confirmed after the gateway sends its server notification.
          </p>
        </section>
        <aside className="panel checkout-summary">
          <p className="eyebrow">ORDER SUMMARY</p>
          <h2>
            {cart.items.length} {cart.items.length === 1 ? "item" : "items"}
          </h2>
          <div className="checkout-items">
            {cart.items.map((item) => (
              <div className="row" key={item.id}>
                <span>
                  {item.productName} <small>× {item.quantity}</small>
                </span>
                <strong>{item.lineTotal.toLocaleString()} VND</strong>
              </div>
            ))}
          </div>
          <div className="row checkout-total">
            <span>Total</span>
            <strong>{cart.totalAmount.toLocaleString()} VND</strong>
          </div>
          <button
            className="button button-primary"
            disabled={placingOrder}
            onClick={placeOrder}
          >
            {placingOrder ? "Placing order..." : "Place order"}
          </button>
        </aside>
      </div>
    </main>
  );
}
