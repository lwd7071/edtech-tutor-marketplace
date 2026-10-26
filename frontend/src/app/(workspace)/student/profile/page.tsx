'use client';

import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Avatar, Button, Form, Input, message, Skeleton, Switch } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/features/auth';
import { authApi, type ParentContactRequest } from '@/shared/api/auth';

export default function StudentProfilePage() {
  const { user } = useAuthStore();
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const parentEmail = Form.useWatch('parentEmail', form);

  const loadContact = useCallback(async () => {
    setLoading(true); setLoadError(false);
    try { const result = await authApi.getParentContact(); form.setFieldsValue(result.data); }
    catch { setLoadError(true); }
    finally { setLoading(false); }
  }, [form]);
  useEffect(() => { void loadContact(); }, [loadContact]);

  const handleFinish = async (values: ParentContactRequest) => {
    try {
      setSubmitting(true);
      const normalized = {
        parentFullName: values.parentFullName?.trim() || null,
        parentPhone: values.parentPhone?.trim() || null,
        parentEmail: values.parentEmail?.trim() || null,
        notifyParent: Boolean(values.parentEmail?.trim()) && Boolean(values.notifyParent),
      };
      const result = await authApi.updateParentContact(normalized);
      form.setFieldsValue(result.data);
      message.success('Cập nhật thông tin phụ huynh thành công!');
    } catch (error) {
      console.error('Lỗi khi cập nhật thông tin:', error);
      message.error('Không thể cập nhật thông tin phụ huynh. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!user) return null;

  return (
    <main className="tm-stack tm-student-profile" aria-labelledby="student-profile-title">
      <header className="tm-page-heading tm-student-profile__heading">
        <h1 id="student-profile-title">Hồ sơ cá nhân</h1>
        <p>Thông tin tài khoản và liên hệ phụ huynh của bạn.</p>
      </header>

      <section className="tm-student-profile__account" aria-labelledby="student-account-title">
        <div className="tm-student-profile__identity">
          <Avatar className="tm-student-profile__avatar" size={76} src={user.avatarUrl} icon={<UserOutlined />} alt={user.fullName} />
          <div className="tm-student-profile__identity-copy">
            <h2 id="student-account-title">{user.fullName}</h2>
            <p>{user.email}</p>
          </div>
        </div>
        <dl className="tm-student-profile__account-facts">
          <div><dt>Vai trò</dt><dd>Học viên</dd></div>
          <div><dt>Trạng thái</dt><dd>Đang hoạt động</dd></div>
        </dl>
      </section>

      <section className="tm-student-profile__contact" aria-labelledby="student-parent-contact-title">
        <header className="tm-student-profile__contact-heading">
          <h2 id="student-parent-contact-title">Thông tin liên hệ phụ huynh</h2>
          <p>Thông tin này không bắt buộc. Khi bật nhận email, hệ thống gửi các cập nhật học tập quan trọng tới địa chỉ phụ huynh.</p>
        </header>

        {loadError && <Alert className="tm-student-profile__error" type="error" showIcon title="Chưa tải được thông tin phụ huynh" action={<Button onClick={() => void loadContact()}>Thử lại</Button>} />}
        {loading ? <div className="tm-student-profile__loading" role="status" aria-label="Đang tải thông tin phụ huynh"><Skeleton active paragraph={{ rows: 4 }} /></div> : (
          <Form className="tm-student-profile__form" form={form} layout="vertical" onFinish={handleFinish} initialValues={{ notifyParent: false }}>
            <div className="tm-student-profile__fields">
              <Form.Item label="Họ và tên phụ huynh" name="parentFullName" rules={[{ min: 3, message: 'Tên quá ngắn' }]}>
                <Input placeholder="Nhập họ và tên" autoComplete="name" />
              </Form.Item>
              <Form.Item label="Số điện thoại phụ huynh" name="parentPhone" rules={[{ pattern: /^[0-9]{10,11}$/, message: 'Số điện thoại không hợp lệ' }]}>
                <Input placeholder="Nhập số điện thoại" inputMode="numeric" autoComplete="tel" />
              </Form.Item>
              <Form.Item className="tm-student-profile__email" label="Email phụ huynh" name="parentEmail" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}>
                <Input placeholder="Nhập địa chỉ email" autoComplete="email" />
              </Form.Item>
            </div>

            <div className="tm-student-profile__notify-control">
              <Form.Item className="tm-student-profile__notify" name="notifyParent" valuePropName="checked" noStyle>
                <Switch id="student-notify-parent" disabled={!parentEmail?.trim()} aria-labelledby="student-notify-parent-label" />
              </Form.Item>
              <label id="student-notify-parent-label" htmlFor="student-notify-parent">Gửi thông báo qua email phụ huynh</label>
            </div>

            <div className="tm-student-profile__actions">
              <Button type="primary" htmlType="submit" loading={submitting}>Lưu thông tin</Button>
            </div>
          </Form>
        )}
      </section>
    </main>
  );
}
