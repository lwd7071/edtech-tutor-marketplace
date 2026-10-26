import { Alert, Button, Tag, Typography } from 'antd';

function OriginalStudentBookingDetail({ item, role, labels, settlementLabels, settlement, confirm, canComplete, setAction, currentReview, review, confirmMutation }: any) {
  return <>
    <section className="tm-panel">
      <p className="tm-eyebrow">{item.trial ? 'Buổi học thử' : 'Buổi học'}</p><h1>{item.subject.name}</h1>
      <p>Gia sư: {item.teacher.fullName} · Học viên: {item.student.fullName}</p>
      <p>{item.startTime} – {item.endTime}</p>
      <p>Trạng thái: {labels[item.status]}</p>
      {item.outsideAvailabilityWarning && <Alert type="warning" showIcon title="Buổi học nằm ngoài lịch rảnh đã công bố" />}
      {!item.trial && item.settlementStatus && <div style={{ margin: '12px 0' }}><Typography.Text>Quyết toán: <Tag color={item.settlementStatus === 'RELEASED' ? 'green' : item.settlementStatus === 'HELD' ? 'orange' : 'blue'}>{settlementLabels[item.settlementStatus]}</Tag></Typography.Text><br /><Typography.Text type="secondary">Hạn xác nhận: {settlement?.confirmationDeadline ? settlement.confirmationDeadline : '—'} · Gia sư: {settlement?.teacherConfirmedAt ? 'đã xác nhận' : 'chưa xác nhận'} · Học viên: {settlement?.studentConfirmedAt ? 'đã xác nhận' : 'chưa xác nhận'}</Typography.Text></div>}
      {item.deliveryMode === 'ONLINE' && item.meetingLink && /^https?:\/\//.test(item.meetingLink) && item.status === 'SCHEDULED' && <a className="tm-button" href={item.meetingLink} target="_blank" rel="noopener noreferrer">Mở phòng học ↗</a>}
      {item.locationAddress && <p>Địa điểm: {item.locationAddress}</p>}
      {role === 'student' && <span>MessageTeacherButton</span>}
      {role === 'student' && item.canConfirm && <Button type="primary" onClick={confirm} loading={confirmMutation.isPending}>Xác nhận buổi học</Button>}
      {canComplete && <Button type="primary" onClick={() => setAction('complete')}>Hoàn thành và viết báo cáo</Button>}
      {role === 'teacher' && item.status === 'SCHEDULED' && <Button onClick={() => setAction('cancel')}>Hủy buổi học</Button>}
      {role === 'teacher' && item.canDispute && <Button onClick={() => setAction('dispute')}>Khiếu nại khoản tiền đang giữ</Button>}
      {role === 'student' && item.canReview && !review.isLoading && !currentReview && <Button onClick={() => setAction('review')}>Đánh giá buổi học</Button>}
      {currentReview && <p>Bạn đã đánh giá <strong>{currentReview.rating}/5</strong>{currentReview.comment ? ` · ${currentReview.comment}` : ''}</p>}
    </section>
    {item.sessionReport && <section className="tm-panel"><h2> Báo cáo buổi học </h2><p className="tm-prose">{item.sessionReport.content}</p><h3>Nhận xét</h3><p>{item.sessionReport.feedback}</p>{item.sessionReport.followUpNote && <p>Việc cần làm tiếp: {item.sessionReport.followUpNote}</p>}</section>}
  </>;
}
