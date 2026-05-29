'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { ProgressSpinner } from 'primereact/progressspinner';
import { useAuth } from '../providers';

interface Props {
  children: React.ReactNode;
  requireRoles?: (role: string) => boolean;
}

export function AuthGuard({ children, requireRoles }: Props) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace('/login');
      return;
    }
    if (requireRoles && !requireRoles(user.role)) {
      router.replace('/unavailability');
    }
  }, [user, loading, router, requireRoles]);

  if (loading || !user || (requireRoles && !requireRoles(user.role))) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <ProgressSpinner strokeWidth="3" />
      </div>
    );
  }
  return <>{children}</>;
}
