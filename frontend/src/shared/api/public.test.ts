import axiosClient from './axiosClient';
import { getPublicSubjects, getPublicTeachers, getTeacherDetail, getTeacherPackages, getTeacherAvailability, getTeacherReviews, getGlobalRanking } from './public';

jest.mock('./axiosClient', () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
}));

describe('Public API', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('getPublicSubjects', () => {
    it('should call GET /api/public/subjects with correct params', async () => {
      const mockResponse = { data: { success: true, message: null, data: [{ id: '1', name: 'Toán' }], errors: null, meta: { page: 0 } } };
      (axiosClient.get as jest.Mock).mockResolvedValue(mockResponse);

      const params = { keyword: 'Toán', page: 0, size: 10 };
      const result = await getPublicSubjects(params);

      expect(axiosClient.get).toHaveBeenCalledWith('/api/public/subjects', { params });
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('getPublicTeachers', () => {
    it('should call GET /api/public/teachers with correct params', async () => {
      const mockResponse = { data: { success: true, message: null, data: [{ id: '1', user: { fullName: 'Nguyen Van A' } }], errors: null, meta: { page: 0 } } };
      (axiosClient.get as jest.Mock).mockResolvedValue(mockResponse);

      const params = { keyword: 'Toán', sort: 'rating_desc', page: 0, size: 6 };
      const result = await getPublicTeachers(params);

      expect(axiosClient.get).toHaveBeenCalledWith('/api/public/teachers', { params });
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('getTeacherDetail', () => {
    it('should call GET /api/public/teachers/:id', async () => {
      const mockResponse = { data: { success: true, message: null, data: { id: '1', fullName: 'John' }, errors: null, meta: null } };
      (axiosClient.get as jest.Mock).mockResolvedValue(mockResponse);

      const result = await getTeacherDetail('1');

      expect(axiosClient.get).toHaveBeenCalledWith('/api/public/teachers/1');
      expect(result).toEqual(mockResponse.data.data);
    });
  });

  describe('getTeacherPackages', () => {
    it('should call GET /api/public/teachers/:id/packages', async () => {
      const mockResponse = { data: { success: true, message: null, data: [{ id: 'pkg1' }], errors: null, meta: { page: 0 } } };
      (axiosClient.get as jest.Mock).mockResolvedValue(mockResponse);

      const result = await getTeacherPackages('1', 0, 10);

      expect(axiosClient.get).toHaveBeenCalledWith('/api/public/teachers/1/packages', { params: { page: 0, size: 10 } });
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('getTeacherAvailability', () => {
    it('should call GET /api/public/teachers/:id/availability', async () => {
      const mockResponse = { data: { success: true, message: null, data: [{ dayOfWeek: 'MONDAY' }], errors: null, meta: null } };
      (axiosClient.get as jest.Mock).mockResolvedValue(mockResponse);

      const result = await getTeacherAvailability('1');

      expect(axiosClient.get).toHaveBeenCalledWith('/api/public/teachers/1/availability');
      expect(result).toEqual(mockResponse.data.data);
    });
  });

  describe('getTeacherReviews', () => {
    it('should call GET /api/public/teachers/:id/reviews', async () => {
      const mockResponse = { data: { success: true, message: null, data: [{ id: 'rev1' }], errors: null, meta: { page: 0 } } };
      (axiosClient.get as jest.Mock).mockResolvedValue(mockResponse);

      const result = await getTeacherReviews('1', 0, 5);

      expect(axiosClient.get).toHaveBeenCalledWith('/api/public/teachers/1/reviews', { params: { page: 0, size: 5 } });
      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('getGlobalRanking', () => {
    it('should call /api/public/teachers/ranking and return data', async () => {
      const mockRanking = [{ teacherId: 't1', fullName: 'John Doe', globalRank: 1 }];
      (axiosClient.get as jest.Mock).mockResolvedValue({ data: { success: true, message: null, data: mockRanking, errors: null, meta: {} } });

      const result = await getGlobalRanking('math-1', 0, 10);
      expect(result.data).toEqual(mockRanking);
      expect(axiosClient.get).toHaveBeenCalledWith('/api/public/teachers/ranking', { params: { subjectId: 'math-1', page: 0, size: 10 } });
    });
  });
});
