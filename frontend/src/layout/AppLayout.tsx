import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Toaster } from 'sonner';
import Navbar from './Navbar';
import Footer from './Footer';
import { authStore } from '@/store/auth.store';

const FULL_BLEED_ROUTES = ['/login', '/register', '/verify-email'];

function AppLayout() {
  const isAuthenticated = authStore((s) => s.isAuthenticated);
  const { pathname } = useLocation();
  const isFullBleed = FULL_BLEED_ROUTES.includes(pathname);

  useEffect(() => {
    if (isAuthenticated) {
      authStore.getState().validateSession();
    }
  }, [isAuthenticated]);

  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className={`flex-1${isFullBleed ? ' bg-[#0D0B26]' : ''}`}>
        <Outlet />
      </main>
      {!isFullBleed && <Footer />}
      <Toaster richColors position="top-center" />
    </div>
  );
}

export default AppLayout;
