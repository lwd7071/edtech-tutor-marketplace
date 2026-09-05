'use client';

import { Button, Input, Select, DatePicker, Checkbox, Radio, Switch, Tabs, Space, Typography, Divider, Card, Alert, Tooltip, Breadcrumb, Pagination, App } from 'antd';
import { QuestionCircleOutlined, UserOutlined } from '@ant-design/icons';
import DebouncedSearch from '@/shared/components/ui/DebouncedSearch';
import RadioCard from '@/shared/components/ui/RadioCard';
import FormItem from '@/shared/components/ui/FormItem';
import ResponsiveModal from '@/shared/components/ui/ResponsiveModal';
import ResponsiveTable from '@/shared/components/ui/ResponsiveTable';
import FileUpload from '@/shared/components/ui/FileUpload';
import { useConfirmDialog } from '@/shared/components/ui/useConfirmDialog';
import TeacherCard from '@/shared/components/data-display/TeacherCard';
import SubjectCard from '@/shared/components/data-display/SubjectCard';
import PackageCard from '@/shared/components/data-display/PackageCard';
import NotificationBell from '@/shared/components/feedback/NotificationBell';
import ChatBubble from '@/shared/components/feedback/ChatBubble';
import TeacherApprovalBanner from '@/shared/components/ui/TeacherApprovalBanner';
import WeeklyScheduleGrid from '@/shared/components/data-display/WeeklyScheduleGrid';
import { useState } from 'react';

const { Title, Text } = Typography;

export default function ComponentSandbox() {
  const { message } = App.useApp();
  const { confirm, confirmContext } = useConfirmDialog();
  const [radioCardVal, setRadioCardVal] = useState<string | number>('1');
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <div style={{ padding: 'var(--space-8)' }}>
      <Title level={2}>A1.4 Shared Primitive Components Sandbox</Title>
      
      <Card title="1. Buttons" style={{ marginBottom: 'var(--space-6)' }}>
        <Space wrap>
          <Button type="primary">Primary</Button>
          <Button>Default</Button>
          <Button type="dashed">Dashed</Button>
          <Button type="text">Text</Button>
          <Button type="link">Link</Button>
          <Button danger>Danger</Button>
          <Button disabled>Disabled</Button>
          <Button loading>Loading</Button>
        </Space>
        <Divider />
        <Space wrap>
          <Button size="small">Small</Button>
          <Button size="middle">Middle</Button>
          <Button size="large">Large</Button>
        </Space>
      </Card>

      <Card title="2. Inputs & Form Controls" style={{ marginBottom: 'var(--space-6)' }}>
        <Space direction="vertical" style={{ width: '100%', maxWidth: 400 }}>
          <Input placeholder="Default Input" />
          <Input prefix={<UserOutlined />} placeholder="With Prefix" />
          <Input.Password placeholder="Password Input" />
          <Input status="error" placeholder="Error Input" />
          <Input disabled placeholder="Disabled Input" />
          <DebouncedSearch 
            placeholder="Debounced Search (500ms)" 
            onSearch={(v) => console.log('Searched:', v)} 
          />
        </Space>
      </Card>

      <Card title="3. Selects & Pickers" style={{ marginBottom: 'var(--space-6)' }}>
        <Space wrap>
          <Select defaultValue="1" style={{ width: 120 }}>
            <Select.Option value="1">Option 1</Select.Option>
            <Select.Option value="2">Option 2</Select.Option>
          </Select>
          <Select mode="multiple" placeholder="Select multiple" style={{ width: 200 }}>
            <Select.Option value="1">Option 1</Select.Option>
            <Select.Option value="2">Option 2</Select.Option>
          </Select>
          <DatePicker />
          <DatePicker.RangePicker />
        </Space>
      </Card>

      <Card title="4. Radio, Checkbox, Switch" style={{ marginBottom: 'var(--space-6)' }}>
        <Space wrap size="large">
          <Checkbox>Checkbox</Checkbox>
          <Checkbox indeterminate>Indeterminate</Checkbox>
          <Radio.Group defaultValue={1}>
            <Radio value={1}>A</Radio>
            <Radio value={2}>B</Radio>
          </Radio.Group>
          <Switch defaultChecked />
        </Space>
        <Divider />
        <Space direction="vertical" style={{ width: 300 }}>
          <RadioCard 
            value="1" 
            checked={radioCardVal === '1'} 
            onChange={setRadioCardVal} 
            title="Gói Cơ bản" 
            description="Lựa chọn tối ưu cho người mới bắt đầu" 
          />
          <RadioCard 
            value="2" 
            checked={radioCardVal === '2'} 
            onChange={setRadioCardVal} 
            title="Gói Nâng cao" 
            description="Bao gồm nhiều tính năng chuyên sâu" 
          />
        </Space>
      </Card>

      <Card title="5. Tabs & FormItem Wrapper" style={{ marginBottom: 'var(--space-6)' }}>
        <Tabs
          defaultActiveKey="1"
          items={[
            { key: '1', label: 'Tab 1', children: 'Content of Tab Pane 1' },
            { key: '2', label: 'Tab 2', children: 'Content of Tab Pane 2' },
          ]}
        />
        <Divider />
        <FormItem label="Email" validateStatus="error" help="Please input a valid email.">
          <Input placeholder="example@email.com" />
        </FormItem>
      </Card>

      <Card title="6. Overlays & Navigation" style={{ marginBottom: 'var(--space-6)' }}>
        {confirmContext}
        <Space wrap>
          <Button onClick={() => setIsModalOpen(true)}>Open Responsive Modal</Button>
          <Button onClick={() => confirm({ title: 'Normal Confirm', content: 'Are you sure?' })}>Normal Confirm</Button>
          <Button danger onClick={() => confirm({ variant: 'danger', title: 'Danger Confirm', content: 'Delete this?' })}>Danger Confirm</Button>
          <Button type="primary" onClick={() => confirm({ variant: 'stale', title: 'Stale Confirm' })}>Stale Confirm (409)</Button>
          <Button onClick={() => message.success('This is a toast message')}>Show Toast</Button>
        </Space>

        <ResponsiveModal
          open={isModalOpen}
          title="Responsive Modal"
          onCancel={() => setIsModalOpen(false)}
          onOk={() => setIsModalOpen(false)}
        >
          <p>This modal will turn into a full-width bottom sheet on mobile devices!</p>
        </ResponsiveModal>

        <Divider />

        <Space direction="vertical" style={{ width: '100%' }}>
          <Alert message="Informational Notes" type="info" showIcon />
          <Alert message="Success message" type="success" showIcon />
          <Alert message="Warning message" type="warning" showIcon />
          <Alert message="Error message" type="error" showIcon />
          <Tooltip title="This is a tooltip explaining the reason">
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, cursor: 'help' }}>
              Hover over me <QuestionCircleOutlined />
            </span>
          </Tooltip>
          <Breadcrumb
            items={[
              { title: 'Home' },
              { title: 'Dashboard' },
              { title: 'Data Display' },
            ]}
          />
        </Space>
      </Card>

      <Card title="7. Data Display (Table, Upload, Pagination)" style={{ marginBottom: 'var(--space-6)' }}>
        <ResponsiveTable 
          dataSource={[{ id: 1, name: 'John Doe', role: 'Teacher' }, { id: 2, name: 'Jane Smith', role: 'Student' }]}
          columns={[
            { title: 'ID', dataIndex: 'id' },
            { title: 'Name', dataIndex: 'name' },
            { title: 'Role', dataIndex: 'role' },
          ]}
          rowKey="id"
          mobileCardRender={(record: { id: number; name: string; role: string; }) => (
            <Space direction="vertical">
              <Text strong>{record.name}</Text>
              <Text type="secondary">{record.role}</Text>
            </Space>
          )}
          pagination={false}
        />
        <Divider />
        <Pagination defaultCurrent={1} total={50} />
        <Divider />
        <FileUpload />
      </Card>

      <Title level={2} style={{ marginTop: 'var(--space-8)' }}>A1.6 Composite Components Sandbox</Title>
      
      <Card title="8. Cards (Teacher, Subject, Package)" style={{ marginBottom: 'var(--space-6)' }}>
        <div style={{ display: 'flex', gap: 'var(--space-4)', flexWrap: 'wrap' }}>
          <div style={{ width: 300 }}>
            <TeacherCard 
              id="1" name="Nguyen Van A" avatarUrl="" isVerified rating={4.8} reviewCount={120} lowestPrice={150000} subjects={['Toán', 'Lý']}
            />
          </div>
          <div style={{ width: 250 }}>
            <TeacherCard 
              id="2" name="Tran Thi B" variant="compact" rating={4.5} lowestPrice={120000} subjects={['Anh']}
            />
          </div>
          <div style={{ width: 200 }}>
            <SubjectCard id="1" name="Toán học" description="Toán cấp 3" teacherCount={12} />
          </div>
          <div style={{ width: 250 }}>
            <PackageCard id="1" name="Khóa cơ bản" price={500000} sessionCount={10} description="Học phí rẻ" />
          </div>
          <div style={{ width: 250 }}>
            <PackageCard id="2" name="Khóa đã mua" price={500000} sessionCount={10} variant="purchased" completedSessions={4} />
          </div>
        </div>
      </Card>

      <Card title="9. Notification & Chat" style={{ marginBottom: 'var(--space-6)' }}>
        <Space size="large" align="start">
          <NotificationBell 
            unreadCount={2}
            notifications={[
              { id: '1', title: 'Thông báo 1', message: 'Bạn có tin nhắn', timestamp: new Date().toISOString(), isRead: false },
              { id: '2', title: 'Thông báo 2', message: 'Đã duyệt', timestamp: new Date(Date.now() - 3600000).toISOString(), isRead: true }
            ]}
          />
          <div style={{ width: 400, border: '1px solid var(--color-border)', padding: 16, borderRadius: 8 }}>
            <ChatBubble variant="other" content="Chào bạn, mình muốn hỏi về khóa học" timestamp={new Date().toISOString()} />
            <ChatBubble variant="own" content="Vâng, bạn cần hỏi gì ạ?\nKhóa học có giá 500k." timestamp={new Date().toISOString()} status="sent" />
            <ChatBubble variant="system" content="Người dùng đã offline" />
          </div>
        </Space>
      </Card>

      <Card title="10. Complex UI" style={{ marginBottom: 'var(--space-6)' }}>
        <TeacherApprovalBanner status="PENDING_APPROVAL" submittedAt={new Date().toISOString()} />
        <TeacherApprovalBanner status="REJECTED" rejectionReason="Ảnh CMND mờ" onEdit={() => {}} />
        
        <Title level={4}>Weekly Schedule (Readonly)</Title>
        <div style={{ height: 400, overflowY: 'auto' }}>
          <WeeklyScheduleGrid 
            mode="readonly"
            availableSlots={[
              { dayOfWeek: 2, startTime: '09:00', endTime: '10:00' },
              { dayOfWeek: 2, startTime: '10:00', endTime: '11:00', isBooked: true },
            ]}
          />
        </div>
      </Card>
    </div>
  );
}
