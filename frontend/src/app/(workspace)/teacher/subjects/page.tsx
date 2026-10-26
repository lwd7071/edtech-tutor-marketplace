'use client';

import { useEffect, useState } from 'react';
import { Button, Popconfirm, message } from 'antd';
import { TeacherSubject, teacherApi } from '@/shared/api/teacher';
import SubjectSelector from './SubjectSelector';
import styles from './subjects.module.css';

export default function SubjectsPage() {
  const [subjects, setSubjects] = useState<TeacherSubject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchSubjects = async () => {
    setLoading(true);
    try {
      const data = await teacherApi.getSubjects();
      setSubjects(data);
      setError(null);
    } catch {
      setError('Không thể tải danh sách môn học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchSubjects();
  }, []);

  const handleAddSubject = async (subjectId: string) => {
    if (subjects.some(subject => subject.subjectId === subjectId)) {
      message.warning('Bạn đã đăng ký môn học này rồi.');
      return;
    }
    try {
      const newSubject = await teacherApi.addSubject(subjectId);
      setSubjects(previous => [...previous, newSubject]);
      message.success('Đã thêm môn học thành công.');
    } catch {
      message.error('Thêm môn học thất bại.');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await teacherApi.deleteSubject(id);
      setSubjects(previous => previous.filter(subject => subject.id !== id));
      message.success('Đã xóa môn học.');
    } catch {
      message.error('Xóa môn học thất bại.');
    }
  };

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1>Môn đang dạy</h1>
        <p>Quản lý các môn học đã gắn với hồ sơ gia sư và chọn môn khác từ danh mục.</p>
      </header>

      {error && (
        <div className={styles.error} role="alert">
          <span>{error}</span>
          <Button type="default" onClick={() => void fetchSubjects()}>Thử tải lại</Button>
        </div>
      )}

      <div className={styles.workspace}>
        <section className={styles.subjects} aria-labelledby="teacher-subjects-title" aria-busy={loading}>
          <header className={styles.sectionHeading}>
            <div>
              <h2 id="teacher-subjects-title">Môn đã gắn</h2>
              <p>Tên môn và cấp học theo danh mục hiện tại.</p>
            </div>
            <span className={styles.count}>{subjects.length}<small> môn</small></span>
          </header>

          {loading ? (
            <div className={styles.loading} role="status" aria-label="Đang tải môn học">
              <span>Đang tải môn học…</span>
              {[0, 1, 2].map(row => <i key={row} aria-hidden="true" />)}
            </div>
          ) : subjects.length === 0 ? (
            <section className={styles.empty} aria-label="Chưa có môn học">
              <h3>Chưa có môn học nào</h3>
              <p>Chọn môn từ danh mục bên dưới để thêm vào hồ sơ.</p>
            </section>
          ) : (
            <ul className={styles.subjectList}>
              {subjects.map(subject => (
                <li className={styles.subjectRow} key={subject.id}>
                  <div className={styles.subjectDetails}>
                    <strong>{subject.name}</strong>
                    <span>{subject.category}</span>
                  </div>
                  <Popconfirm
                    title="Xóa môn học"
                    description="Bạn có chắc chắn muốn xóa môn này không?"
                    onConfirm={() => handleDelete(subject.id)}
                    okText="Đồng ý"
                    cancelText="Hủy"
                  >
                    <Button type="default" danger aria-label={`Xóa môn ${subject.name}`}>Xóa</Button>
                  </Popconfirm>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className={styles.catalog} aria-labelledby="subject-catalog-title">
          <header className={styles.sectionHeading}>
            <div>
              <h2 id="subject-catalog-title">Thêm môn từ danh mục</h2>
              <p>Tìm một môn công khai và gắn vào danh sách đang dạy.</p>
            </div>
          </header>
          <SubjectSelector onAdd={handleAddSubject} />
        </section>
      </div>
    </div>
  );
}
