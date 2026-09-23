import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App } from 'antd';
import { StudentAssignmentDetail } from './StudentAssignmentDetail';
import { learningApi } from '../api/learningApi';

jest.mock('../api/learningApi', () => ({ learningApi: {
  getStudentAssignmentDetail: jest.fn(), submitAssignment: jest.fn(),
} }));
jest.mock('@/shared/components/ui/FileUpload', () => ({ __esModule: true, default: () => <div>Tệp đính kèm</div> }));
jest.mock('@/shared/components/data-display/FileViewer', () => ({ FileViewer: () => null }));

const assignment = {
  id: 'assignment-1', title: 'Bài tập Toán', status: 'PUBLISHED', dueAt: '2099-01-01T00:00:00Z',
  contentBlocks: [], assignmentAttachments: [], submissionAttachments: [], submission: null,
};

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<QueryClientProvider client={queryClient}><App><StudentAssignmentDetail assignmentId="assignment-1" /></App></QueryClientProvider>);
}

beforeEach(() => {
  jest.mocked(learningApi.getStudentAssignmentDetail).mockResolvedValue({ data: assignment } as never);
  jest.mocked(learningApi.submitAssignment).mockResolvedValue({ data: {} } as never);
});

afterEach(() => jest.clearAllMocks());

test('saves a draft without submitting it for grading', async () => {
  setup();
  await screen.findByRole('button', { name: 'Lưu bản nháp' });
  fireEvent.change(screen.getByRole('textbox', { name: 'Câu trả lời' }), { target: { value: 'Lời giải nháp' } });
  fireEvent.click(screen.getByRole('button', { name: 'Lưu bản nháp' }));
  await waitFor(() => expect(learningApi.submitAssignment).toHaveBeenCalledWith('assignment-1',
    expect.objectContaining({ status: 'DRAFT', version: 0, contentBlocks: [{ type: 'TEXT', content: 'Lời giải nháp' }] })));
});

test('submits the answer when the primary action is used', async () => {
  setup();
  await screen.findByRole('button', { name: 'Nộp bài' });
  fireEvent.change(screen.getByRole('textbox', { name: 'Câu trả lời' }), { target: { value: 'Lời giải cuối' } });
  fireEvent.click(screen.getByRole('button', { name: 'Nộp bài' }));
  await waitFor(() => expect(learningApi.submitAssignment).toHaveBeenCalledWith('assignment-1',
    expect.objectContaining({ status: 'SUBMITTED', contentBlocks: [{ type: 'TEXT', content: 'Lời giải cuối' }] })));
});
