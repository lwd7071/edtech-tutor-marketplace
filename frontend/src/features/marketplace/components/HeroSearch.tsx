'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Input, Button } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
// Simple standard form


export default function HeroSearch() {
  const [keyword, setKeyword] = useState('');
  const router = useRouter();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (keyword.trim()) {
      router.push(`/teachers?keyword=${encodeURIComponent(keyword.trim())}`);
    }
  };

  return (
    <form onSubmit={handleSearch} style={{ display: 'flex', gap: '8px', width: '100%', maxWidth: '600px', margin: '0 auto' }}>
      <Input
        size="large"
        prefix={<SearchOutlined style={{ color: 'var(--color-text-tertiary, #A8A29E)' }} />}
        placeholder="Tìm kiếm môn học, kỹ năng..."
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        style={{ flex: 1, borderRadius: 'var(--radius-md, 8px)' }}
      />
      <Button
        type="primary"
        size="large"
        htmlType="submit"
        style={{ borderRadius: 'var(--radius-md, 8px)' }}
      >
        Tìm gia sư
      </Button>
    </form>
  );
}
