'use client';

import React, { useEffect, useState } from 'react';
import { Alert, Button, Empty, Form, Input, message, Select, Spin, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { TeacherSubjectProposal, teacherApi } from '@/shared/api/teacher';
import styles from './subject-proposals.module.css';

const EDUCATION_LABELS: Record<TeacherSubjectProposal['educationLevel'], string> = {
  ELEMENTARY: 'Tiểu học', MIDDLE_SCHOOL: 'THCS', HIGH_SCHOOL: 'THPT', UNIVERSITY: 'Đại học', OTHER: 'Khác',
};

const STATUS: Record<TeacherSubjectProposal['status'], { label: string; className: string }> = {
  PENDING: { label: 'Đang chờ', className: styles.pending },
  APPROVED: { label: 'Chấp thuận', className: styles.approved },
  REJECTED: { label: 'Từ chối', className: styles.rejected },
};

export default function SubjectProposalsPage() {
  const [proposals, setProposals] = useState<TeacherSubjectProposal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const fetchProposals = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await teacherApi.getSubjectProposals();
      setProposals(data);
    } catch (err) {
      setError('Không thể tải danh sách đề xuất.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProposals();
  }, []);

  const onFinish = async (values: { proposedName: string; educationLevel: TeacherSubjectProposal['educationLevel']; description?: string }) => {
    setSubmitting(true);
    try {
      const newProp = await teacherApi.createSubjectProposal(values);
      setProposals(prev => [newProp, ...prev]);
      message.success('Gửi đề xuất thành công!');
      form.resetFields();
    } catch (err) {
      message.error('Gửi đề xuất thất bại.');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<TeacherSubjectProposal> = [
    { title: 'Tên môn học', dataIndex: 'proposedName', key: 'proposedName', render: (name: string) => <><span className={styles.mobileLabel}>Tên môn học</span><strong className={styles.proposedName}>{name}</strong></> },
    { title: 'Cấp học', dataIndex: 'educationLevel', key: 'educationLevel', render: (level: TeacherSubjectProposal['educationLevel']) => <><span className={styles.mobileLabel}>Cấp học</span>{EDUCATION_LABELS[level] || level}</> },
    { title: 'Mô tả', dataIndex: 'description', key: 'description', render: (description?: string) => <><span className={styles.mobileLabel}>Mô tả</span>{description || '—'}</> },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (status: TeacherSubjectProposal['status']) => <><span className={styles.mobileLabel}>Trạng thái</span><Tag className={`${styles.status} ${STATUS[status]?.className ?? ''}`}>{STATUS[status]?.label ?? status}</Tag></> },
    { title: 'Phản hồi', dataIndex: 'reviewNote', key: 'reviewNote', render: (note?: string | null) => <><span className={styles.mobileLabel}>Phản hồi</span>{note || 'Chưa có phản hồi'}</> },
  ];

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <h1>Đề xuất môn học</h1>
        <p>Gửi môn học chưa có trong danh mục và theo dõi kết quả xét duyệt tại đây.</p>
      </header>

      {error && <Alert className={styles.error} type="error" showIcon message={error} action={<Button onClick={fetchProposals}>Thử lại</Button>} />}

      <div className={styles.workspace}>
        <section className={styles.formSection} aria-labelledby="proposal-form-title">
          <div className={styles.sectionHeading}>
            <h2 id="proposal-form-title">Gửi đề xuất mới</h2>
            <p>Thông tin sẽ được chuyển tới quản trị viên để xem xét.</p>
          </div>
          <Form form={form} layout="vertical" onFinish={onFinish} className={styles.form}>
            <Form.Item label="Tên môn học" name="proposedName" rules={[{ required: true, message: 'Vui lòng nhập tên môn học' }]}>
              <Input className={styles.field} placeholder="Tên môn học" />
            </Form.Item>
            <Form.Item label="Cấp học" name="educationLevel" rules={[{ required: true, message: 'Vui lòng chọn cấp học' }]}>
              <Select className={styles.field} placeholder="Chọn cấp học" options={[
                { value: 'ELEMENTARY', label: 'Tiểu học' },
                { value: 'MIDDLE_SCHOOL', label: 'THCS' },
                { value: 'HIGH_SCHOOL', label: 'THPT' },
                { value: 'UNIVERSITY', label: 'Đại học' },
                { value: 'OTHER', label: 'Khác' },
              ]} />
            </Form.Item>
            <Form.Item label="Mô tả" name="description">
              <Input.TextArea className={styles.field} rows={4} placeholder="Mô tả" />
            </Form.Item>
            <Button className={styles.submit} type="primary" htmlType="submit" loading={submitting}>Gửi đề xuất</Button>
          </Form>
        </section>

        <section className={styles.history} aria-labelledby="proposal-history-title">
          <div className={styles.sectionHeading}>
            <h2 id="proposal-history-title">Lịch sử đề xuất</h2>
            <p>{loading ? 'Đang tải các đề xuất đã gửi' : `${proposals.length} đề xuất`}</p>
          </div>
          {loading ? (
            <div className={styles.loading} role="status"><Spin /><span>Đang tải danh sách đề xuất...</span></div>
          ) : error ? (
            <div className={styles.retryHint}>Danh sách chưa tải được. Hãy thử lại để xem các đề xuất đã gửi.</div>
          ) : (
            <div className={styles.tableRegion}>
              <Table<TeacherSubjectProposal>
                className={styles.table}
                dataSource={proposals}
                columns={columns}
                rowKey="id"
                pagination={{ pageSize: 5 }}
                locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Bạn chưa gửi đề xuất môn học nào." /> }}
                scroll={{ x: 560 }}
              />
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
