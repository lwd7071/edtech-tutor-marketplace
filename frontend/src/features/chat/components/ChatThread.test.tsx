import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { ChatThread } from './ChatThread';

beforeAll(() => {
  Element.prototype.scrollTo = jest.fn();
});

describe('ChatThread composer', () => {
  const props = {
    conversationId: 'conversation-1', participantName: 'Gia sư', messages: [],
    onSendMessage: jest.fn(), onSendAttachment: jest.fn(async () => true), onRetryMessage: jest.fn(),
  };

  beforeEach(() => { jest.clearAllMocks(); });

  it('labels the composer and uploads a selected file once when sending', async () => {
    render(<ChatThread {...props} />);
    const file = new File(['lesson'], 'lesson.pdf', { type: 'application/pdf' });
    fireEvent.change(screen.getByLabelText('Chọn tệp đính kèm'), { target: { files: [file] } });
    fireEvent.change(screen.getByRole('textbox', { name: 'Nội dung tin nhắn' }), { target: { value: 'Tài liệu' } });
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Gửi tệp đính kèm' })); });
    await waitFor(() => expect(props.onSendAttachment).toHaveBeenCalledWith(file, 'Tài liệu'));
    expect(props.onSendMessage).not.toHaveBeenCalled();
  });

  it('retains the selected file and offers retry after upload failure', async () => {
    props.onSendAttachment.mockResolvedValueOnce(false);
    render(<ChatThread {...props} />);
    const file = new File(['lesson'], 'lesson.pdf', { type: 'application/pdf' });
    fireEvent.change(screen.getByLabelText('Chọn tệp đính kèm'), { target: { files: [file] } });
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Gửi tệp đính kèm' })); });
    expect(await screen.findByText('Chưa tải được tệp. Bạn có thể thử gửi lại.')).toBeInTheDocument();
    expect(screen.getByText('lesson.pdf')).toBeInTheDocument();
  });
});
