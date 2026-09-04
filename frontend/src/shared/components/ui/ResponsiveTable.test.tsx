import { render, screen } from '@testing-library/react';
import ResponsiveTable from './ResponsiveTable';

// Mock matchMedia for antd
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    // Simulate desktop by default
    matches: query.includes('max-width'),
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

describe('ResponsiveTable', () => {
  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Age', dataIndex: 'age', key: 'age' },
  ];
  
  const data = [
    { key: '1', name: 'John Doe', age: 30 },
  ];

  it('renders correctly', () => {
    // In actual unit test, we might struggle to reliably test the resize logic 
    // unless we mock useMediaQuery effectively. We'll test basic rendering here.
    render(<ResponsiveTable columns={columns} dataSource={data} rowKey="key" />);
    
    // Ant Design table should render headers
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Age')).toBeInTheDocument();
  });
});
