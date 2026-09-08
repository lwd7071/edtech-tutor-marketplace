import axiosClient from './axiosClient';
import { teacherApi } from './teacher';

jest.mock('./axiosClient');

describe('teacherApi', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Availabilities API', () => {
    it('should get availabilities successfully', async () => {
      const mockData = [{ id: '1', dayOfWeek: 'MONDAY', startTime: '08:00:00', endTime: '10:00:00' }];
      (axiosClient.get as jest.Mock).mockResolvedValueOnce({ data: { data: mockData } });

      const result = await teacherApi.getAvailabilities();

      expect(axiosClient.get).toHaveBeenCalledWith('/api/teacher/availability');
      expect(result).toEqual(mockData);
    });

    it('should replace availabilities successfully', async () => {
      const mockPayload = { timezone: 'Asia/Ho_Chi_Minh', items: [{ dayOfWeek: 'MONDAY', startTime: '08:00:00', endTime: '10:00:00' }] };
      (axiosClient.put as jest.Mock).mockResolvedValueOnce({ data: undefined });

      await teacherApi.replaceAvailabilities(mockPayload);

      expect(axiosClient.put).toHaveBeenCalledWith('/api/teacher/availability', { availabilities: mockPayload.items });
    });
  });

  describe('Packages API', () => {
    it('should get packages successfully', async () => {
      const mockData = [{ id: '1', name: 'Basic' }];
      (axiosClient.get as jest.Mock).mockResolvedValueOnce({ data: { data: mockData } });

      const result = await teacherApi.getPackages();

      expect(axiosClient.get).toHaveBeenCalledWith('/api/teacher/packages', { params: { size: 100 } });
      expect(result).toEqual(mockData);
    });

    it('should create package successfully', async () => {
      const mockPayload = { name: 'Basic', priceVnd: 500000, subjectId: 'subj1', description: 'desc', sessionCount: 10, durationMonths: 1, trialEnabled: false };
      const mockData = { id: '1', ...mockPayload };
      (axiosClient.post as jest.Mock).mockResolvedValueOnce({ data: { data: mockData } });

      const result = await teacherApi.createPackage(mockPayload);

      expect(axiosClient.post).toHaveBeenCalledWith('/api/teacher/packages', mockPayload);
      expect(result).toEqual(mockData);
    });

    it('should update package successfully', async () => {
      const mockPayload = { name: 'Pro', priceVnd: 600000, description: 'new desc', sessionCount: 12, durationMonths: 2, trialEnabled: true };
      const mockData = { id: '1', ...mockPayload };
      (axiosClient.put as jest.Mock).mockResolvedValueOnce({ data: { data: mockData } });

      const result = await teacherApi.updatePackage('1', mockPayload);

      expect(axiosClient.put).toHaveBeenCalledWith('/api/teacher/packages/1', mockPayload);
      expect(result).toEqual(mockData);
    });

    it('should update package status successfully', async () => {
      (axiosClient.patch as jest.Mock).mockResolvedValueOnce({ data: undefined });

      await teacherApi.updatePackageStatus('1', 'INACTIVE');

      expect(axiosClient.patch).toHaveBeenCalledWith('/api/teacher/packages/1/status', { status: 'INACTIVE' });
    });
  });
});
