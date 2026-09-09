import { axiosClient } from './axiosClient';
import { ApiResponse } from '@/shared/backend';

export interface AttachmentView {
  id: string;
  uploaderId: string;
  attachableType: string;
  attachableId: string | null;
  fileUrl: string;
  originalName: string;
  mimeType: string;
  fileSize: number;
  uploadedAt: string;
}

export const attachmentApi = {
  /**
   * Upload một file đính kèm
   */
  uploadAttachment: async (
    file: File,
    attachableType: 'ASSIGNMENT' | 'SUBMISSION' | 'SESSION_REPORT'
  ): Promise<ApiResponse<AttachmentView>> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('attachableType', attachableType);

    const response = await axiosClient.post<ApiResponse<AttachmentView>>(
      '/api/attachments',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },
};
