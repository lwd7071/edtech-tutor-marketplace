import React from 'react';
import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import DocumentsPage from './page';
import { teacherApi } from '@/shared/api/teacher';

// Mock dependencies
jest.mock('@/shared/api/teacher', () => ({
  teacherApi: {
    getDocuments: jest.fn(),
    uploadDocument: jest.fn(),
    deleteDocument: jest.fn(),
  },
}));

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
  }),
}));

const mockDocuments = [
  {
    id: 'doc-1',
    name: 'Degree.pdf',
    url: 'http://example.com/degree.pdf',
    type: 'application/pdf',
    uploadedAt: '2026-09-01T10:00:00Z',
    status: 'VERIFIED',
  },
  {
    id: 'doc-2',
    name: 'Certificate.png',
    url: 'http://example.com/cert.png',
    type: 'image/png',
    uploadedAt: '2026-09-02T10:00:00Z',
    status: 'PENDING',
  },
];

describe('Teacher Documents Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (teacherApi.getDocuments as jest.Mock).mockResolvedValue(mockDocuments);
  });

  it('renders document list with fetched data', async () => {
    await act(async () => {
      render(<DocumentsPage />);
    });

    expect(screen.queryByText('Đang tải tài liệu...')).not.toBeInTheDocument();

    expect(screen.getByText('Degree.pdf')).toBeInTheDocument();
    expect(screen.getByText('Certificate.png')).toBeInTheDocument();
  });

  it('allows uploading a new document', async () => {
    const newDoc = {
      id: 'doc-3',
      name: 'IELTS.pdf',
      url: 'http://example.com/ielts.pdf',
      type: 'application/pdf',
      uploadedAt: '2026-09-05T10:00:00Z',
      status: 'PENDING',
    };
    (teacherApi.uploadDocument as jest.Mock).mockResolvedValue(newDoc);

    await act(async () => {
      render(<DocumentsPage />);
    });

    // We can't easily mock antd's Upload component interactions in jsdom without specifics,
    // so we assume the Upload component passes the file to the customRequest.
    // Instead of a deep interaction test, we check if the API is called when we simulate a file input change if accessible.
    // However, antd Upload hides the actual input. Let's find the input by type="file".
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    expect(fileInput).toBeInTheDocument();

    const file = new File(['dummy content'], 'IELTS.pdf', { type: 'application/pdf' });
    
    await act(async () => {
      fireEvent.change(fileInput, { target: { files: [file] } });
    });

    await waitFor(() => {
      expect(teacherApi.uploadDocument).toHaveBeenCalled();
    });

    expect(screen.getAllByText('IELTS.pdf').length).toBeGreaterThan(0);
  });

  it('allows deleting a document', async () => {
    (teacherApi.deleteDocument as jest.Mock).mockResolvedValue({});

    await act(async () => {
      render(<DocumentsPage />);
    });

    // Find delete buttons. Assuming there is a button with an aria-label or title for deleting.
    const deleteButtons = screen.getAllByRole('button', { name: /xóa/i });
    expect(deleteButtons.length).toBeGreaterThan(0);

    await act(async () => {
      fireEvent.click(deleteButtons[0]);
    });

    // Antd Popconfirm requires clicking OK
    const okButton = await screen.findByRole('button', { name: /đồng ý/i });
    
    await act(async () => {
      fireEvent.click(okButton);
    });

    await waitFor(() => {
      expect(teacherApi.deleteDocument).toHaveBeenCalledWith('doc-1');
    });

    // The document should disappear
    expect(screen.queryByText('Degree.pdf')).not.toBeInTheDocument();
  });
});
