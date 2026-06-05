import { NextRequest, NextResponse } from 'next/server';

/**
 * Camada BFF: repassa as requisições de /api/** do Next para o backend Java
 * (Spring Boot), preservando método, corpo, query string e o header Authorization.
 *
 * O cliente continua chamando /api/... no Next; estes route handlers encaminham
 * ao Java e devolvem a resposta como veio. Não há mais lógica de negócio aqui.
 */
const BACKEND_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

const HOP_BY_HOP = new Set([
  'connection', 'keep-alive', 'transfer-encoding', 'te', 'trailer',
  'upgrade', 'proxy-authorization', 'proxy-authenticate', 'host', 'content-length',
]);

/**
 * Encaminha a request atual para o backend Java, mantendo o mesmo path /api/...
 * e a query string. Retorna a resposta do Java intacta (status + JSON).
 */
export async function proxy(req: NextRequest): Promise<NextResponse> {
  const { pathname, search } = req.nextUrl;
  const target = `${BACKEND_URL}${pathname}${search}`;

  const headers = new Headers();
  // Repassa apenas headers relevantes (Authorization, Content-Type, Accept).
  const auth = req.headers.get('authorization');
  if (auth) headers.set('authorization', auth);
  const contentType = req.headers.get('content-type');
  if (contentType) headers.set('content-type', contentType);
  const accept = req.headers.get('accept');
  if (accept) headers.set('accept', accept);
  // Encaminha IP de origem para o rate limit/lockout do backend.
  const fwd = req.headers.get('x-forwarded-for');
  if (fwd) headers.set('x-forwarded-for', fwd);

  const method = req.method.toUpperCase();
  let body: string | undefined;
  if (method !== 'GET' && method !== 'HEAD') {
    const text = await req.text();
    if (text) body = text;
  }

  let upstream: Response;
  try {
    upstream = await fetch(target, { method, headers, body, redirect: 'manual' });
  } catch {
    return NextResponse.json(
      { error: 'Sem conexão com o servidor. Tente novamente.' },
      { status: 502 },
    );
  }

  // Devolve o corpo como veio, removendo headers hop-by-hop.
  const respHeaders = new Headers();
  upstream.headers.forEach((value, key) => {
    if (!HOP_BY_HOP.has(key.toLowerCase())) respHeaders.set(key, value);
  });
  const respBody = await upstream.arrayBuffer();
  return new NextResponse(respBody, {
    status: upstream.status,
    headers: respHeaders,
  });
}
