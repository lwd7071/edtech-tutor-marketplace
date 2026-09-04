'use client';

import { useState, useEffect } from 'react';
import { Input } from 'antd';
import type { SearchProps } from 'antd/es/input';

interface DebouncedSearchProps extends Omit<SearchProps, 'onSearch'> {
  debounceMs?: number;
  onSearch: (value: string) => void;
}

export default function DebouncedSearch({ debounceMs = 500, onSearch, onChange, ...props }: DebouncedSearchProps) {
  const [value, setValue] = useState(props.defaultValue || props.value || '');

  useEffect(() => {
    const timer = setTimeout(() => {
      onSearch(value as string);
    }, debounceMs);

    return () => clearTimeout(timer);
  }, [value, debounceMs, onSearch]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setValue(e.target.value);
    if (onChange) {
      onChange(e);
    }
  };

  return (
    <Input.Search
      {...props}
      value={value}
      onChange={handleChange}
      onSearch={(val) => {
        setValue(val);
        onSearch(val);
      }}
    />
  );
}
