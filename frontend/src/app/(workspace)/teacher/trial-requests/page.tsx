'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Form, Input, Modal, Pagination, Select, Skeleton, Tag } from 'antd';
import { bookingApi } from '@/features/bookings/api/bookingApi';
import type { TrialRequestView } from '@/features/bookings/types';
import { bookingKeys } from '@/features/bookings/data/bookingKeys';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import { formatVietnamDateTime, vietnamLocalInputToIso } from '@/shared/lib/vietnamTime';
import styles from './trial-requests.module.css';

type TrialAction = 'accept' | 'reject';

type TrialResponseValues = {
  start: string;
  end: string;
  deliveryMode: 'ONLINE' | 'OFFLINE';
  meetingLink?: string;
  locationAddress?: string;
  reason: string;
};

const statusLabels: Record<TrialRequestView['status'], string> = {
  PENDING: 'Chờ phản hồi',
  ACCEPTED: 'Đã chấp nhận',
  REJECTED: 'Đã từ chối',
};

const statusColors: Record<TrialRequestView['status'], string> = {
  PENDING: 'gold',
  ACCEPTED: 'green',
  REJECTED: 'default',
};

function RequestRow({
  request,
  onRespond,
}: {
  request: TrialRequestView;
  onRespond: (request: TrialRequestView, action: TrialAction) => void;
}) {
  return (
    <article className={styles.request} aria-labelledby={`trial-${request.id}`}>
      <div className={styles.requestHeading}>
        <div>
          <p className={styles.eyebrow}>Yêu cầu học thử</p>
          <h2 id={`trial-${request.id}`}>#{request.id.slice(0, 8)}</h2>
        </div>
        <Tag className={styles.statusTag} color={statusColors[request.status]}>{statusLabels[request.status]}</Tag>
      </div>

      <div className={styles.requestDetails}>
        <div className={styles.fact}>
          <span>Mã học viên</span>
          <strong title={request.studentId}>{request.studentId.slice(0, 8)}</strong>
        </div>
        <div className={styles.fact}>
          <span>Mã môn học</span>
          <strong title={request.subjectId}>{request.subjectId.slice(0, 8)}</strong>
        </div>
        <div className={styles.fact}>
          <span>Thời gian mong muốn</span>
          <strong>{formatVietnamDateTime(request.preferredStartTime)}</strong>
        </div>
      </div>

      <div className={styles.note}>
        <h3>Mục tiêu của học viên</h3>
        <p>{request.note || 'Học viên chưa gửi lời nhắn.'}</p>
      </div>

      {request.status === 'REJECTED' && request.rejectionReason ? (
        <p className={styles.rejection}>Lý do từ chối: {request.rejectionReason}</p>
      ) : null}

      {request.status === 'PENDING' ? (
        <div className={styles.actions}>
          <Button className={styles.acceptButton} type="primary" onClick={() => onRespond(request, 'accept')}>
            Chọn thời gian và chấp nhận
          </Button>
          <Button className={styles.rejectButton} onClick={() => onRespond(request, 'reject')}>Từ chối</Button>
        </div>
      ) : null}
    </article>
  );
}

export default function TeacherTrialRequestsPage() {
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<TrialRequestView | null>(null);
  const [action, setAction] = useState<TrialAction>('accept');
  const [busy, setBusy] = useState(false);
  const { message } = App.useApp();
  const query = useQuery({
    queryKey: bookingKeys.teacherTrials(page),
    queryFn: () => bookingApi.getTeacherTrialRequests(undefined, page, 12),
  });

  const openResponse = (request: TrialRequestView, nextAction: TrialAction) => {
    setSelected(request);
    setAction(nextAction);
  };

  const respond = async (values: TrialResponseValues) => {
    if (!selected || busy) return;
    setBusy(true);
    try {
      if (action === 'accept') {
        await bookingApi.acceptTrialRequest(selected.id, {
          startTime: vietnamLocalInputToIso(values.start),
          endTime: vietnamLocalInputToIso(values.end),
          deliveryMode: values.deliveryMode,
          meetingLink: values.meetingLink,
          locationAddress: values.locationAddress,
          version: selected.version,
        });
      } else {
        await bookingApi.rejectTrialRequest(selected.id, {
          reason: values.reason,
          version: selected.version,
        });
      }
      setSelected(null);
      await query.refetch();
      message.success('Đã phản hồi yêu cầu');
    } catch (error) {
      if (isConcurrentModification(error)) {
        message.error('Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.');
        await query.refetch();
      } else {
        message.error(parseApiError(error).message);
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className={styles.page}>
      <header className={styles.pageHeading}>
        <div>
          <p className={styles.eyebrow}>Không gian gia sư</p>
          <h1>Yêu cầu học thử</h1>
          <p>Đọc mục tiêu và thời gian mong muốn trước khi phản hồi từng học viên.</p>
        </div>
        <div className={styles.total} aria-live="polite">
          {query.data?.meta?.totalElements ?? 0} yêu cầu
        </div>
      </header>

      {query.isLoading ? (
        <section className={styles.loading} aria-label="Đang tải yêu cầu học thử">
          <Skeleton className={styles.skeleton} active paragraph={{ rows: 4 }} />
          <Skeleton className={styles.skeleton} active paragraph={{ rows: 4 }} />
        </section>
      ) : query.isError ? (
        <section className={styles.state}>
          <Alert
            className={styles.errorAlert}
            type="error"
            title="Chưa tải được yêu cầu học thử"
            description="Kiểm tra kết nối rồi thử tải lại danh sách."
            action={<Button className={styles.retryButton} onClick={() => query.refetch()}>Thử lại</Button>}
          />
        </section>
      ) : query.data?.data.length ? (
        <>
          <section className={styles.list} aria-label="Danh sách yêu cầu học thử">
            {query.data.data.map(request => (
              <RequestRow key={request.id} request={request} onRespond={openResponse} />
            ))}
          </section>
          {(query.data.meta?.totalPages ?? 0) > 1 ? (
            <div className={styles.pagination}>
              <Pagination
                className={styles.paginationControl}
                current={page + 1}
                total={query.data.meta?.totalElements || 0}
                pageSize={12}
                showSizeChanger={false}
                onChange={current => setPage(current - 1)}
              />
            </div>
          ) : null}
        </>
      ) : (
        <section className={styles.empty}>
          <p className={styles.eyebrow}>Hàng đợi đang trống</p>
          <h2>Chưa có yêu cầu học thử</h2>
          <p>Các yêu cầu mới sẽ xuất hiện tại đây để bạn xem mục tiêu và phản hồi.</p>
        </section>
      )}

      <Modal
        className={styles.modal}
        title={action === 'accept' ? 'Sắp xếp buổi học thử' : 'Lý do từ chối'}
        open={!!selected}
        footer={null}
        onCancel={() => {
          if (!busy) setSelected(null);
        }}
        destroyOnHidden
      >
        <Form className={styles.responseForm} layout="vertical" onFinish={respond}>
          {action === 'accept' ? (
            <>
              <Form.Item className={styles.formItem} name="start" label="Bắt đầu (giờ Việt Nam)" rules={[{ required: true }]}>
                <Input className={styles.formControl} type="datetime-local" />
              </Form.Item>
              <Form.Item
                className={styles.formItem}
                name="end"
                label="Kết thúc"
                dependencies={['start']}
                rules={[
                  { required: true },
                  ({ getFieldValue }) => ({
                    validator: (_, value) =>
                      !value || vietnamLocalInputToIso(value) > vietnamLocalInputToIso(getFieldValue('start'))
                        ? Promise.resolve()
                        : Promise.reject(new Error('Giờ kết thúc phải sau giờ bắt đầu')),
                  }),
                ]}
              >
                <Input className={styles.formControl} type="datetime-local" />
              </Form.Item>
              <Form.Item className={styles.formItem} name="deliveryMode" label="Hình thức" rules={[{ required: true }]}>
                <Select className={styles.formControl} options={[{ value: 'ONLINE', label: 'Online' }, { value: 'OFFLINE', label: 'Trực tiếp' }]} />
              </Form.Item>
              <Form.Item className={styles.formItem} name="meetingLink" label="Link buổi học online" rules={[{ type: 'url' }]}>
                <Input className={styles.formControl} />
              </Form.Item>
              <Form.Item className={styles.formItem} name="locationAddress" label="Địa điểm học trực tiếp">
                <Input className={styles.formControl} />
              </Form.Item>
            </>
          ) : (
            <Form.Item className={styles.formItem} name="reason" label="Lý do" rules={[{ required: true }]}>
              <Input.TextArea className={styles.formControl} rows={4} />
            </Form.Item>
          )}
          <Button className={styles.submitButton} htmlType="submit" type="primary" loading={busy}>
            {action === 'accept' ? 'Tạo buổi học thử' : 'Xác nhận từ chối'}
          </Button>
        </Form>
      </Modal>
    </main>
  );
}
