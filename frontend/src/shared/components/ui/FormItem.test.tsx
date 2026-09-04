import { render, screen } from '@testing-library/react';
import FormItem from './FormItem';
import { Form, Input } from 'antd';

describe('FormItem', () => {
  it('renders children correctly', () => {
    render(
      <Form>
        <FormItem label="Email" name="email">
          <Input placeholder="Enter email" />
        </FormItem>
      </Form>
    );
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter email')).toBeInTheDocument();
  });
});
