import { render, screen, fireEvent } from '@testing-library/react';
import RadioCard from './RadioCard';

describe('RadioCard', () => {
  it('renders checked state correctly', () => {
    const handleChange = jest.fn();
    render(<RadioCard value="opt1" checked={true} onChange={handleChange} title="Option 1" description="Desc 1" />);

    const wrapper = screen.getByText('Option 1').closest('.radio-card');
    // We expect the wrapper to have a specific active border/class when checked
    expect(wrapper!.className).toContain('checked');
  });

  it('calls onChange when clicked', () => {
    const handleChange = jest.fn();
    render(<RadioCard value="opt2" checked={false} onChange={handleChange} title="Option 2" />);

    const wrapper = screen.getByText('Option 2').closest('.radio-card');
    fireEvent.click(wrapper!);
    
    expect(handleChange).toHaveBeenCalledWith('opt2');
  });
});
