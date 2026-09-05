'use client';

import React, { useState } from 'react';
import { Tabs, Empty, Spin, Row, Col } from 'antd';
import { BookingDetail, BookingStatus } from '../types';
import { BookingCard } from './BookingCard';
import { BookingDetailDrawer } from './BookingDetailDrawer';

interface BookingCalendarViewProps {
  bookings: BookingDetail[];
  isLoading: boolean;
  onStatusChange?: (status?: BookingStatus) => void;
}

export const BookingCalendarView: React.FC<BookingCalendarViewProps> = ({
  bookings,
  isLoading,
  onStatusChange,
}) => {
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [selectedBooking, setSelectedBooking] = useState<BookingDetail | null>(null);
  const [drawerOpen, setDrawerOpen] = useState<boolean>(false);

  const tabItems = [
    { key: 'ALL', label: 'Tất cả' },
    { key: 'SCHEDULED', label: 'Đã lên lịch' },
    { key: 'COMPLETED', label: 'Hoàn thành' },
    { key: 'CANCELLED', label: 'Đã hủy' },
  ];

  const handleTabChange = (key: string) => {
    setActiveTab(key);
    onStatusChange?.(key === 'ALL' ? undefined : (key as BookingStatus));
  };

  const filteredBookings =
    activeTab === 'ALL'
      ? bookings
      : bookings.filter((b) => b.status === activeTab);

  const handleOpenDetail = (booking: BookingDetail) => {
    setSelectedBooking(booking);
    setDrawerOpen(true);
  };

  return (
    <div>
      <Tabs
        activeKey={activeTab}
        items={tabItems}
        onChange={handleTabChange}
        style={{ marginBottom: 20 }}
      />

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin size="large" />
        </div>
      ) : filteredBookings.length === 0 ? (
        <Empty description="Không có buổi học nào trong mục này" style={{ padding: '40px 0' }} />
      ) : (
        <Row gutter={[16, 16]}>
          {filteredBookings.map((b) => (
            <Col xs={24} sm={12} lg={8} key={b.id}>
              <BookingCard booking={b} onViewDetail={handleOpenDetail} />
            </Col>
          ))}
        </Row>
      )}

      <BookingDetailDrawer
        open={drawerOpen}
        booking={selectedBooking}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedBooking(null);
        }}
      />
    </div>
  );
};
