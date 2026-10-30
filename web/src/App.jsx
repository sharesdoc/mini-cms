import React from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { ToastProvider } from './components/ui.jsx';
import { useAuth } from './store/auth.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Home from './pages/Home.jsx';
import Customers from './pages/Customers.jsx';
import CustomerDetail from './pages/CustomerDetail.jsx';
import Stats from './pages/Stats.jsx';
import Messages from './pages/Messages.jsx';
import Thread from './pages/Thread.jsx';
import Me from './pages/Me.jsx';
import ConfigPage from './pages/Config.jsx';

// 受保护路由：无 role 跳登录；管理员访问客户专属页跳报表
function Protected({ children, adminOnly, customerOnly }) {
  const { role } = useAuth();
  const loc = useLocation();
  if (!role) return <Navigate to="/login" replace state={{ from: loc.pathname }} />;
  if (adminOnly && role !== 'ADMIN') return <Navigate to="/home" replace />;
  if (customerOnly && role === 'ADMIN') return <Navigate to="/stats" replace />;
  return children;
}

export default function App() {
  return (
    <ToastProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route element={<Protected><Layout /></Protected>}>
          <Route path="/home" element={<Protected customerOnly><Home /></Protected>} />
          <Route path="/customers" element={<Customers />} />
          <Route path="/customers/:id" element={<CustomerDetail />} />
          <Route path="/stats" element={<Protected adminOnly><Stats /></Protected>} />
          <Route path="/messages" element={<Protected customerOnly><Messages /></Protected>} />
          <Route path="/messages/:kind/:id" element={<Protected customerOnly><Thread /></Protected>} />
          <Route path="/me" element={<Me />} />
          <Route path="/config" element={<Protected adminOnly><ConfigPage /></Protected>} />
        </Route>
        <Route path="*" element={<Navigate to="/home" replace />} />
      </Routes>
    </ToastProvider>
  );
}
