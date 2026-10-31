"use client";
import { useSearchParams, useRouter } from "next/navigation";
import { Suspense, useState } from "react";
import { api } from "../../lib/api";
export default function ResetPassword() {
  return (
    <Suspense fallback={<main className="panel">Loading password reset...</main>}>
      <ResetPasswordForm />
    </Suspense>
  );
}

function ResetPasswordForm() {
  const token = useSearchParams().get("token") ?? "",
    router = useRouter(),
    [password, setPassword] = useState(""),
    [message, setMessage] = useState(""),
    [loading, setLoading] = useState(false);
  return (
    <main className="form panel">
      <p className="eyebrow">ACCOUNT RECOVERY</p>
      <h1>Choose a new password</h1>
      <form
        onSubmit={async (event) => {
          event.preventDefault();
          setLoading(true);
          setMessage("");
          try {
            await api.request("/auth/reset-password", {
              method: "POST",
              body: JSON.stringify({ token, newPassword: password }),
            });
            setMessage("Password updated.");
            setTimeout(() => router.push("/login"), 800);
          } catch (cause) {
            setMessage(
              cause instanceof Error
                ? cause.message
                : "Unable to update your password.",
            );
          } finally {
            setLoading(false);
          }
        }}
      >
        <label htmlFor="new-password">
          New password
          <input
            autoComplete="new-password"
            id="new-password"
            type="password"
            minLength={8}
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        <button className="button button-primary" disabled={loading || !token}>
          {loading ? "Updating..." : "Update password"}
        </button>
        {message && (
          <p className={message.startsWith("Unable") ? "error" : "muted"} role="status">
            {message}
          </p>
        )}
      </form>
    </main>
  );
}
