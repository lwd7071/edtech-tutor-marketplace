import { render, screen } from '@testing-library/react';
import Footer from './Footer';

describe('Footer Component', () => {
  it('renders footer content', () => {
    render(<Footer />);
    expect(screen.getByRole('link', { name: /tutor match/i })).toBeInTheDocument();
    expect(screen.getByText('Tìm đúng gia sư. Học theo nhịp của bạn.')).toBeInTheDocument();
  });
});
