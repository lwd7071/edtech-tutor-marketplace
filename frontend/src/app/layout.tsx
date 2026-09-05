import type { Metadata } from "next";
import { Inter, Be_Vietnam_Pro, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { AppThemeProvider } from "@/shared/components/AppThemeProvider";
import AuthProvider from "@/shared/components/auth/AuthProvider";

// Configure Next.js optimized fonts
const inter = Inter({
  subsets: ["latin", "vietnamese"],
  display: "swap",
  variable: "--font-body",
  weight: ["400", "500", "600"],
});

const beVietnamPro = Be_Vietnam_Pro({
  subsets: ["latin", "vietnamese"],
  display: "swap",
  variable: "--font-heading",
  weight: ["400", "500", "600", "700"],
});

const jetBrainsMono = JetBrains_Mono({
  subsets: ["latin", "vietnamese"],
  display: "swap",
  variable: "--font-mono",
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
    <html lang="vi" className={`${inter.variable} ${beVietnamPro.variable} ${jetBrainsMono.variable}`}>
      <body className="antialiased">
        <AppThemeProvider>
          <AuthProvider>
            {children}
          </AuthProvider>
        </AppThemeProvider>
      </body>
    </html>
  );
}
