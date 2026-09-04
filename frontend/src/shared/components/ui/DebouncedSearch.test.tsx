import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import DebouncedSearch from './DebouncedSearch';

jest.useFakeTimers();

describe('DebouncedSearch', () => {
  it('calls onSearch with debounced value', async () => {
    const handleSearch = jest.fn();
    render(<DebouncedSearch onSearch={handleSearch} debounceMs={300} placeholder="Search..." />);

    const input = screen.getByPlaceholderText('Search...');
    
    // Type into the input
    fireEvent.change(input, { target: { value: 'test' } });
    
    // Should not be called immediately
    expect(handleSearch).not.toHaveBeenCalled();

    // Fast-forward time
    jest.advanceTimersByTime(300);

    // Should be called after debounce
    await waitFor(() => {
      expect(handleSearch).toHaveBeenCalledWith('test');
    });
  });
});
