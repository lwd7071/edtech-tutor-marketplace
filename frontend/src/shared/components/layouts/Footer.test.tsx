import { render, screen } from '@testing-library/react';
import Footer from './Footer';

describe('Footer Component', () => {
  it('renders footer content', () => {
    render(<Footer />);
    expect(screen.getByText('Edtech Tutor Marketplace')).toBeInTheDocument();
    expect(screen.getByText(/Bản quyền thuộc về/)).toBeInTheDocument();
  });
});
