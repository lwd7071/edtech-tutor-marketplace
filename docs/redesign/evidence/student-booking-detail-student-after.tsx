function CurrentStudentBookingDetail({ item, currentReview, actions, bookingLabels, settlementLabels, formatVietnamDateTime, formatVietnamTime, Alert }: any) {
  return <>
    <header className="tm-booking-detail-heading">
      <div><div className="tm-booking-detail-kind">{item.trial ? 'Buổi học thử' : 'Buổi học'}</div><h1>{item.subject.name}</h1></div>
      <div className="tm-booking-status" role="status" aria-label={`Trạng thái: ${bookingLabels[item.status]}`}>{bookingLabels[item.status]}</div>
    </header>
    <time className="tm-booking-time" dateTime={item.startTime}>{formatVietnamDateTime(item.startTime)} <span aria-hidden="true">–</span> {formatVietnamTime(item.endTime)}</time>
    <section className="tm-booking-facts" aria-labelledby="booking-facts-title">
      <h2 id="booking-facts-title">Thông tin buổi học</h2>
      <dl>
        <div><dt>Gia sư</dt><dd>{item.teacher.fullName}</dd></div>
        <div><dt>Học viên</dt><dd>{item.student.fullName}</dd></div>
        <div><dt>Thời gian</dt><dd>{formatVietnamDateTime(item.startTime)} – {formatVietnamTime(item.endTime)}</dd></div>
        <div><dt>Hình thức</dt><dd>{item.deliveryMode === 'ONLINE' ? 'Trực tuyến' : 'Trực tiếp'}</dd></div>
        {item.locationAddress && <div><dt>Địa điểm</dt><dd>{item.locationAddress}</dd></div>}
        {item.meetingLink && <div><dt>Phòng học</dt><dd><a href={item.meetingLink} target="_blank" rel="noopener noreferrer">Mở phòng học</a></dd></div>}
        {item.status === 'CANCELLED' && item.cancelReason && <div className="tm-booking-cancel-reason"><dt>Lý do hủy</dt><dd>{item.cancelReason}</dd></div>}
      </dl>
      {item.outsideAvailabilityWarning && <Alert className="tm-booking-warning" type="warning" showIcon title="Buổi học nằm ngoài lịch rảnh đã công bố" />}
    </section>
    {!item.trial && item.settlementStatus && <section className="tm-booking-settlement" aria-labelledby="settlement-title">
      <h2 id="settlement-title">Quyết toán buổi học</h2>
      <div className="tm-booking-settlement-status" role="status">{settlementLabels[item.settlementStatus]}</div>
      <dl>
        {item.settlement?.netAmountVnd != null && <div><dt>Số tiền gia sư nhận</dt><dd>{item.settlement.netAmountVnd} ₫</dd></div>}
        <div><dt>Hạn xác nhận</dt><dd>{item.settlement?.confirmationDeadline ? formatVietnamDateTime(item.settlement.confirmationDeadline) : '—'}</dd></div>
        <div><dt>Gia sư</dt><dd>{item.settlement?.teacherConfirmedAt ? 'Đã xác nhận' : 'Chưa xác nhận'}</dd></div>
        <div><dt>Học viên</dt><dd>{item.settlement?.studentConfirmedAt ? 'Đã xác nhận' : 'Chưa xác nhận'}</dd></div>
      </dl>
    </section>}
    <section className="tm-booking-actions" aria-label="Thao tác buổi học">{actions}</section>
    {currentReview && <div className="tm-booking-existing-review">Bạn đã đánh giá <strong>{currentReview.rating}/5</strong>{currentReview.comment ? ` · ${currentReview.comment}` : ''}</div>}
    {item.sessionReport && <section className="tm-booking-report" aria-labelledby="session-report-title"><h2 id="session-report-title">Báo cáo buổi học</h2><div className="tm-booking-report-grid"><div><h3>Nội dung buổi học</h3><p>{item.sessionReport.content}</p></div><div><h3>Nhận xét của gia sư</h3><p>{item.sessionReport.feedback}</p></div>{item.sessionReport.followUpNote && <div><h3>Việc cần làm tiếp</h3><p>{item.sessionReport.followUpNote}</p></div>}</div></section>}
  </>;
}
