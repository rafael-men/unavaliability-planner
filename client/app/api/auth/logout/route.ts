import { NextRequest, NextResponse } from 'next/server';
import { proxy } from '../../../lib/backend';

const COOKIE_NAME = 'auth_token';

export async function POST(req: NextRequest) {
  const upstream = await proxy(req);
  const res = new NextResponse(upstream.body, {
    status: upstream.status,
    headers: upstream.headers,
  });
  res.cookies.set(COOKIE_NAME, '', {
    httpOnly: true,
    sameSite: 'strict',
    path: '/',
    maxAge: 0,
    secure: process.env.NODE_ENV === 'production',
  });
  return res;
}
