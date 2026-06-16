import { ReactNode } from 'react';

interface CardProps {
  children: ReactNode;
  className?: string;
  borderColor?: string;
}

export function Card({ children, className = '', borderColor }: CardProps) {
  const borderStyle = borderColor ? { borderLeftColor: borderColor, borderLeftWidth: 3 } : undefined;
  return (
    <div
      className={`bg-[var(--card)] border border-[var(--border)] rounded-2xl p-5 backdrop-blur-md shadow-[0_8px_32px_rgba(0,0,0,0.25)] transition-all ${className}`}
      style={borderStyle}
    >
      {children}
    </div>
  );
}
