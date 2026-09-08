'use client';

import type { ReactNode } from 'react';
import Link from 'next/link';
import { ArrowLeftOutlined, ArrowRightOutlined } from '@ant-design/icons';

type LinkProps = {
  href: string;
  children: ReactNode;
  className?: string;
};

const classes = (...values: Array<string | undefined>) => values.filter(Boolean).join(' ');

export function BackLink({ href, children, className }: LinkProps) {
  return (
    <Link className={classes('tm-back-link', className)} href={href}>
      <ArrowLeftOutlined aria-hidden />
      <span>{children}</span>
    </Link>
  );
}

export function ActionLink({
  href,
  children,
  variant = 'primary',
  className,
}: LinkProps & { variant?: 'primary' | 'secondary' }) {
  return (
    <Link className={classes('tm-action-link', `tm-action-link-${variant}`, className)} href={href}>
      <span>{children}</span>
      {variant === 'secondary' && <ArrowRightOutlined aria-hidden />}
    </Link>
  );
}

export function InlineActionLink({ href, children, className }: LinkProps) {
  return (
    <Link className={classes('tm-inline-link', className)} href={href}>
      <span>{children}</span>
      <ArrowRightOutlined className="tm-inline-link-arrow" aria-hidden />
    </Link>
  );
}

export function PageActions({ children, centered = false }: { children: ReactNode; centered?: boolean }) {
  return <div className={classes('tm-page-actions', centered ? 'tm-page-actions-centered' : undefined)}>{children}</div>;
}
