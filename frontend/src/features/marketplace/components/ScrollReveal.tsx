'use client';

import { useEffect, useRef, type HTMLAttributes } from 'react';

type ScrollRevealProps = HTMLAttributes<HTMLElement>;

export default function ScrollReveal({ children, className = '', ...props }: ScrollRevealProps) {
  const sectionRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const section = sectionRef.current;
    if (!section || !('IntersectionObserver' in window) || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        section.classList.add('is-visible');
        observer.disconnect();
      }
    }, { threshold: 0.12, rootMargin: '0px 0px -7% 0px' });

    section.classList.add('tm-reveal-ready');
    observer.observe(section);
    return () => observer.disconnect();
  }, []);

  return <section ref={sectionRef} className={`tm-reveal ${className}`.trim()} {...props}>{children}</section>;
}
