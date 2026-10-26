'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Form, Input, InputNumber, Select, Skeleton } from 'antd';
import { teacherApi, type CreatePackageRequest } from '@/shared/api/teacher';
import type { PricingPackageView } from '@/shared/api/public';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import { teacherDashboardKeys } from '../data/teacherDashboardKeys';
import styles from './PackageForm.module.css';

type PackageFormValues = Omit<CreatePackageRequest, 'version'>;

interface PackageFormProps {
  mode: 'create' | 'edit';
  packageId?: string;
  initialValues?: Partial<PricingPackageView>;
  onSave: () => void;
  onCancel: () => void;
}

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Bản nháp', ACTIVE: 'Đang mở bán', INACTIVE: 'Ngừng bán',
};

const NUMBER_FIELDS = [
  ['totalSessions', 'Số buổi'],
  ['sessionDurationMinutes', 'Thời lượng mỗi buổi (phút)'],
  ['durationDays', 'Hạn sử dụng (ngày)'],
  ['priceVnd', 'Giá trọn gói (VNĐ)'],
] as const;

export function PackageForm({ mode, packageId, initialValues, onSave, onCancel }: PackageFormProps) {
  const subjects = useQuery({ queryKey: teacherDashboardKeys.subjects(), queryFn: teacherApi.getSubjects });
  const pkg = useQuery({
    queryKey: teacherDashboardKeys.package(packageId ?? ''),
    queryFn: () => teacherApi.getPackage(packageId!),
    enabled: mode === 'edit' && !!packageId,
  });
  const [busy, setBusy] = useState(false);
  const [form] = Form.useForm<PackageFormValues>();
  const values = Form.useWatch([], form) as Partial<PackageFormValues> | undefined;
  const { message } = App.useApp();
  const chosenSubject = subjects.data?.find(subject => subject.subjectId === values?.subjectId);

  if (subjects.isLoading || (mode === 'edit' && pkg.isLoading)) {
    return <main className={styles.page}>
      <header className={styles.header}><h1>Đang tải dữ liệu gói học</h1></header>
      <div className={styles.loading} role="status"><span>Đang tải dữ liệu gói học...</span><Skeleton active paragraph={{ rows: 5 }} /></div>
    </main>;
  }

  if (subjects.isError || pkg.isError) {
    return <main className={styles.page}>
      <header className={styles.header}><h1>{mode === 'create' ? 'Tạo gói học' : 'Chỉnh sửa gói học'}</h1></header>
      <Alert className={styles.error} type="error" showIcon message="Chưa tải được dữ liệu gói" action={<Button onClick={() => { subjects.refetch(); if (packageId) pkg.refetch(); }}>Thử lại</Button>} />
    </main>;
  }

  const save = async (submittedValues: PackageFormValues) => {
    setBusy(true);
    try {
      if (mode === 'edit' && packageId && pkg.data) {
        await teacherApi.updatePackage(packageId, { ...submittedValues, version: pkg.data.version });
      } else {
        await teacherApi.createPackage({ ...submittedValues, version: 0 });
      }
      message.success('Đã lưu gói học');
      onSave();
    } catch (error) {
      if (isConcurrentModification(error)) {
        message.error('Dữ liệu gói đã thay đổi, vui lòng tải lại.');
        await pkg.refetch();
      } else {
        message.error(parseApiError(error).message);
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div>
          <h1>{mode === 'create' ? 'Tạo gói học' : 'Chỉnh sửa gói học'}</h1>
          <p>{mode === 'create' ? 'Thiết lập môn học, số buổi và điều khoản của gói.' : 'Cập nhật thông tin và điều khoản của gói học.'}</p>
        </div>
      </header>

      {mode === 'edit' && pkg.data && (
        <div className={styles.savedStatus} aria-label="Trạng thái gói đã lưu">
          <span>Trạng thái hiện tại</span>
          <strong>{STATUS_LABELS[pkg.data.status] || pkg.data.status}</strong>
        </div>
      )}

      <Form<PackageFormValues>
        form={form}
        className={styles.form}
        layout="vertical"
        initialValues={pkg.data || initialValues || { totalSessions: 10, sessionDurationMinutes: 60, durationDays: 60, status: 'DRAFT' }}
        onFinish={save}
      >
        <section className={styles.fields} aria-labelledby="package-details-title">
          <div className={styles.sectionHeading}>
            <h2 id="package-details-title">Thông tin gói</h2>
            <p>Đặt tên dễ nhận biết và chọn môn bạn đang dạy.</p>
          </div>
          <Form.Item name="name" label="Tên gói học" rules={[{ required: true }]}>
            <Input className={styles.control} />
          </Form.Item>
          <Form.Item name="subjectId" label="Môn học" rules={[{ required: true }]}>
            <Select className={styles.control} options={subjects.data?.map(subject => ({ value: subject.subjectId, label: subject.name }))} />
          </Form.Item>
          <Form.Item name="description" label="Nội dung và mục tiêu">
            <Input.TextArea className={styles.descriptionField} rows={4} />
          </Form.Item>

          <div className={styles.sectionHeading}>
            <h2>Điều khoản học</h2>
            <p>Các giá trị hiển thị cho học viên khi xem gói.</p>
          </div>
          <div className={styles.numericFields}>
            {NUMBER_FIELDS.map(([name, label]) => (
              <Form.Item key={name} name={name} label={label} rules={[{ required: true }]}>
                <InputNumber className={styles.control} min={1} precision={0} style={{ width: '100%' }} />
              </Form.Item>
            ))}
          </div>
          <Form.Item name="status" label="Trạng thái" rules={[{ required: true }]}>
            <Select className={styles.control} options={[{ value: 'DRAFT', label: 'Bản nháp' }, { value: 'ACTIVE', label: 'Đang mở bán' }, { value: 'INACTIVE', label: 'Ngừng bán' }]} />
          </Form.Item>
        </section>

        <aside className={styles.summary} aria-labelledby="package-summary-title">
          <h2 id="package-summary-title">Tóm tắt gói học</h2>
          <strong className={styles.summaryName}>{values?.name || 'Tên gói học'}</strong>
          <span className={styles.summarySubject}>{chosenSubject?.name ?? 'Chưa chọn môn học'}</span>
          <dl>
            <div><dt>Số buổi</dt><dd>{values?.totalSessions ? `${values.totalSessions} buổi` : 'Chưa nhập'}</dd></div>
            <div><dt>Thời lượng mỗi buổi</dt><dd>{values?.sessionDurationMinutes ? `${values.sessionDurationMinutes} phút` : 'Chưa nhập'}</dd></div>
            <div><dt>Hạn sử dụng</dt><dd>{values?.durationDays ? `${values.durationDays} ngày` : 'Chưa nhập'}</dd></div>
            <div><dt>Trạng thái</dt><dd>{values?.status ? STATUS_LABELS[values.status] || values.status : 'Chưa chọn'}</dd></div>
          </dl>
          <div className={styles.summaryPrice}>
            <span>Giá trọn gói</span>
            <strong>{values?.priceVnd ? `${Number(values.priceVnd).toLocaleString('vi-VN')}đ` : 'Chưa nhập'}</strong>
          </div>
          <div className={styles.actions}>
            <Button className={styles.submit} type="primary" htmlType="submit" loading={busy}>Lưu gói học</Button>
            <Button className={styles.cancel} onClick={onCancel} disabled={busy}>Quay lại</Button>
          </div>
        </aside>
      </Form>
    </main>
  );
}
