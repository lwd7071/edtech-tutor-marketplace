'use client';

import {useState} from 'react';
import {useQuery, useQueryClient} from '@tanstack/react-query';
import {Alert, App, Button, Form, Input, InputNumber, Select, Skeleton, Switch} from 'antd';
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
  if (query.isLoading) return <Skeleton active/>;
  if (query.isError || !profile) return <Alert type="error" title="Chưa tải được hồ sơ" action={<Button onClick={() => query.refetch()}>Thử lại</Button>}/>;
  const p = profile;
  const status = p.approvalStatus || 'DRAFT';
  const saveResidence = async (values: Pick<TeacherProfile, 'provinceCode'|'wardCode'>) => {
    setResidenceBusy(true);
    try { await teacherApi.updateResidence(values); await client.invalidateQueries({queryKey: teacherDashboardKeys.profile()}); message.success('Đã cập nhật nơi ở.'); }
    catch { message.error('Chưa cập nhật được nơi ở.'); }
    finally { setResidenceBusy(false); }
  };

  return <div className="tm-stack" style={{maxWidth: 820}}>
    <header className="tm-page-heading"><h1>Hồ sơ gia sư</h1><p>Thông tin rõ ràng giúp học viên hiểu cách bạn giảng dạy.</p></header>
    <Alert showIcon type={status === 'REJECTED' ? 'warning' : status === 'APPROVED' ? 'success' : 'info'} title={{DRAFT: 'Hồ sơ bản nháp', PENDING_APPROVAL: 'Hồ sơ đang chờ duyệt', APPROVED: 'Hồ sơ đã được duyệt', REJECTED: 'Hồ sơ cần bổ sung'}[status]} description={p.rejectionReason || (status === 'APPROVED' ? 'Chỉnh sửa nội dung hồ sơ sẽ cần gửi duyệt lại. Nơi ở có thể cập nhật riêng.' : 'Hoàn thiện giới thiệu, môn dạy và tài liệu xác minh trước khi gửi duyệt.')}/>
    <section className="tm-panel"><Form key={JSON.stringify(p)} layout="vertical" initialValues={p} disabled={busy || status === 'PENDING_APPROVAL'} onFinish={save}>
      <Form.Item name="bio" label="Giới thiệu và phương pháp giảng dạy" rules={[{required: true, message: 'Nhập giới thiệu'}]}><Input.TextArea rows={6}/></Form.Item>
      <Form.Item name="yearsOfExperience" label="Số năm kinh nghiệm" rules={[{required: true}]}><InputNumber min={0} max={80}/></Form.Item>
      <Form.Item name="languages" label="Ngôn ngữ giảng dạy"><Select mode="tags" options={['Tiếng Việt', 'Tiếng Anh'].map(value => ({value, label: value}))}/></Form.Item>
      <div className="tm-inline"><Form.Item name="supportsOnline" label="Dạy online" valuePropName="checked"><Switch/></Form.Item><Form.Item name="supportsOffline" label="Dạy trực tiếp" valuePropName="checked"><Switch/></Form.Item></div>
      <Form.Item name="introductionVideoUrl" label="Link video giới thiệu" rules={[{type: 'url', message: 'Nhập đường dẫn video hợp lệ'}]}><Input/></Form.Item>
      <Button htmlType="submit" type="primary" loading={busy}>Lưu hồ sơ</Button>
    </Form></section>
    <section className="tm-panel"><h2>Nơi ở</h2><p className="tm-muted">Chỉ hiển thị khu vực tổng quát để học viên dễ trao đổi; không hiển thị địa chỉ chi tiết.</p><Form form={residenceForm} layout="vertical" initialValues={{provinceCode: p.provinceCode, wardCode: p.wardCode}} onFinish={saveResidence}>
      <Form.Item name="provinceCode" label="Tỉnh/thành"><Select allowClear showSearch optionFilterProp="label" placeholder="Chọn tỉnh/thành" options={(provinces.data || []).map(x => ({value: x.code, label: x.name}))} onChange={(value) => {setSelectedProvince(value); residenceForm.setFieldValue('wardCode', undefined);}}/></Form.Item>
      <Form.Item name="wardCode" label="Xã/phường"><Select allowClear showSearch optionFilterProp="label" disabled={!provinceCode} placeholder="Chọn xã/phường" options={(wards.data || []).map(x => ({value: x.code, label: x.name}))}/></Form.Item>
      <Button htmlType="submit" type="primary" loading={residenceBusy}>Lưu nơi ở</Button>
    </Form></section>
    {['DRAFT', 'REJECTED'].includes(status) && <Button type="primary" loading={busy} onClick={async () => {setBusy(true); try {await teacherApi.submitProfile(); await query.refetch(); message.success('Đã gửi hồ sơ xét duyệt');} catch {message.error('Chưa gửi được hồ sơ. Kiểm tra điều kiện và thử lại.');} finally {setBusy(false);}}}>Gửi hồ sơ xét duyệt</Button>}
  </div>;
}
