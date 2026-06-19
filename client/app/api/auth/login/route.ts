import { NextRequest, NextResponse } from 'next/server';
import { proxy } from '../../../lib/backend';

const COOKIE_NAME = 'auth_token';
const COOKIE_MAX_AGE = 7 * 24 * 60 * 60; 

export async function POST(req: NextRequest) {
  const upstream = await proxy(req);


  try {
    const body = await upstream.json();
    if (body?.token) {
      const res = NextResponse.json(
        { ...body, token: undefined },
        { status: upstream.status },
      );
      res.cookies.set(COOKIE_NAME, body.token, {
        httpOnly: true,
        sameSite: 'strict',
        path: '/',
        maxAge: COOKIE_MAX_AGE,
        secure: process.env.NODE_ENV === 'production',
      });
      return res;
    }
  } catch {
  }

  return upstream;
}
