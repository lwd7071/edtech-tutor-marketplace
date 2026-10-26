'use client';

import React, { useEffect, useState } from 'react';
import { Button, message } from 'antd';
import { TeacherDocument, teacherApi } from '@/shared/api/teacher';
import DocumentUploadBox from './DocumentUploadBox';
import DocumentList from './DocumentList';

export default function DocumentsPage() {
  const [documents, setDocuments] = useState<TeacherDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDocuments = async () => {
    try {
      const data = await teacherApi.getDocuments();
      setDocuments(data);
      setError(null);
    } catch (err) {
      setError('Không thể tải tài liệu.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  const handleUploadSuccess = (newDoc: TeacherDocument) => {
    setDocuments((prev) => [...prev, newDoc]);
    message.success('Tải lên tài liệu thành công!');
  };

  const handleDelete = async (id: string) => {
    try {
      await teacherApi.deleteDocument(id);
      setDocuments((prev) => prev.filter(doc => doc.id !== id));
      message.success('Đã xóa tài liệu.');
    } catch (err) {
      message.error('Không thể xóa tài liệu.');
    }
  };

  return (
    <main className="tm-teacher-documents">
      <header className="tm-teacher-documents__heading">
        <h1>Xác minh danh tính</h1>
        <p>Giấy tờ dùng để admin xác minh hồ sơ gia sư. Tệp này riêng tư và không hiển thị trên hồ sơ công khai.</p>
      </header>

      {error && <section className="tm-teacher-documents__error" role="alert" aria-labelledby="teacher-documents-error-title">
        <div><h2 id="teacher-documents-error-title">{error}</h2><p>Danh sách chưa được cập nhật. Tệp đã tải lên vẫn được giữ nguyên.</p></div>
        <Button onClick={async () => { setError(null); setLoading(true); await fetchDocuments(); }}>Thử tải lại</Button>
      </section>}

      <section className="tm-teacher-documents__list-section" aria-labelledby="teacher-documents-list-title">
        <div className="tm-teacher-documents__section-heading">
          <h2 id="teacher-documents-list-title">Tài liệu đã tải lên</h2>
          {!loading && !error && <span>{documents.length} tài liệu</span>}
        </div>
        {loading ? <p className="tm-teacher-documents__loading" role="status">Đang tải tài liệu...</p> : error ? null : <DocumentList documents={documents} onDelete={handleDelete} />}
      </section>

      <section className="tm-teacher-documents__upload-section" aria-labelledby="teacher-documents-upload-title">
        <div className="tm-teacher-documents__section-heading">
          <h2 id="teacher-documents-upload-title">Thêm tài liệu xác minh</h2>
          <p>Chỉ nhận PDF, PNG, JPG hoặc JPEG; tối đa 10 MB mỗi tệp. Chỉ admin được xem tệp này.</p>
        </div>
        <DocumentUploadBox onSuccess={handleUploadSuccess} />
      </section>
    </main>
  );
}
