import type { Metadata } from "next";
import '@fontsource-variable/inter';
import '@fontsource/be-vietnam-pro/400.css';
import '@fontsource/be-vietnam-pro/500.css';
import '@fontsource/be-vietnam-pro/600.css';
import '@fontsource/be-vietnam-pro/700.css';
import '@fontsource/jetbrains-mono/400.css';
import '@fontsource/jetbrains-mono/500.css';
import "./globals.css";
import './tutor-match.css';
import { AppThemeProvider } from "@/shared/components/AppThemeProvider";
import { SessionHydrator } from "@/features/auth";
import { AppProviders } from '@/shared/components/AppProviders';

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
    <html lang="vi" suppressHydrationWarning>
      <body className="antialiased" suppressHydrationWarning>
        <AppThemeProvider>
          <AppProviders>
            <SessionHydrator>
              {children}
            </SessionHydrator>
          </AppProviders>
        </AppThemeProvider>
      </body>
    </html>
  );
}
