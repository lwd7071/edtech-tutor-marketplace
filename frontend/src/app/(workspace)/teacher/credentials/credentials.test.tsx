import { act, fireEvent, render, screen } from '@testing-library/react';
import TeacherCredentialsPage from './page';
import { teacherApi } from '@/shared/api/teacher';

jest.mock('@/shared/api/teacher', () => ({
  teacherApi: {
    getCredentials: jest.fn(),
    createCredential: jest.fn(),
    updateCredential: jest.fn(),
    deleteCredential: jest.fn(),
    getCredentialProof: jest.fn(),
  },
}));

jest.setTimeout(20_000);

beforeEach(() => {
  jest.clearAllMocks();
  (teacherApi.getCredentials as jest.Mock).mockResolvedValue([]);
  (teacherApi.createCredential as jest.Mock).mockResolvedValue({});
});

it('rejects files over 5MB before upload', async () => {
  render(<TeacherCredentialsPage />);
  expect(teacherApi.getCredentials).toHaveBeenCalledTimes(1);
  await act(async () => { await (teacherApi.getCredentials as jest.Mock).mock.results[0].value; });
  await screen.findByText(/Chưa có minh chứng nào/, {}, { timeout: 5000 });
  fireEvent.change(screen.getByLabelText('Tên chứng chỉ hoặc bằng cấp'), { target: { value: 'TOEIC 850' } });
  const file = new File(['%PDF-1.4'], 'score.pdf', { type: 'application/pdf' });
  Object.defineProperty(file, 'size', { value: 5 * 1024 * 1024 + 1 });
  fireEvent.change(screen.getByLabelText('Ảnh/PDF minh chứng'), { target: { files: [file] } });
  expect(screen.getByRole('alert')).toHaveTextContent('5MB');
  expect(teacherApi.createCredential).not.toHaveBeenCalled();
});

it('shows review status and submits a valid proof', async () => {
  (teacherApi.getCredentials as jest.Mock).mockResolvedValueOnce([
    { id: 'id-1', label: 'IELTS 7.5', status: 'REJECTED', rejectedReason: 'Ảnh mờ', version: 1 },
  ]).mockResolvedValue([]);
  render(<TeacherCredentialsPage />);
  expect(teacherApi.getCredentials).toHaveBeenCalledTimes(1);
  await act(async () => { await (teacherApi.getCredentials as jest.Mock).mock.results[0].value; });
  expect(await screen.findByText('Lý do từ chối: Ảnh mờ', {}, { timeout: 5000 })).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText('Tên chứng chỉ hoặc bằng cấp'), { target: { value: 'TOEIC 850' } });
  const file = new File(['%PDF-1.4'], 'score.pdf', { type: 'application/pdf' });
  fireEvent.change(screen.getByLabelText('Ảnh/PDF minh chứng'), { target: { files: [file] } });
  await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Gửi duyệt' })); });
  expect(teacherApi.createCredential).toHaveBeenCalledWith('TOEIC 850', file);
  expect(await screen.findByRole('status')).toHaveTextContent('Đã gửi minh chứng để duyệt.');
});
