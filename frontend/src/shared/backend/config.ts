// Keep browser API calls same-origin so localhost, Cloudflare Tunnel, and a
// future production hostname all route through the Nginx gateway consistently.
export const BASE_API_URL = process.env.NEXT_PUBLIC_API_URL ?? '';
