import React from 'react';
import { Rate, RateProps } from 'antd';

export interface RatingStarsProps extends RateProps {
  readonly?: boolean;
}

export const RatingStars: React.FC<RatingStarsProps> = ({ readonly = false, ...props }) => {
  return (
    <Rate
      disabled={readonly}
      allowHalf
      style={{ color: 'var(--color-warning-600)' }}
      {...props}
    />
  );
};
