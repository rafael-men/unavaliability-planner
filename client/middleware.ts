import { NextRequest, NextResponse } from 'next/server';



const PUBLIC_PATHS = ['/login', '/register'];

function isPublic(pathname: string): boolean {
  return PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(p + '/'));
}

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const hasToken = Boolean(req.cookies.get('auth_token')?.value);

  
  if (hasToken && isPublic(pathname)) {
    return NextResponse.redirect(new URL('/', req.url));
  }

  
  if (!hasToken && !isPublic(pathname)) {
    const loginUrl = new URL('/login', req.url);
    loginUrl.searchParams.set('redirect', pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico|.*\\..*).*)'],
};
