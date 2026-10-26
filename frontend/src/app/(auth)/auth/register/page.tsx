'use client';

import React, { useEffect, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Alert, Button, Checkbox, Divider, Input } from 'antd';
import { GoogleOutlined } from '@ant-design/icons';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { BASE_API_URL } from '@/shared/backend';
import RadioCard from '@/shared/components/ui/RadioCard';

const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;

const registerSchema = z.object({
  fullName: z.string().min(1, 'Họ và tên không được để trống'),
  email: z.string().min(1, 'Email không được để trống').email('Email không đúng định dạng'),
  password: z.string()
    .min(8, 'Mật khẩu phải dài ít nhất 8 ký tự')
    .regex(passwordRegex, 'Mật khẩu phải bao gồm chữ cái, chữ số và ký tự đặc biệt'),
  role: z.enum(['STUDENT', 'TEACHER'] as const, { error: 'Vui lòng chọn vai trò' }),
  agreeTerms: z.boolean().refine((val) => val === true, { message: 'Vui lòng đồng ý với điều khoản sử dụng' }),
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const { control, handleSubmit, setValue, formState: { errors } } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { fullName: '', email: '', password: '', role: 'STUDENT', agreeTerms: false },
  });

  useEffect(() => {
    if (new URLSearchParams(window.location.search).get('role') === 'TEACHER') setValue('role', 'TEACHER');
  }, [setValue]);

  const onSubmit = async (values: RegisterFormValues) => {
    try {
      setLoading(true);
      setErrorMsg(null);
      const payload = {
        fullName: values.fullName,
        email: values.email,
        password: values.password,
        role: values.role,
      };
      const result = await authApi.register(payload);
      router.push(`/auth/verify-email?email=${encodeURIComponent(result.data.email)}`);
    } catch (error: any) {
      setErrorMsg(error.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="tm-register-page">
      <header className="tm-register-heading">
        <h1>Đăng ký tài khoản</h1>
        <p>Bắt đầu hành trình học tập cùng EdTech Tutor</p>
      </header>

      {errorMsg && <Alert className="tm-register-error" description={errorMsg} type="error" showIcon role="alert" />}

      <form className="tm-register-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <fieldset className="tm-register-role-group">
          <legend>Bạn là:</legend>
          <Controller
            name="role"
            control={control}
            render={({ field }) => (
              <div className="tm-register-role-options">
                <RadioCard
                  className="tm-register-role-option"
                  title="Học viên / Phụ huynh"
                  description="Tìm kiếm gia sư và tham gia các khóa học"
                  value="STUDENT"
                  checked={field.value === 'STUDENT'}
                  onChange={field.onChange}
                />
                <RadioCard
                  className="tm-register-role-option"
                  title="Gia sư"
                  description="Trở thành người giảng dạy và tạo thu nhập"
                  value="TEACHER"
                  checked={field.value === 'TEACHER'}
                  onChange={field.onChange}
                />
              </div>
            )}
          />
          {errors.role && <p className="tm-register-field-error">{errors.role.message}</p>}
        </fieldset>

        <div className="tm-register-field">
          <label htmlFor="register-name">Họ và tên</label>
          <Controller
            name="fullName"
            control={control}
            render={({ field }) => (
              <Input
                {...field}
                id="register-name"
                size="large"
                placeholder="Ví dụ: Nguyễn Văn A"
                autoComplete="name"
                status={errors.fullName ? 'error' : ''}
                aria-invalid={Boolean(errors.fullName)}
                aria-describedby={errors.fullName ? 'register-name-error' : undefined}
              />
            )}
          />
          {errors.fullName && <p className="tm-register-field-error" id="register-name-error">{errors.fullName.message}</p>}
        </div>

        <div className="tm-register-field">
          <label htmlFor="register-email">Email</label>
          <Controller
            name="email"
            control={control}
            render={({ field }) => (
              <Input
                {...field}
                id="register-email"
                size="large"
                placeholder="Nhập email của bạn"
                autoComplete="email"
                status={errors.email ? 'error' : ''}
                aria-invalid={Boolean(errors.email)}
                aria-describedby={errors.email ? 'register-email-error' : undefined}
              />
            )}
          />
          {errors.email && <p className="tm-register-field-error" id="register-email-error">{errors.email.message}</p>}
        </div>

        <div className="tm-register-field">
          <label htmlFor="register-password">Mật khẩu</label>
          <Controller
            name="password"
            control={control}
            render={({ field }) => (
              <Input.Password
                {...field}
                id="register-password"
                size="large"
                placeholder="Nhập mật khẩu"
                autoComplete="new-password"
                status={errors.password ? 'error' : ''}
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? 'register-password-error' : 'register-password-hint'}
              />
            )}
          />
          {errors.password
            ? <p className="tm-register-field-error" id="register-password-error">{errors.password.message}</p>
            : <p className="tm-register-password-hint" id="register-password-hint">Tối thiểu 8 ký tự, gồm chữ cái, chữ số và ký tự đặc biệt (@$!%*#?&).</p>}
        </div>

        <div className="tm-register-terms">
          <Controller
            name="agreeTerms"
            control={control}
            render={({ field }) => (
              <Checkbox checked={field.value} onChange={(event) => field.onChange(event.target.checked)}>
                Tôi đồng ý với <Link href="/terms">Điều khoản sử dụng</Link> và <Link href="/privacy">Chính sách bảo mật</Link>
              </Checkbox>
            )}
          />
          {errors.agreeTerms && <p className="tm-register-field-error">{errors.agreeTerms.message}</p>}
        </div>

        <Button className="tm-register-submit" type="primary" htmlType="submit" size="large" block loading={loading}>
          Đăng ký
        </Button>
      </form>

      <Divider className="tm-register-divider" plain>Hoặc</Divider>

      <Button
        className="tm-register-google"
        size="large"
        block
        icon={<GoogleOutlined />}
        href={`${BASE_API_URL}/oauth2/authorization/google`}
      >
        Đăng ký bằng Google
      </Button>

      <footer className="tm-register-login">
        <span>Đã có tài khoản?</span>
        <Link href="/auth/login">Đăng nhập</Link>
      </footer>
    </main>
  );
}
