'use client';

import { AuthGuard } from './AuthGuard';

export function withAuth<P extends object>(
  Component: React.ComponentType<P>,
  requireRoles?: (role: string) => boolean,
) {
  function Wrapped(props: P) {
    return (
      <AuthGuard requireRoles={requireRoles}>
        <Component {...props} />
      </AuthGuard>
    );
  }
  Wrapped.displayName = `withAuth(${Component.displayName || Component.name || 'Component'})`;
  return Wrapped;
}
