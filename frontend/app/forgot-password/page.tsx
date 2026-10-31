"use client";
import { useState, type FormEvent } from "react";
import { api } from "../../lib/api";
export default function ForgotPassword() {
  const [email, setEmail] = useState(""),
    [done, setDone] = useState(false),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(false);
  return (
    <main className="form panel">
      <p className="eyebrow">ACCOUNT RECOVERY</p>
      <h1>Reset your password</h1>
      {done ? (
        <p role="status">
          If that email exists, a reset link has been sent.
        </p>
      ) : (
        <form
          onSubmit={async (event: FormEvent<HTMLFormElement>) => {
            event.preventDefault();
            setLoading(true);
            setError("");
            try {
              await api.request("/auth/forgot-password", {
                method: "POST",
                body: JSON.stringify({ email }),
              });
              setDone(true);
            } catch (cause) {
              setError(
                cause instanceof Error
                  ? cause.message
                  : "Unable to request a reset link.",
              );
            } finally {
              setLoading(false);
            }
          }}
        >
          <label htmlFor="email">
            Email
            <input
              autoComplete="email"
              id="email"
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          {error && <p className="error" role="alert">{error}</p>}
          <button className="button button-primary" disabled={loading}>
            {loading ? "Sending..." : "Send reset link"}
          </button>
        </form>
      )}
    </main>
  );
}
