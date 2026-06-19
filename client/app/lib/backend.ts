import { NextRequest, NextResponse } from 'next/server';


const BACKEND_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

const HOP_BY_HOP = new Set([
  'connection', 'keep-alive', 'transfer-encoding', 'te', 'trailer',
  'upgrade', 'proxy-authorization', 'proxy-authenticate', 'host', 'content-length',
]);


export async function proxy(req: NextRequest): Promise<NextResponse> {
  const { pathname, search } = req.nextUrl;
  const target = `${BACKEND_URL}${pathname}${search}`;

  const headers = new Headers();
  const cookieToken = req.cookies.get('auth_token')?.value;
  const incomingAuth = req.headers.get('authorization');
  const auth = cookieToken ? `Bearer ${cookieToken}` : incomingAuth;
  if (auth) headers.set('authorization', auth);
  const contentType = req.headers.get('content-type');
  if (contentType) headers.set('content-type', contentType);
  const accept = req.headers.get('accept');
  if (accept) headers.set('accept', accept);
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
