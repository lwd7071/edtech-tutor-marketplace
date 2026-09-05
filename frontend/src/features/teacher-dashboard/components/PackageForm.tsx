'use client';

import React, { useEffect, useState } from 'react';
import { Form, Input, InputNumber, Select, Switch, Button, message, Skeleton, Space } from 'antd';
import { teacherApi } from '@/shared/api/teacher';

const { TextArea } = Input;
const { Option } = Select;

interface PackageFormProps {
  mode: 'create' | 'edit';
  packageId?: string;
  initialValues?: any;
  onSave: () => void;
  onCancel: () => void;
}

export const PackageForm: React.FC<PackageFormProps> = ({ mode, packageId, initialValues, onSave, onCancel }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(mode === 'edit');
  const [submitting, setSubmitting] = useState(false);
  const [subjects, setSubjects] = useState<any[]>([]);
  const [initialData, setInitialData] = useState<any>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const subs = await teacherApi.getSubjects();
        setSubjects(subs);
        
        if (mode === 'edit' && packageId) {
          const pkgs = await teacherApi.getPackages();
          const pkg = pkgs.find(p => p.id === packageId);
          if (pkg) {
            setInitialData(pkg);
          } else {
            message.error('Không tìm thấy gói học');
            onCancel();
          }
        }
      } catch (error) {
        message.error('Lỗi tải dữ liệu');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [mode, packageId, onCancel]);

  const onFinish = async (values: any) => {
    setSubmitting(true);
    try {
      if (mode === 'create') {
        await teacherApi.createPackage(values);
        message.success('Tạo gói học thành công');
      } else {
        await teacherApi.updatePackage(packageId!, values);
        message.success('Cập nhật gói học thành công');
      }
      onSave();
    } catch (error) {
      message.error(mode === 'create' ? 'Tạo gói học thất bại' : 'Cập nhật thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div data-testid="loading-skeleton"><Skeleton active /></div>;
  }

  return (
    <div className="package-form bg-surface p-6 rounded-xl border border-border shadow-sm max-w-3xl">
      <h2 className="text-xl font-bold mb-6 text-text-primary">
        {mode === 'create' ? 'Tạo Gói Học Mới' : 'Sửa Gói Học'}
      </h2>
      
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={initialData || initialValues || { trialEnabled: false }}
        requiredMark={false}
      >
        <Form.Item
          name="name"
          label="Tên gói học"
          rules={[{ required: true, message: 'Vui lòng nhập tên gói học' }]}
        >
          <Input placeholder="Ví dụ: Toán 12 Cấp Tốc" />
        </Form.Item>

        <Form.Item
          name="subjectId"
          label="Môn học"
          rules={[{ required: true, message: 'Vui lòng chọn môn học' }]}
        >
          <Select placeholder="Chọn môn học bạn giảng dạy">
            {subjects.map(sub => (
              <Option key={sub.id} value={sub.id}>{sub.name}</Option>
            ))}
          </Select>
        </Form.Item>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Form.Item
            name="priceVnd"
            label="Giá tiền (VNĐ)"
            rules={[{ required: true, message: 'Vui lòng nhập giá' }]}
          >
            <InputNumber 
              className="w-full" 
              formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              disabled={mode === 'edit'} 
            />
          </Form.Item>

          <Form.Item
            name="sessionCount"
            label="Số buổi"
            rules={[{ required: true, message: 'Vui lòng nhập số buổi' }]}
          >
            <InputNumber className="w-full" min={1} disabled={mode === 'edit'} />
          </Form.Item>
        </div>

        <Form.Item
          name="durationMonths"
          label="Thời hạn (tháng)"
          rules={[{ required: true, message: 'Vui lòng nhập thời hạn' }]}
        >
          <InputNumber className="w-full" min={1} max={12} />
        </Form.Item>

        <Form.Item
          name="description"
          label="Mô tả chi tiết"
          rules={[{ required: true, message: 'Vui lòng nhập mô tả' }]}
        >
          <TextArea rows={4} placeholder="Mô tả nội dung học, đối tượng phù hợp..." />
        </Form.Item>

        <Form.Item
          name="trialEnabled"
          valuePropName="checked"
        >
          <Switch checkedChildren="Cho phép học thử" unCheckedChildren="Không cho học thử" />
        </Form.Item>

        <Form.Item className="mb-0 mt-6 flex justify-end">
          <Space>
            <Button onClick={onCancel} disabled={submitting}>Hủy</Button>
            <Button type="primary" htmlType="submit" loading={submitting}>
              {mode === 'create' ? 'Tạo gói học' : 'Lưu thay đổi'}
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </div>
  );
};
