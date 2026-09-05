import React from 'react';
import { render, screen } from '@testing-library/react';
import { Avatar } from './Avatar';

jest.mock('@ant-design/icons', () => ({
  CheckCircleFilled: (props: any) => <span data-testid="verified-badge" {...props} />
}));

describe('Avatar Component', () => {
  it('renders correctly with given initials', () => {
    render(<Avatar size="md">NN</Avatar>);
    expect(screen.getByText('NN')).toBeInTheDocument();
  });

  it('renders verified badge when isVerified is true', () => {
    render(<Avatar isVerified size="lg" data-testid="avatar-wrapper">NN</Avatar>);
    // We expect the verified icon to be present
    expect(screen.getByTestId('verified-badge')).toBeInTheDocument();
  });

  it('applies correct size mapping', () => {
    render(<Avatar size="xl" data-testid="avatar-xl">XL</Avatar>);
    // Avatar should have inline style or class for 96px
    // Ant Design's Avatar receives size prop which sets style
    const avatar = screen.getByText('XL').closest('.ant-avatar');
    expect(avatar).toHaveStyle({ width: '96px', height: '96px' });
  });
});
