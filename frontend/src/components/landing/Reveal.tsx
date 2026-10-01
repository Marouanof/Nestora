import type { CSSProperties, ElementType, ReactNode } from 'react';
import { useInView } from '@/hooks/useInView';

interface RevealProps {
  as?: ElementType;
  children: ReactNode;
  delay?: number;
  className?: string;
}

export default function Reveal({
  as = 'div',
  children,
  delay = 0,
  className = '',
}: RevealProps) {
  const { ref, inView } = useInView<HTMLElement>();
  const Tag = as;
  const style = { '--reveal-delay': `${delay}ms` } as CSSProperties;

  return (
    <Tag
      ref={ref}
      className={`landing-reveal ${inView ? 'in-view' : ''} ${className}`.trim()}
      style={style}
    >
      {children}
    </Tag>
  );
}
