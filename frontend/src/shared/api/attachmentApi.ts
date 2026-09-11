import { axiosClient } from './axiosClient';
import { ApiResponse } from '@/shared/backend';

export interface AttachmentView {
  id: string;
  secureUrl: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
}

export const attachmentApi = {
  /**
   * Upload một file đính kèm
   */
  uploadAttachment: async (
    file: File,
    attachableType: 'ASSIGNMENT' | 'SUBMISSION' | 'MESSAGE'
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
