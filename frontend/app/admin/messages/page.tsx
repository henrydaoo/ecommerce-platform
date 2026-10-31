"use client";
import { useCallback, useEffect, useState } from "react";
import { admin, EmailDeadLetter } from "../../../lib/api";

export default function AdminMessages() {
  const [messages, setMessages] = useState<EmailDeadLetter[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState<string>();

  const load = useCallback(
    async () => setMessages(await admin.emailDeadLetters()),
    [],
  );
  useEffect(() => {
    load().catch((cause: unknown) =>
      setError(
        cause instanceof Error
          ? cause.message
          : "Unable to load the email queue.",
      ),
    );
  }, [load]);

  return (
    <main className="admin-page">
      <div className="section-heading">
        <div>
          <p className="eyebrow">MESSAGE OPERATIONS</p>
          <h1>Email queue</h1>
        </div>
        <button
          className="button button-quiet"
          onClick={() =>
            load().catch((cause: unknown) =>
              setError(
                cause instanceof Error
                  ? cause.message
                  : "Unable to refresh the queue.",
              ),
            )
          }
        >
          Refresh
        </button>
      </div>
      <p className="muted">
        Review failed notifications and replay them after addressing the cause.
      </p>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {messages.length === 0 ? (
        <p className="empty-state">No unresolved email messages.</p>
      ) : (
        <section className="admin-order-list">
          {messages.map((message) => (
            <article className="panel dead-letter-card" key={message.eventId}>
              <div>
                <div className="row">
                  <strong>{message.eventType}</strong>
                  <span className="status-pill">{message.replayState}</span>
                </div>
                <p className="muted">Event {message.eventId}</p>
                <small className="muted">
                  Last failed {new Date(message.lastFailedAt).toLocaleString()}{" "}
                  · replayed {message.replayCount} times
                </small>
              </div>
              <button
                disabled={
                  message.replayState !== "OPEN" || busy === message.eventId
                }
                onClick={async () => {
                  setBusy(message.eventId);
                  setError("");
                  try {
                    await admin.replayEmailDeadLetter(message.eventId);
                    await load();
                  } catch (cause) {
                    setError(
                      cause instanceof Error
                        ? cause.message
                        : "Unable to replay this message.",
                    );
                  } finally {
                    setBusy(undefined);
                  }
                }}
              >
                {busy === message.eventId ? "Queuing..." : "Replay message"}
              </button>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
