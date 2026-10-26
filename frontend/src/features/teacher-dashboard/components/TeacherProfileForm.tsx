'use client';

import {useState} from 'react';
import {useQuery, useQueryClient} from '@tanstack/react-query';
import {App, Button, Form, Input, InputNumber, Select, Skeleton, Switch} from 'antd';
import {teacherApi, type TeacherProfile} from '@/shared/api/teacher';
import {getPublicProvinces, getPublicWards} from '@/shared/api/public';
import {teacherDashboardKeys} from '../data/teacherDashboardKeys';

export function TeacherProfileForm() {
  const {message} = App.useApp();
  const client = useQueryClient();
  const [busy, setBusy] = useState(false);
  const [residenceBusy, setResidenceBusy] = useState(false);
  const query = useQuery({queryKey: teacherDashboardKeys.profile(), queryFn: teacherApi.getProfile});
  const provinces = useQuery({queryKey: ['public-locations', 'provinces'], queryFn: getPublicProvinces, staleTime: 300_000});
  const [residenceForm] = Form.useForm();
  const [selectedProvince, setSelectedProvince] = useState<string>();

  const save = async (values: TeacherProfile) => {
    setBusy(true);
    try { await teacherApi.updateProfile(values); await client.invalidateQueries({queryKey: teacherDashboardKeys.profile()}); message.success('Đã lưu hồ sơ.'); }
    catch { message.error('Chưa lưu được hồ sơ. Kiểm tra thông tin và thử lại.'); }
    finally { setBusy(false); }
  };

  const profile = query.data;
  const provinceCode = selectedProvince || profile?.provinceCode;
  const wards = useQuery({queryKey: ['public-locations', 'wards', provinceCode], queryFn: () => getPublicWards(provinceCode!), enabled: !!provinceCode, staleTime: 300_000});
  if (query.isLoading) return <main className="tm-teacher-profile tm-teacher-profile__loading" role="status" aria-label="Đang tải hồ sơ gia sư"><Skeleton active paragraph={{rows: 8}}/></main>;
  if (query.isError || !profile) return <main className="tm-teacher-profile tm-teacher-profile__load-error" role="alert"><h1>Chưa tải được hồ sơ</h1><p>Thông tin hồ sơ chưa sẵn sàng. Thử tải lại để tiếp tục.</p><Button onClick={() => query.refetch()}>Thử lại</Button></main>;
  const p = profile;
  const status = p.approvalStatus || 'DRAFT';
  const saveResidence = async (values: Pick<TeacherProfile, 'provinceCode'|'wardCode'>) => {
    setResidenceBusy(true);
    try { await teacherApi.updateResidence(values); await client.invalidateQueries({queryKey: teacherDashboardKeys.profile()}); message.success('Đã cập nhật nơi ở.'); }
    catch { message.error('Chưa cập nhật được nơi ở.'); }
    finally { setResidenceBusy(false); }
  };

  return <main className="tm-stack tm-teacher-profile">
    <header className="tm-page-heading tm-teacher-profile__heading">
      <h1>Hồ sơ gia sư</h1>
      <p>Thông tin rõ ràng giúp học viên hiểu cách bạn giảng dạy.</p>
    </header>

    <section className={`tm-teacher-profile__status tm-teacher-profile__status--${status.toLowerCase()}`} aria-labelledby="teacher-profile-status-title" role="status">
      <div>
        <h2 id="teacher-profile-status-title">{{DRAFT: 'Hồ sơ bản nháp', PENDING_APPROVAL: 'Hồ sơ đang chờ duyệt', APPROVED: 'Hồ sơ đã được duyệt', REJECTED: 'Hồ sơ cần bổ sung'}[status]}</h2>
        <p>{p.rejectionReason || (status === 'APPROVED' ? 'Chỉnh sửa nội dung hồ sơ sẽ cần gửi duyệt lại. Nơi ở có thể cập nhật riêng.' : 'Hoàn thiện giới thiệu, môn dạy và tài liệu xác minh trước khi gửi duyệt.')}</p>
      </div>
    </section>

    <section className="tm-teacher-profile__section" aria-labelledby="teacher-profile-content-title">
      <div className="tm-teacher-profile__section-heading">
        <h2 id="teacher-profile-content-title">Thông tin giảng dạy</h2>
        <p>Các nội dung này giúp học viên hiểu kinh nghiệm và cách bạn tổ chức buổi học.</p>
      </div>
      <Form className="tm-teacher-profile__form" key={JSON.stringify(p)} layout="vertical" initialValues={p} disabled={busy || status === 'PENDING_APPROVAL'} onFinish={save}>
        <Form.Item className="tm-teacher-profile__field--wide" name="bio" label="Giới thiệu và phương pháp giảng dạy" rules={[{required: true, message: 'Nhập giới thiệu'}]}><Input.TextArea rows={6}/></Form.Item>
        <Form.Item className="tm-teacher-profile__field--experience" name="yearsOfExperience" label="Số năm kinh nghiệm" rules={[{required: true}]}><InputNumber min={0} max={80}/></Form.Item>
        <Form.Item className="tm-teacher-profile__field--languages" name="languages" label="Ngôn ngữ giảng dạy"><Select mode="tags" options={['Tiếng Việt', 'Tiếng Anh'].map(value => ({value, label: value}))}/></Form.Item>
        <fieldset className="tm-teacher-profile__teaching-modes">
          <legend>Hình thức giảng dạy</legend>
          <div className="tm-teacher-profile__switches">
            <Form.Item className="tm-teacher-profile__field--online" name="supportsOnline" label="Dạy online" valuePropName="checked"><Switch/></Form.Item>
            <Form.Item className="tm-teacher-profile__field--offline" name="supportsOffline" label="Dạy trực tiếp" valuePropName="checked"><Switch/></Form.Item>
          </div>
        </fieldset>
        <Form.Item className="tm-teacher-profile__field--video" name="introductionVideoUrl" label="Link video giới thiệu" rules={[{type: 'url', message: 'Nhập đường dẫn video hợp lệ'}]}><Input/></Form.Item>
        <div className="tm-teacher-profile__form-action"><Button className="tm-teacher-profile__save-profile" htmlType="submit" type="primary" loading={busy}>Lưu hồ sơ</Button></div>
      </Form>
    </section>

    <section className="tm-teacher-profile__section tm-teacher-profile__residence" aria-labelledby="teacher-profile-residence-title">
      <div className="tm-teacher-profile__section-heading">
        <h2 id="teacher-profile-residence-title">Nơi ở</h2>
        <p>Chỉ hiển thị khu vực tổng quát để học viên dễ trao đổi; không hiển thị địa chỉ chi tiết.</p>
      </div>
      <Form className="tm-teacher-profile__residence-form" form={residenceForm} layout="vertical" initialValues={{provinceCode: p.provinceCode, wardCode: p.wardCode}} onFinish={saveResidence}>
        <Form.Item className="tm-teacher-profile__field--province" name="provinceCode" label="Tỉnh/thành"><Select allowClear showSearch optionFilterProp="label" placeholder="Chọn tỉnh/thành" options={(provinces.data || []).map(x => ({value: x.code, label: x.name}))} onChange={(value) => {setSelectedProvince(value); residenceForm.setFieldValue('wardCode', undefined);}}/></Form.Item>
        <Form.Item className="tm-teacher-profile__field--ward" name="wardCode" label="Xã/phường"><Select allowClear showSearch optionFilterProp="label" disabled={!provinceCode} placeholder="Chọn xã/phường" options={(wards.data || []).map(x => ({value: x.code, label: x.name}))}/></Form.Item>
        <div className="tm-teacher-profile__form-action"><Button className="tm-teacher-profile__save-residence" htmlType="submit" type="primary" loading={residenceBusy}>Lưu nơi ở</Button></div>
      </Form>
    </section>

    {['DRAFT', 'REJECTED'].includes(status) && <div className="tm-teacher-profile__approval-action"><Button className="tm-teacher-profile__submit-approval" type="primary" loading={busy} onClick={async () => {setBusy(true); try {await teacherApi.submitProfile(); await query.refetch(); message.success('Đã gửi hồ sơ xét duyệt');} catch {message.error('Chưa gửi được hồ sơ. Kiểm tra điều kiện và thử lại.');} finally {setBusy(false);}}}>Gửi hồ sơ xét duyệt</Button></div>}
  </main>;
}
