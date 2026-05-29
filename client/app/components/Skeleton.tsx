export function Skeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          className="animate-shimmer rounded-xl"
          style={{ height: 60 + Math.random() * 40, animationDelay: `${i * 0.08}s` }}
        />
      ))}
    </div>
  );
}

export function Spinner({ className = '' }: { className?: string }) {
  return <div className={`spinner ${className}`} />;
}
