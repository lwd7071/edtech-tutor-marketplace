import React from 'react';
import { Tooltip } from 'antd';

export type MoneyTextVariant = 'normal' | 'positive' | 'negative' | 'compact';

export interface MoneyTextProps extends React.HTMLAttributes<HTMLSpanElement> {
  amount: number;
  variant?: MoneyTextVariant;
}

const formatNormal = (val: number) => {
  return Math.abs(val).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
};

const formatCompact = (val: number) => {
  const absVal = Math.abs(val);
  if (absVal >= 1_000_000_000) {
    return (absVal / 1_000_000_000).toLocaleString('vi-VN', { maximumFractionDigits: 1 }) + ' tỷ';
  }
  if (absVal >= 1_000_000) {
    return (absVal / 1_000_000).toLocaleString('vi-VN', { maximumFractionDigits: 1 }) + 'tr';
  }
  if (absVal >= 1_000) {
    return (absVal / 1_000).toLocaleString('vi-VN', { maximumFractionDigits: 1 }) + 'k';
  }
  return formatNormal(absVal);
};

export const MoneyText: React.FC<MoneyTextProps> = ({ amount, variant = 'normal', style, ...props }) => {
  const isNegative = amount < 0 || variant === 'negative';
  
  let formattedAmount = formatNormal(amount);
  if (variant === 'compact') {
    formattedAmount = formatCompact(amount);
  }

  let prefix = '';
  let color = 'inherit';

  if (variant === 'positive') {
    prefix = '+ ';
    color = 'var(--color-success-600)';
  } else if (variant === 'negative' || (isNegative && variant !== 'normal' && variant !== 'compact')) {
    prefix = '\u2212 '; // U+2212
    color = 'var(--color-error-600)';
  } else if (variant === 'normal' && isNegative) {
    prefix = '\u2212 ';
  }

  const content = (
    <span
      className="tabular-nums"
      style={{
        whiteSpace: 'nowrap',
        textAlign: 'right',
        color,
        ...style,
      }}
      {...props}
    >
      {prefix}{formattedAmount} ₫
    </span>
  );

  if (variant === 'compact') {
    return (
      <Tooltip title={`${isNegative ? '\u2212 ' : ''}${formatNormal(amount)} ₫`}>
        {content}
      </Tooltip>
    );
  }

  return content;
};
