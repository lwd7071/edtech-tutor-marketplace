"use client";

import { Button, Card, Typography } from "antd";

const { Title, Paragraph } = Typography;

export default function Home() {
  return (
    <div style={{ padding: "var(--space-8)" }}>
      <Card title="Edtech Tutor Marketplace" style={{ maxWidth: 600, margin: "0 auto" }}>
        <Title level={2}>Frontend Foundation Setup</Title>
        <Paragraph>
          Nếu bạn nhìn thấy thông báo này và nút bên dưới có màu xanh teal (#0F766E), 
          điều đó có nghĩa là Design Tokens và Ant Design Theme (A1.1 & A1.2) đã được thiết lập thành công.
        </Paragraph>
        <div style={{ display: "flex", gap: "var(--space-3)", marginTop: "var(--space-4)" }}>
          <Button type="primary" size="large">
            Nút Primary (Thành công)
          </Button>
          <Button size="large">
            Nút Mặc định
          </Button>
        </div>
      </Card>
    </div>
  );
}
