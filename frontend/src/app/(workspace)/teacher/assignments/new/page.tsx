'use client';

import { vietnamLocalInputToIso } from '@/shared/lib/vietnamTime';
import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Form, Input, Select, Skeleton } from 'antd';
import { getLearners } from '@/features/teacher-dashboard/components/LearnerList';
import { learningApi } from '@/features/learning/api/learningApi';
import { learningKeys } from '@/features/learning/data/learningKeys';
import type { CreateAssignmentRequest } from '@/features/learning/types';
import styles from './assignment-create.module.css';

type AssignmentFormValues = {
  packageId: string;
  title: string;
  instructions: string;
  dueAt: string;
  status: 'DRAFT' | 'PUBLISHED';
};

export default function Page() {
  const query = useQuery({
    queryKey: learningKeys.assignmentLearners(),
    queryFn: () => getLearners(0, 100),
  });
  const [busy, setBusy] = useState(false);
  const [form] = Form.useForm<AssignmentFormValues>();
  const router = useRouter();
  const { message } = App.useApp();
  const status = Form.useWatch('status', form);

  const packages = query.data?.data.filter(row =>
    ['ACTIVE', 'COMPLETED'].includes(row.studentPackage.status),
  ) ?? [];

  async function submit(values: AssignmentFormValues) {
    const selected = query.data?.data.find(row => row.studentPackage.id === values.packageId);
    if (!selected) return;

    setBusy(true);
    try {
      const request: CreateAssignmentRequest = {
        studentId: selected.studentId,
        subjectId: selected.studentPackage.subjectId,
        title: values.title,
        assignmentType: 'FREEFORM',
        status: values.status,
        dueAt: vietnamLocalInputToIso(values.dueAt),
        contentBlocks: [{ type: 'TEXT', content: values.instructions }],
        version: 0,
      };
      await learningApi.createAssignment(request);
      message.success(values.status === 'DRAFT' ? 'Đã lưu nháp' : 'Đã giao bài');
      router.push('/teacher/assignments');
    } catch {
      message.error('Chưa giao được bài. Kiểm tra thông tin và quan hệ học viên.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className={styles.page}>
      <Link className={styles.backLink} href="/teacher/assignments">Danh sách bài tập</Link>

      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Không gian gia sư / Bài tập</p>
          <h1>Giao bài tập</h1>
          <p className={styles.intro}>Chọn người học, viết đề bài và đặt hạn nộp.</p>
        </div>
      </header>

      {query.isLoading ? (
        <section className={styles.loading} role="status" aria-label="Đang tải học viên và gói học">
          <div className={styles.loadingSection}><span /><Skeleton active paragraph={{ rows: 2 }} /></div>
          <div className={styles.loadingSection}><span /><Skeleton active paragraph={{ rows: 4 }} /></div>
        </section>
      ) : query.isError ? (
        <section className={styles.state}>
          <Alert
            className={styles.error}
            type="error"
            title="Chưa tải được danh sách học viên"
            description="Kiểm tra kết nối rồi thử tải lại để chọn đúng gói học."
            action={<Button className={styles.retry} onClick={() => query.refetch()}>Thử lại</Button>}
          />
        </section>
      ) : packages.length === 0 ? (
        <section className={styles.empty} aria-labelledby="assignment-create-empty-title">
          <p className={styles.stepLabel}>Người nhận</p>
          <div>
            <h2 id="assignment-create-empty-title">Chưa có học viên hoặc gói học phù hợp</h2>
            <p>Chỉ gói đang học hoặc đã hoàn thành mới có thể nhận bài tập.</p>
            <Link className={styles.emptyLink} href="/teacher/students">Danh sách học viên</Link>
          </div>
        </section>
      ) : (
        <Form<AssignmentFormValues>
          className={styles.form}
          form={form}
          layout="vertical"
          initialValues={{ status: 'DRAFT' }}
          onFinish={submit}
        >
          <section className={styles.formSection} aria-labelledby="assignment-recipient-title">
            <div className={styles.sectionIntro}>
              <span className={styles.stepNumber}>01</span>
              <div>
                <h2 id="assignment-recipient-title">Người nhận</h2>
                <p>Chọn học viên và gói học gắn với môn của bài tập.</p>
              </div>
            </div>
            <div className={styles.sectionFields}>
              <Form.Item
                className={styles.recipientField}
                name="packageId"
                label="Học viên và môn học"
                rules={[{ required: true, message: 'Vui lòng chọn học viên và môn học' }]}
              >
                <Select
                  showSearch
                  className={styles.recipientSelect}
                  optionFilterProp="label"
                  placeholder="Chọn học viên và gói học"
                  options={packages.map(row => ({
                    value: row.studentPackage.id,
                    label: `${row.studentName} · ${row.subjectName} · ${row.studentPackage.packageName}`,
                  }))}
                />
              </Form.Item>
            </div>
          </section>

          <section className={styles.formSection} aria-labelledby="assignment-content-title">
            <div className={styles.sectionIntro}>
              <span className={styles.stepNumber}>02</span>
              <div>
                <h2 id="assignment-content-title">Nội dung bài tập</h2>
                <p>Tiêu đề và hướng dẫn sẽ được gửi tới học viên đã chọn.</p>
              </div>
            </div>
            <div className={styles.sectionFields}>
              <Form.Item className={styles.titleField} name="title" label="Tên bài tập" rules={[{ required: true }]}>
                <Input className={styles.titleInput} autoComplete="off" placeholder="Ví dụ: Ôn tập phương trình bậc hai" />
              </Form.Item>
              <Form.Item className={styles.instructionsField} name="instructions" label="Đề bài và hướng dẫn" rules={[{ required: true }]}>
                <Input.TextArea className={styles.instructionsInput} rows={7} placeholder="Viết yêu cầu và hướng dẫn làm bài" />
              </Form.Item>
            </div>
          </section>

          <section className={styles.formSection} aria-labelledby="assignment-delivery-title">
            <div className={styles.sectionIntro}>
              <span className={styles.stepNumber}>03</span>
              <div>
                <h2 id="assignment-delivery-title">Hạn nộp và trạng thái</h2>
                <p>Thời gian nhập theo giờ Việt Nam. Chọn lưu nháp hoặc phát hành ngay.</p>
              </div>
            </div>
            <div className={styles.sectionFields}>
              <Form.Item className={styles.deadlineField} name="dueAt" label="Hạn nộp (giờ Việt Nam)" rules={[{ required: true }]}>
                <Input className={styles.deadlineInput} type="datetime-local" />
              </Form.Item>
              <Form.Item className={styles.statusField} name="status" label="Trạng thái">
                <Select className={styles.statusSelect} options={[
                  { value: 'DRAFT', label: 'Lưu nháp' },
                  { value: 'PUBLISHED', label: 'Phát hành ngay' },
                ]} />
              </Form.Item>
            </div>
          </section>

          <footer className={styles.actions}>
            <p>Bạn có thể mở bài tập sau khi lưu để tiếp tục chỉnh sửa.</p>
            <Button className={styles.submit} htmlType="submit" type="primary" loading={busy}>
              {status === 'PUBLISHED' ? 'Phát hành bài tập' : 'Lưu bản nháp'}
            </Button>
          </footer>
        </Form>
      )}
    </div>
  );
}
