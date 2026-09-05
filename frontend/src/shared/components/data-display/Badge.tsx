import React from 'react';
import { Badge as AntdBadge, BadgeProps as AntdBadgeProps } from 'antd';

export type BadgeVariant = 'dot' | 'count' | 'accent';

export interface BadgeProps extends AntdBadgeProps {
  variant?: BadgeVariant;
}

export const Badge: React.FC<BadgeProps> = ({ variant = 'count', ...props }) => {
  const isDot = variant === 'dot';
  const color = variant === 'accent' ? 'var(--color-primary-500)' : undefined;

  return (
    <AntdBadge
      dot={isDot}
      color={color}
      {...props}
    />
  );
};
