import { createServer } from 'node:http';

export const MENU = { classic: 250, pink: 300, mint: 350 }; // prices in cents

export class InvalidOrderError extends Error {}

// Business rule: an order of 10 cups or more gets 10% off.
export function priceOrder(order) {
  let cups = 0;
  let subtotal = 0;
  for (const [drink, quantity] of Object.entries(order)) {
    if (!(drink in MENU)) throw new InvalidOrderError(`Unknown drink: ${drink}`);
    if (!Number.isInteger(quantity) || quantity < 1) throw new InvalidOrderError(`Bad quantity for ${drink}`);
    cups += quantity;
    subtotal += MENU[drink] * quantity;
  }
  const discount = cups >= 10 ? Math.round(subtotal / 10) : 0;
  return { cups, totalCents: subtotal - discount };
}

export function createHandler(version) {
  return (request, response) => {
    const url = new URL(request.url, 'http://localhost');
    const send = (status, body) => {
      response.writeHead(status, { 'content-type': 'application/json' });
      response.end(JSON.stringify(body));
    };
    if (url.pathname === '/health') return send(200, { status: 'ok', version });
    if (url.pathname === '/') return send(200, { menu: MENU, version });
    if (url.pathname !== '/price') return send(404, { error: 'Not found' });
    try {
      const order = Object.fromEntries([...url.searchParams].map(([drink, n]) => [drink, Number(n)]));
      return send(200, priceOrder(order));
    } catch (error) {
      if (error instanceof InvalidOrderError) return send(400, { error: error.message });
      throw error;
    }
  };
}

// Configuration is read once, at startup, and passed in.
if (import.meta.main) {
  const port = Number(process.env.PORT ?? 8080);
  const server = createServer(createHandler(process.env.APP_VERSION ?? 'dev'));
  server.listen(port, () => console.log(`Lemonade Stand listening on ${port}`));
  process.on('SIGTERM', () => server.close()); // k3s sends SIGTERM before stopping a pod
}
