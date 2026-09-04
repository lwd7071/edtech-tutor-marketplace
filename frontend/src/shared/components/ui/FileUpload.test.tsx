import { render, screen } from '@testing-library/react';
import FileUpload from './FileUpload';

describe('FileUpload', () => {
  it('renders correctly with default text', () => {
    render(<FileUpload onChange={() => {}} />);
    
    expect(screen.getByText('Nhấp hoặc kéo thả file vào đây')).toBeInTheDocument();
    expect(screen.getByText('Hỗ trợ tải lên một hoặc nhiều file.')).toBeInTheDocument();
  });
});
