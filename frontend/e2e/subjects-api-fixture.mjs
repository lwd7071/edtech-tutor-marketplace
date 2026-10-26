import http from 'node:http';

const subjects = [
  ['subject-math', 'MATH', 'Toán học', 'HIGH_SCHOOL'],
  ['subject-english', 'ENGLISH', 'Tiếng Anh', 'HIGH_SCHOOL'],
  ['subject-physics', 'PHYSICS', 'Vật lý', 'HIGH_SCHOOL'],
  ['subject-chemistry', 'CHEMISTRY', 'Hóa học', 'HIGH_SCHOOL'],
  ['subject-literature', 'LITERATURE', 'Ngữ văn', 'HIGH_SCHOOL'],
  ['subject-biology', 'BIOLOGY', 'Sinh học', 'HIGH_SCHOOL'],
  ['subject-history', 'HISTORY', 'Lịch sử', 'HIGH_SCHOOL'],
  ['subject-geography', 'GEOGRAPHY', 'Địa lý', 'HIGH_SCHOOL'],
  ['subject-programming', 'PROGRAMMING', 'Lập trình', 'UNIVERSITY'],
  ['subject-economics', 'ECONOMICS', 'Kinh tế', 'UNIVERSITY'],
  ['subject-art', 'ART', 'Mỹ thuật', 'OTHER'],
].map(([id, code, name, educationLevel]) => ({
  id,
  code,
  name,
  slug: code.toLowerCase(),
  educationLevel,
  description: null,
}));

const teachers = [
  { id: 'teacher-an', fullName: 'Nguyễn An', avatarUrl: null, bioExcerpt: 'Cùng bạn xây nền tảng Toán vững chắc, theo đúng mục tiêu học tập.', yearsOfExperience: 8, verifiedBadge: true, supportsOnline: true, supportsOffline: true, subjects: [{ id: 'subject-math', name: 'Toán học' }, { id: 'subject-physics', name: 'Vật lý' }], startingPriceVnd: 400000, averageRating: 4.9, bayesianRating: 4.8, reviewCount: 24, globalRank: 2, provinceName: 'Hà Nội', wardName: 'Cầu Giấy' },
  { id: 'teacher-linh', fullName: 'Trần Minh Linh', avatarUrl: null, bioExcerpt: 'Tiếng Anh giao tiếp và luyện thi với lộ trình rõ ràng.', yearsOfExperience: 6, verifiedBadge: true, supportsOnline: true, supportsOffline: false, subjects: [{ id: 'subject-english', name: 'Tiếng Anh' }], startingPriceVnd: 550000, averageRating: 4.8, bayesianRating: 4.7, reviewCount: 18, globalRank: 5, provinceName: 'TP. Hồ Chí Minh', wardName: 'Quận 3' },
  { id: 'teacher-mai', fullName: 'Phạm Ngọc Mai', avatarUrl: null, bioExcerpt: 'Kinh nghiệm đồng hành cùng học sinh THCS và THPT.', yearsOfExperience: 11, verifiedBadge: false, supportsOnline: true, supportsOffline: true, subjects: [{ id: 'subject-math', name: 'Toán học' }, { id: 'subject-chemistry', name: 'Hóa học' }], startingPriceVnd: 350000, averageRating: 4.7, bayesianRating: 4.6, reviewCount: 31, globalRank: 8, provinceName: 'Hà Nội', wardName: 'Đống Đa' },
];
const paginationTeachers = Array.from({ length: 13 }, (_, index) => {
  const teacher = teachers[index % teachers.length];
  return { ...teacher, id: `teacher-pagination-${index + 1}`, fullName: `${teacher.fullName} · ${index + 1}` };
});
const detailTeacherId = '22222222-2222-4222-8222-222222222222';
const detailTeacher = {
  id: detailTeacherId,
  fullName: 'Nguyễn An',
  avatarUrl: null,
  bio: 'Gia sư Toán với lộ trình học rõ ràng.\n\nTập trung xây nền tảng và luyện tập theo mục tiêu của học viên.',
  yearsOfExperience: 5,
  languages: ['Tiếng Việt'],
  supportsOnline: true,
  supportsOffline: true,
  provinceName: 'Hà Nội',
  wardName: 'Cầu Giấy',
  introductionVideoUrl: null,
  subjects: ['Toán học'],
  averageRating: 4.8,
  reviewCount: 11,
  credentials: [{ id: 'credential-fixture-1', label: 'Bằng cử nhân Sư phạm' }],
};
const detailPackages = Array.from({ length: 7 }, (_, index) => ({
  id: `package-fixture-${index + 1}`,
  name: `Gói Toán ${index + 1}`,
  description: index === 0 ? 'Ôn tập theo mục tiêu và củng cố phần kiến thức còn yếu.' : undefined,
  priceVnd: 500000 + index * 100000,
  totalSessions: 4 + index,
  sessionDurationMinutes: 60,
  durationDays: 30 + index * 5,
  subjectId: 'subject-math',
  subjectName: 'Toán học',
  version: 0,
  status: 'ACTIVE',
}));
const detailAvailability = [
  { id: 'availability-fixture-1', dayOfWeek: 'MONDAY', startTime: '09:00:00', endTime: '12:00:00' },
  { id: 'availability-fixture-2', dayOfWeek: 'WEDNESDAY', startTime: '13:00:00', endTime: '17:00:00' },
  { id: 'availability-fixture-3', dayOfWeek: 'SATURDAY', startTime: '08:00:00', endTime: '11:00:00' },
];
const detailReviews = Array.from({ length: 11 }, (_, index) => ({
  id: `review-fixture-${index + 1}`,
  rating: index % 2 === 0 ? 5 : 4,
  comment: index === 0 ? 'Giải thích rõ ràng, bài học bám sát mục tiêu.' : 'Buổi học có cấu trúc và dễ theo dõi.',
  createdAt: `2026-09-${String(index + 1).padStart(2, '0')}T10:00:00Z`,
  reviewerName: `Học viên ${index + 1}`,
}));
const rankingTeachers = Array.from({ length: 13 }, (_, index) => ({
  teacherId: `${String(index + 1).padStart(8, '0')}-3333-4333-8333-${String(index + 1).padStart(12, '0')}`,
  fullName: `Gia sư ${index + 1}`,
  avatarUrl: null,
  bioExcerpt: `Kinh nghiệm hướng dẫn học viên môn ${index % 2 === 0 ? 'Toán học' : 'Tiếng Anh'}.`,
  globalRank: index + 1,
  bayesianRating: Number((4.9 - index * 0.07).toFixed(2)),
  completedSessionCount: 120 - index * 7,
  subjectId: index % 2 === 0 ? 'subject-math' : 'subject-english',
}));

const sendPage = (response, data, page, size, totalElements) => {
  const totalPages = Math.ceil(totalElements / size);
  return send(response, data, {
    page,
    size,
    totalElements,
    totalPages,
    hasNext: page + 1 < totalPages,
    hasPrevious: page > 0,
  });
};

const send = (response, data, meta = null) => {
  response.writeHead(200, {
    'content-type': 'application/json; charset=utf-8',
    'access-control-allow-origin': '*',
  });
  response.end(JSON.stringify({ success: true, message: null, data, errors: null, meta }));
};

http.createServer(async (request, response) => {
  if (request.method === 'OPTIONS') {
    response.writeHead(204, {
      'access-control-allow-origin': '*',
      'access-control-allow-methods': 'GET, OPTIONS',
      'access-control-allow-headers': 'Content-Type, Authorization',
    });
    return response.end();
  }
  const url = new URL(request.url || '/', 'http://127.0.0.1');
  if (url.pathname === '/api/public/subjects') {
    const delayMs = Number(process.env.SUBJECT_FIXTURE_DELAY_MS || 0);
    if (delayMs > 0) await new Promise(resolve => setTimeout(resolve, delayMs));
    const keyword = (url.searchParams.get('keyword') || '').toLocaleLowerCase('vi');
    const educationLevel = url.searchParams.get('educationLevel') || '';
    const filtered = subjects.filter(subject => subject.name.toLocaleLowerCase('vi').includes(keyword)
      && (!educationLevel || subject.educationLevel === educationLevel));
    const page = Number(url.searchParams.get('page') || 0);
    const size = Number(url.searchParams.get('size') || 11);
    const data = filtered.slice(page * size, (page + 1) * size);
    const totalPages = Math.ceil(filtered.length / size);
    return send(response, data, {
      page,
      size,
      totalElements: filtered.length,
      totalPages,
      hasNext: page + 1 < totalPages,
      hasPrevious: page > 0,
    });
  }
  if (url.pathname.startsWith('/api/public/subjects/')) {
    const id = decodeURIComponent(url.pathname.split('/').at(-1) || '');
    const subject = subjects.find(candidate => candidate.id === id);
    if (subject) return send(response, subject);
    response.writeHead(404, { 'content-type': 'application/json; charset=utf-8' });
    return response.end(JSON.stringify({ success: false, message: null, data: null, errors: [{ code: 'SUBJECT_NOT_FOUND', message: 'Subject not found' }], meta: null }));
  }
  if (url.pathname === `/api/public/teachers/${detailTeacherId}`) return send(response, detailTeacher);
  if (url.pathname === `/api/public/teachers/${detailTeacherId}/packages`) {
    const page = Number(url.searchParams.get('page') || 0);
    const size = Number(url.searchParams.get('size') || 6);
    return sendPage(response, detailPackages.slice(page * size, (page + 1) * size), page, size, detailPackages.length);
  }
  if (url.pathname === `/api/public/teachers/${detailTeacherId}/availability`) return send(response, detailAvailability);
  if (url.pathname === `/api/public/teachers/${detailTeacherId}/reviews`) {
    const page = Number(url.searchParams.get('page') || 0);
    const size = Number(url.searchParams.get('size') || 10);
    return sendPage(response, detailReviews.slice(page * size, (page + 1) * size), page, size, detailReviews.length);
  }
  if (url.pathname === '/api/public/teachers/ranking') {
    const subjectId = url.searchParams.get('subjectId');
    if (subjectId === 'fixture-slow') await new Promise(resolve => setTimeout(resolve, 1_200));
    if (subjectId === 'fixture-error') {
      response.writeHead(503, {
        'content-type': 'application/json; charset=utf-8',
        'access-control-allow-origin': '*',
      });
      return response.end(JSON.stringify({ success: false, message: null, data: null, errors: [{ code: 'SERVICE_UNAVAILABLE', message: 'Fixture error' }], meta: null }));
    }
    const filtered = rankingTeachers.filter(teacher => !subjectId || teacher.subjectId === subjectId);
    const page = Number(url.searchParams.get('page') || 0);
    const size = Number(url.searchParams.get('size') || 50);
    const data = filtered.slice(page * size, (page + 1) * size).map(({ subjectId: _subjectId, ...teacher }) => teacher);
    return sendPage(response, data, page, size, filtered.length);
  }
  if (url.pathname === '/api/public/teachers') {
    const keyword = (url.searchParams.get('keyword') || '').toLocaleLowerCase('vi');
    const subjectId = url.searchParams.get('subjectId');
    const isPaginationFixture = keyword === 'pagination-fixture';
    const source = isPaginationFixture ? paginationTeachers : teachers;
    const filtered = source.filter(teacher => (isPaginationFixture || !keyword || `${teacher.fullName} ${teacher.bioExcerpt}`.toLocaleLowerCase('vi').includes(keyword))
      && (!subjectId || teacher.subjects.some(subject => subject.id === subjectId)));
    const page = Number(url.searchParams.get('page') || 0);
    const size = Number(url.searchParams.get('size') || 12);
    const data = filtered.slice(page * size, (page + 1) * size);
    const totalPages = Math.ceil(filtered.length / size);
    return send(response, data, { page, size, totalElements: filtered.length, totalPages, hasNext: page + 1 < totalPages, hasPrevious: page > 0 });
  }
  response.writeHead(404);
  response.end();
}).listen(Number(process.env.PORT || 3181), '127.0.0.1');
