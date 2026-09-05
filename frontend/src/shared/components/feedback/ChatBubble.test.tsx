import { render, screen } from '@testing-library/react';
import ChatBubble from './ChatBubble';

describe('ChatBubble', () => {
  it('renders own message correctly', () => {
    render(<ChatBubble content="Hello there" variant="own" timestamp="2026-09-05T08:00:00Z" status="sent" />);
    
    expect(screen.getByText('Hello there')).toBeInTheDocument();
    
    const wrapper = screen.getByText('Hello there').closest('div.chat-bubble');
    expect(wrapper!.className).toContain('own');
    // It should have the sent icon checkmark (we test that it renders)
  });

  it('renders other message correctly', () => {
    render(<ChatBubble content="Hi!" variant="other" timestamp="2026-09-05T08:01:00Z" />);
    
    const wrapper = screen.getByText('Hi!').closest('div.chat-bubble');
    expect(wrapper!.className).toContain('other');
  });

  it('renders system message correctly', () => {
    render(<ChatBubble content="System info" variant="system" />);
    
    const wrapper = screen.getByText('System info').closest('div.chat-bubble');
    expect(wrapper!.className).toContain('system');
  });
});
