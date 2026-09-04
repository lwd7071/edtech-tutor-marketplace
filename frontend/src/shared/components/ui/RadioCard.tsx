'use client';

import React from 'react';
import { Radio } from 'antd';

interface RadioCardProps {
  value: string | number;
  checked?: boolean;
  onChange?: (value: string | number) => void;
  title: React.ReactNode;
  description?: React.ReactNode;
  disabled?: boolean;
  className?: string;
  style?: React.CSSProperties;
}

export default function RadioCard({
  value,
  checked = false,
  onChange,
  title,
  description,
  disabled = false,
  className = '',
  style,
}: RadioCardProps) {
  return (
    <div
      className={`radio-card ${checked ? 'checked' : ''} ${disabled ? 'disabled' : ''} ${className}`}
      onClick={() => {
        if (!disabled && onChange) {
          onChange(value);
        }
      }}
      style={{
        padding: 'var(--space-4)',
        border: '1px solid',
        borderColor: checked ? 'var(--color-primary-600)' : 'var(--color-border)',
        borderRadius: 'var(--radius-md)',
        backgroundColor: checked ? 'var(--color-primary-50)' : 'var(--color-surface)',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.6 : 1,
        transition: 'all var(--duration-base) var(--ease-standard)',
        display: 'flex',
        alignItems: 'flex-start',
        gap: 'var(--space-3)',
        ...style
      }}
    >
      <Radio value={value} checked={checked} disabled={disabled} style={{ marginTop: 2 }} />
      <div style={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
        <div style={{ fontWeight: 'var(--weight-semibold)', color: checked ? 'var(--color-primary-700)' : 'var(--color-text-primary)' }}>
          {title}
        </div>
        {description && (
          <div style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--text-body-sm)', marginTop: 'var(--space-1)' }}>
            {description}
          </div>
        )}
      </div>
    </div>
  );
}
