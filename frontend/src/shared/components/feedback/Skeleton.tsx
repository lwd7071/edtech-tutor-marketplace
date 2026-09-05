import React from 'react';
import { Skeleton as AntdSkeleton } from 'antd';
import type { SkeletonProps as AntdSkeletonProps } from 'antd/es/skeleton/Skeleton';

export type SkeletonVariant = 'text' | 'card' | 'table' | 'avatar';

export interface SkeletonProps extends Omit<AntdSkeletonProps, 'avatar'> {
  variant?: SkeletonVariant;
}

export const Skeleton: React.FC<SkeletonProps> = ({ variant = 'text', active = true, ...props }) => {
  if (variant === 'avatar') {
    return (
      <AntdSkeleton.Avatar active={active} size="large" shape="circle" {...props as any} />
    );
  }

  if (variant === 'card') {
    return (
      <AntdSkeleton active={active} avatar paragraph={{ rows: 4 }} {...props} />
    );
  }

  if (variant === 'table') {
    return (
      <AntdSkeleton active={active} title={false} paragraph={{ rows: 5, width: '100%' }} {...props} />
    );
  }

  // text variant
  return <AntdSkeleton active={active} paragraph={{ rows: 1 }} {...props} />;
};
