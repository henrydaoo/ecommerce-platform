import "./globals.css";
import { AuthProvider } from "../context/AuthContext";
import { Nav } from "../components/Nav";

export const metadata = {
  title: "Studio Store | Thoughtful essentials",
  description: "A considered collection of everyday goods.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <Nav />
          {children}
          <footer className="site-footer">
            Considered essentials for everyday life.
          </footer>
        </AuthProvider>
      </body>
    </html>
  );
}
