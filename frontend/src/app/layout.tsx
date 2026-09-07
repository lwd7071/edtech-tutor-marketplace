import type { Metadata } from "next";
import { Inter, Be_Vietnam_Pro, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import './tutor-match.css';
import { AppThemeProvider } from "@/shared/components/AppThemeProvider";
import AuthProvider from "@/shared/components/auth/AuthProvider";
import { AppProviders } from '@/shared/components/AppProviders';

// Cấu hình webfont tối ưu từ Google Fonts hỗ trợ đầy đủ tiếng Việt
const inter = Inter({
  subsets: ["latin", "vietnamese"],
  display: "swap",
  variable: "--font-inter",
  weight: ["400", "500", "600", "700"],
});

const beVietnamPro = Be_Vietnam_Pro({
  subsets: ["latin", "vietnamese"],
  display: "swap",
  variable: "--font-be-vietnam-pro",
  weight: ["400", "500", "600", "700"],
});

const jetBrainsMono = JetBrains_Mono({
  subsets: ["latin", "vietnamese"],
  display: "swap",
  variable: "--font-jetbrains-mono",
  weight: ["400", "500"],
});

export const metadata: Metadata = {
  title: "Edtech Tutor Marketplace",
  description: "Nền tảng kết nối gia sư 1-1",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className={`${inter.variable} ${beVietnamPro.variable} ${jetBrainsMono.variable}`} suppressHydrationWarning>
      <body className="antialiased" suppressHydrationWarning>
        <AppThemeProvider>
          <AppProviders>
            <AuthProvider>
              {children}
            </AuthProvider>
          </AppProviders>
        </AppThemeProvider>
      </body>
    </html>
  );
}
