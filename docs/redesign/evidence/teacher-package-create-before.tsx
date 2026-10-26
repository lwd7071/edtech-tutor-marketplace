'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Form, Input, InputNumber, Select, Skeleton } from 'antd';
import { teacherApi, type CreatePackageRequest } from '@/shared/api/teacher';
import type { PricingPackageView } from '@/shared/api/public';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import { teacherDashboardKeys } from '../data/teacherDashboardKeys';

type PackageFormValues = Omit<CreatePackageRequest, 'version'>;

interface PackageFormProps {
  mode: 'create' | 'edit';
  packageId?: string;
  initialValues?: Partial<PricingPackageView>;
  onSave: () => void;
  onCancel: () => void;
}

export function PackageForm({ mode, packageId, initialValues, onSave, onCancel }: PackageFormProps) {
  const subjects = useQuery({ queryKey: teacherDashboardKeys.subjects(), queryFn: teacherApi.getSubjects });
  const pkg = useQuery({
    queryKey: teacherDashboardKeys.package(packageId ?? ''),
    queryFn: () => teacherApi.getPackage(packageId!),
    enabled: mode === 'edit' && !!packageId,
  });
  const [busy, setBusy] = useState(false);
  const { message } = App.useApp();

  if (subjects.isLoading || (mode === 'edit' && pkg.isLoading)) return <Skeleton active />;
  if (subjects.isError || pkg.isError) {
    return <Alert type="error" title="Chưa tải được dữ liệu gói" action={<Button onClick={() => { subjects.refetch(); if (packageId) pkg.refetch(); }}>Thử lại</Button>} />;
  }

  const save = async (values: PackageFormValues) => {
    setBusy(true);
    try {
      if (mode === 'edit' && packageId && pkg.data) {
        await teacherApi.updatePackage(packageId, { ...values, version: pkg.data.version });
      } else {
        await teacherApi.createPackage({ ...values, version: 0 });
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

  return <section className="tm-panel" style={{ maxWidth: 820 }}>
    <h1>{mode === 'create' ? 'Tạo gói học' : 'Chỉnh sửa gói học'}</h1>
    <Form<PackageFormValues>
      layout="vertical"
      initialValues={pkg.data || initialValues || { totalSessions: 10, sessionDurationMinutes: 60, durationDays: 60, status: 'DRAFT' }}
      onFinish={save}
    >
      <Form.Item name="name" label="Tên gói học" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="subjectId" label="Môn học" rules={[{ required: true }]}>
        <Select options={subjects.data?.map(subject => ({ value: subject.subjectId, label: subject.name }))} />
      </Form.Item>
      <Form.Item name="description" label="Nội dung và mục tiêu"><Input.TextArea rows={4} /></Form.Item>
      <div className="tm-package-grid">
        {([['totalSessions', 'Số buổi'], ['sessionDurationMinutes', 'Thời lượng mỗi buổi (phút)'], ['durationDays', 'Hạn sử dụng (ngày)'], ['priceVnd', 'Giá trọn gói (VNĐ)']] as const).map(([name, label]) =>
          <Form.Item key={name} name={name} label={label} rules={[{ required: true }]}>
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>)}
      </div>
      <Form.Item name="status" label="Trạng thái" rules={[{ required: true }]}>
        <Select options={[{ value: 'DRAFT', label: 'Bản nháp' }, { value: 'ACTIVE', label: 'Đang mở bán' }, { value: 'INACTIVE', label: 'Ngừng bán' }]} />
      </Form.Item>
      <div className="tm-inline">
        <Button htmlType="submit" type="primary" loading={busy}>Lưu gói học</Button>
        <Button onClick={onCancel} disabled={busy}>Quay lại</Button>
      </div>
    </Form>
  </section>;
}
