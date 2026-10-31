import Link from "next/link";

const sections = [
  {
    href: "/admin/categories",
    title: "Categories",
    description: "Organize the store's product collection.",
  },
  {
    href: "/admin/products",
    title: "Products",
    description: "Create products and manage available stock.",
  },
  {
    href: "/admin/orders",
    title: "Orders",
    description: "Review orders and update fulfillment status.",
  },
  {
    href: "/admin/users",
    title: "Users",
    description: "View registered customers and administrator accounts.",
  },
  {
    href: "/admin/messages",
    title: "Email queue",
    description:
      "Inspect and replay messages that reached the dead-letter queue.",
  },
];

export default function Admin() {
  return (
    <main className="admin-page">
      <p className="eyebrow">STORE OPERATIONS</p>
      <h1>Good afternoon.</h1>
      <p className="muted">Choose an area to manage.</p>
      <section className="admin-section-grid">
        {sections.map((section) => (
          <Link
            className="panel admin-section-card"
            href={section.href}
            key={section.href}
          >
            <span className="admin-card-arrow" aria-hidden="true">
              ↗
            </span>
            <h2>{section.title}</h2>
            <p className="muted">{section.description}</p>
          </Link>
        ))}
      </section>
    </main>
  );
}
