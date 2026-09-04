import Navbar from '@/shared/components/layouts/Navbar';
import Footer from '@/shared/components/layouts/Footer';

export default function PublicLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />
      <main style={{ flex: 1, backgroundColor: 'var(--color-background)' }}>
        {children}
      </main>
      <Footer />
    </div>
  );
}
