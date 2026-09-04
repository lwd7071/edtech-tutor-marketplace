import { render, screen, fireEvent } from '@testing-library/react';
import { useConfirmDialog } from './useConfirmDialog';

// Dummy component to test hook
function TestComponent() {
  const { confirm, confirmContext } = useConfirmDialog();

  return (
    <>
      {confirmContext}
      <button 
        onClick={() => confirm({
          title: 'Stale Data',
          variant: 'stale',
          onOk: jest.fn(),
        })}
      >
        Show Stale Confirm
      </button>
    </>
  );
}

describe('useConfirmDialog', () => {
  it('renders stale variant dialog', () => {
    render(<TestComponent />);
    
    // Open dialog
    fireEvent.click(screen.getByText('Show Stale Confirm'));
    
    // Check title and custom stale text
    expect(screen.getAllByText('Stale Data')[0]).toBeInTheDocument();
    expect(screen.getByText('Dữ liệu đã thay đổi ở nơi khác. Vui lòng tải lại trang để xem bản cập nhật mới nhất.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tải lại' })).toBeInTheDocument();
  });
});
