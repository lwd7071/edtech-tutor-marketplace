'use client';

import React from 'react';
import { Form } from 'antd';
import type { FormItemProps } from 'antd/es/form';

export default function FormItem(props: FormItemProps) {
  return (
    <Form.Item
      {...props}
      style={{
        marginBottom: 'var(--space-6)',
        ...props.style,
      }}
    >
      {props.children}
    </Form.Item>
  );
}
