import React, { useEffect, useState } from 'react';

interface BookingCountdownProps {
  endTime: string;
}

export const BookingCountdown: React.FC<BookingCountdownProps> = ({ endTime }) => {
  const [timeLeft, setTimeLeft] = useState<{ hours: number; minutes: number } | null>(null);
  const [isExpired, setIsExpired] = useState(false);

  useEffect(() => {
    const calculateTimeLeft = () => {
      const end = new Date(endTime).getTime();
      const deadline = end + 12 * 60 * 60 * 1000; // 12 hours after end time
      const now = new Date().getTime();
      const difference = deadline - now;

      if (difference <= 0) {
        setIsExpired(true);
        setTimeLeft(null);
      } else {
        setIsExpired(false);
        const hours = Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));
        setTimeLeft({ hours, minutes });
      }
    };

    calculateTimeLeft();
    const timer = setInterval(calculateTimeLeft, 60000); // update every minute

    return () => clearInterval(timer);
  }, [endTime]);

  if (isExpired) {
    return <span className="text-error-600 font-semibold">Đã quá hạn xác nhận</span>;
  }

  if (timeLeft) {
    return (
      <span className="text-warning-600 font-semibold">
        Còn {timeLeft.hours} giờ {timeLeft.minutes} phút để xác nhận
      </span>
    );
  }

  return null;
};
