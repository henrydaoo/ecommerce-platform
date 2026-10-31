const backendOrigin = (
  process.env.BACKEND_API_ORIGIN ?? "http://localhost:8080"
).replace(/\/$/, "");

                                        
const nextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/v1/:path*",
        destination: `${backendOrigin}/api/v1/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
