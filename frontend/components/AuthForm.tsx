"use client";
import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "../context/AuthContext";
export function AuthForm({ register = false }: { register?: boolean }) {
  const { login } = useAuth(),
    r = useRouter();
  const [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [fullName, setName] = useState(""),
    [error, setError] = useState(""),
    [isSubmitting, setIsSubmitting] = useState(false);
  return (
    <main className="auth-page">
      <section className="auth-story">
        <p className="eyebrow">STUDIO STORE</p>
        <h1>Make room for things that matter.</h1>
        <p>
          Thoughtfully chosen essentials, with a simpler way to shop and keep
          track of every order.
        </p>
        <span className="auth-story-mark" aria-hidden="true">
          S
        </span>
      </section>
      <section className="panel auth-panel">
        <p className="eyebrow">{register ? "YOUR ACCOUNT" : "WELCOME BACK"}</p>
        <h2>{register ? "Create your account" : "Sign in"}</h2>
        <p className="muted">
          {register
            ? "Save your cart and keep track of every order."
            : "Enter your details to continue to your account."}
        </p>
        <form
          onSubmit={async (event: FormEvent<HTMLFormElement>) => {
            event.preventDefault();
            setError("");
            setIsSubmitting(true);
            try {
              await login(email, password, register, fullName);
              r.push("/");
            } catch (cause) {
              setError(
                cause instanceof Error ? cause.message : "Unable to sign in.",
              );
            } finally {
              setIsSubmitting(false);
            }
          }}
        >
          {register && (
            <label htmlFor="full-name">
              Full name
              <input
                autoComplete="name"
                id="full-name"
                required
                value={fullName}
                onChange={(event) => setName(event.target.value)}
              />
            </label>
          )}
          <label htmlFor="email">
            Email address
            <input
              autoComplete="email"
              id="email"
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          <label htmlFor="password">
            Password
            <input
              autoComplete={register ? "new-password" : "current-password"}
              id="password"
              minLength={register ? 8 : undefined}
              type="password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}
          <button className="button button-primary" disabled={isSubmitting}>
            {isSubmitting
              ? "Please wait..."
              : register
                ? "Create account"
                : "Sign in"}
          </button>
        </form>
        <p className="auth-switch">
          {register ? "Already have an account?" : "New to Studio Store?"}{" "}
          <Link href={register ? "/login" : "/register"}>
            {register ? "Sign in" : "Create an account"}
          </Link>
        </p>
        {!register && (
          <p className="auth-forgot">
            <Link href="/forgot-password">Forgot your password?</Link>
          </p>
        )}
      </section>
    </main>
  );
}
