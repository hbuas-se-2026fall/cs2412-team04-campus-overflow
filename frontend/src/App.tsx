import { useEffect, useState } from 'react';
import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth';
import { api } from './api';
import { errorMessage } from './errors';
import { QuestionListPage } from './pages/QuestionListPage';
import { QuestionDetailPage } from './pages/QuestionDetailPage';
import { AskPage } from './pages/AskPage';
import { LoginPage } from './pages/LoginPage';
import { LeaderboardPage } from './pages/LeaderboardPage';
import { ReputationPage } from './pages/ReputationPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { SearchPage } from './pages/SearchPage';
import { CourseListPage } from './pages/courses/CourseListPage';
import { MyCoursesPage } from './pages/courses/MyCoursesPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { AdminUsersPage } from './pages/AdminUsersPage';

function Header() {
  const { me, logout } = useAuth();
  const navigate = useNavigate();
  const [unread, setUnread] = useState(0);
  const [logoutError, setLogoutError] = useState('');

  useEffect(() => {
    if (!me) return;
    const load = () =>
      api
        .unreadCount()
        .then((r) => setUnread(r.count))
        .catch(() => undefined);
    load();
    const timer = setInterval(load, 30_000);
    // 审计 2-6：通知页标记已读后立即刷新角标，不等 30 秒轮询；
    // 路由切换时也重拉一次（角标可能因其他页面操作而变）。
    const onChanged = () => void load();
    window.addEventListener('co:notifications-changed', onChanged);
    window.addEventListener('focus', onChanged);
    return () => {
      clearInterval(timer);
      window.removeEventListener('co:notifications-changed', onChanged);
      window.removeEventListener('focus', onChanged);
    };
  }, [me]);

  return (
    <header className="topbar">
      <Link className="brand" to="/">
        CampusOverflow
      </Link>
      <nav>
        <Link to="/">问题</Link>
        <Link to="/courses">课程</Link>
        <Link to="/search">搜索</Link>
        {me && <Link to="/courses/mine">我的课程</Link>}
        <Link to="/leaderboard">排行榜</Link>
        {me && <Link to="/notifications">通知{unread > 0 && <span className="badge">{unread}</span>}</Link>}
        {me?.role === 'ADMIN' && <Link to="/admin">管理</Link>}
      </nav>
      <div className="spacer" />
      {me ? (
        <div className="user">
          <span>{me.displayName}</span>
          <span className="role">{me.role}</span>
          {/* 审计 2-8：退出失败要给出提示，不能静默（未捕获的 rejection 会被当成没反应） */}
          <button
            onClick={() => {
              setLogoutError('');
              logout()
                .then(() => navigate('/'))
                .catch((e) => setLogoutError(errorMessage(e)));
            }}
          >
            退出
          </button>
          {logoutError && <span className="error">退出失败：{logoutError}</span>}
        </div>
      ) : (
        <Link className="primary" to="/login">
          登录 / 注册
        </Link>
      )}
    </header>
  );
}

function RequireAuth({ children }: { children: JSX.Element }) {
  const { me, loading, authError, refresh } = useAuth();
  if (loading) return <p className="muted">加载中…</p>;
  // 审计 2-8：冷启动 5xx/网络错时登录态「未知」，直接跳登录页会误导用户以为被登出；
  // 给一个可重试的失败态，保留用户的继续操作的可能。
  if (!me && authError) {
    return (
      <div className="state-page">
        <h1>无法确认登录状态</h1>
        <p className="muted">服务器暂时无响应或网络异常，请重试。</p>
        <button className="primary" onClick={() => void refresh()}>
          重试
        </button>
      </div>
    );
  }
  return me ? children : <Navigate to="/login" replace />;
}

/** FE-02：徽章通知链接为 /me/reputation，这里重定向到当前用户的声誉主页。 */
function MyReputationRedirect() {
  const { me, loading } = useAuth();
  if (loading) return <p className="muted">加载中…</p>;
  if (!me) return <Navigate to="/login" replace />;
  return <Navigate to={`/users/${me.id}/reputation`} replace />;
}

/** 管理页只给 ADMIN；非管理员即使手输 URL 也只看到无权限页。 */
function RequireAdmin({ children }: { children: JSX.Element }) {
  const { me, loading } = useAuth();
  if (loading) return <p className="muted">加载中…</p>;
  if (!me) return <Navigate to="/login" replace />;
  if (me.role !== 'ADMIN') {
    return (
      <div className="state-page">
        <h1>无权限访问</h1>
        <p className="muted">管理页仅管理员可用。</p>
        <Link to="/">返回首页</Link>
      </div>
    );
  }
  return children;
}

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Header />
        <main>
          <Routes>
            <Route path="/" element={<QuestionListPage />} />
            <Route path="/questions/:id" element={<QuestionDetailPage />} />
            <Route
              path="/ask"
              element={
                <RequireAuth>
                  <AskPage />
                </RequireAuth>
              }
            />
            <Route path="/courses" element={<CourseListPage />} />
            <Route
              path="/courses/mine"
              element={
                <RequireAuth>
                  <MyCoursesPage />
                </RequireAuth>
              }
            />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/leaderboard" element={<LeaderboardPage />} />
            <Route path="/users/:id/reputation" element={<ReputationPage />} />
            <Route path="/me/reputation" element={<MyReputationRedirect />} />
            <Route
              path="/notifications"
              element={
                <RequireAuth>
                  <NotificationsPage />
                </RequireAuth>
              }
            />
            <Route
              path="/admin"
              element={
                <RequireAdmin>
                  <AdminUsersPage />
                </RequireAdmin>
              }
            />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </main>
      </AuthProvider>
    </BrowserRouter>
  );
}
