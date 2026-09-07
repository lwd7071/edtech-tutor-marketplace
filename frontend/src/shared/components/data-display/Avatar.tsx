import React from 'react';
import { Avatar as AntdAvatar, Badge, AvatarProps as AntdAvatarProps } from 'antd';
import { CheckCircleFilled } from '@ant-design/icons';

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

export interface AvatarProps extends Omit<AntdAvatarProps, 'size'> {
  size?: AvatarSize;
  isVerified?: boolean;
}

const sizeMap: Record<AvatarSize, number> = {
  xs: 24,
  sm: 32,
  md: 40,
  lg: 64,
  xl: 96,
};

export const Avatar: React.FC<AvatarProps> = ({ size = 'md', isVerified, children, ...props }) => {
  const pixelSize = sizeMap[size];

  const avatarElement = (
    <AntdAvatar size={pixelSize} {...props}>
      {children || (typeof props.alt === 'string' ? props.alt.trim().split(/\s+/).slice(-2).map(part => part[0]).join('') : undefined)}
    </AntdAvatar>
  );

  if (isVerified) {
    return (
      <Badge
        count={
          <CheckCircleFilled
            data-testid="verified-badge"
            style={{ color: 'var(--color-info-600)', fontSize: Math.max(12, pixelSize * 0.25), background: '#fff', borderRadius: '50%' }}
          />
        }
        offset={[-pixelSize * 0.15, pixelSize * 0.85]}
      >
        {avatarElement}
      </Badge>
    );
  }

  return avatarElement;
};
