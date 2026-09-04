import { render, screen } from '@testing-library/react';
import ResponsiveModal from './ResponsiveModal';

// Mock matchMedia for antd
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

describe('ResponsiveModal', () => {
  it('renders modal with correct title and children', () => {
    render(
      <ResponsiveModal open={true} title="Test Modal" onCancel={() => {}}>
        <div>Modal Content</div>
      </ResponsiveModal>
    );

    // Modal renders outside the root by default in antd, 
    // but React Testing Library can query it by role or text.
    expect(screen.getByText('Test Modal')).toBeInTheDocument();
    expect(screen.getByText('Modal Content')).toBeInTheDocument();
  });
});
