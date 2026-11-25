import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../auth';
import type { Course } from '../../types';
import { CourseCard } from './CourseCard';

/**
 * 课程列表页：所有人（含未登录）都能浏览；登录后点“加入”建立成员关系。
 * 四种状态都要有界面：加载中 / 空 / 出错可重试 / 正常列表。
 */
export function CourseListPage() {
  const { me, loading: authLoading, refresh } = useAuth();
  const [courses, setCourses] = useState<Course[] | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setCourses(null);
    setError('');
    api
      .courses()
      .then(setCourses)
      .catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!authLoading) load();
  }, [authLoading, load]);

  const handleJoined = (courseId: number) => {
    setCourses(
      (prev) => prev?.map((c) => (c.id === courseId && !c.myRole ? { ...c, myRole: 'STUDENT' } : c)) ?? prev,
    );
    // FE-04：AskPage 读的是 AuthProvider 里的 me.courses，只改本页 courses 不够，
    // 否则新用户加完第一门课去提问仍被判“没有课程”。
    void refresh();
  };

  return (
    <div className="form-page">
      <div className="page-head">
        <h1>课程</h1>
        {me && <Link to="/courses/mine">我的课程 →</Link>}
      </div>
      <p className="muted">选择一门课程加入，之后就能在该课程下提问，问题会出现在同班同学的信息流里。</p>

      {error && (
        <div>
          <p className="error">课程加载失败：{error}</p>
          <button onClick={load}>重试</button>
        </div>
      )}

      {!error && courses === null && <p className="muted">课程加载中…</p>}

      {!error && courses !== null && courses.length === 0 && (
        <div className="empty">
          <p className="muted">本学期还没有开设任何课程。</p>
          <p className="muted">请联系课程管理员（教师）在后台创建课程后再回来。</p>
        </div>
      )}

      {!error && courses !== null && courses.length > 0 && (
        <ul className="course-list">
          {courses.map((c) => (
            <CourseCard key={c.id} course={c} authenticated={Boolean(me)} onJoined={handleJoined} />
          ))}
        </ul>
      )}
    </div>
  );
}
