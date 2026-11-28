import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import type { Notification } from '../types';

export function NotificationsPage() {
  const [items, setItems] = useState<Notification[] | null>(null);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [size, setSize] = useState(20);
  const [error, setError] = useState('');
  const [loadingMore, setLoadingMore] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);
  // 审计 2-10：未读数必须来自服务端 unread-count，不能用「当前已加载页」里未读的条数——
  // 分页后第 2 页起的未读会被漏算，导致「全部标记为已读」在仍有未读时被错误禁用。
  const [unreadTotal, setUnreadTotal] = useState<number | null>(null);

  const refreshUnread = () =>
    api
      .unreadCount()
      .then((r) => setUnreadTotal(r.count))
      .catch(() => setUnreadTotal(null));

  const load = (pageNum = 1, append = false) => {
    if (append) setLoadingMore(true);
    return api
      .notifications(pageNum, size)
      .then((p) => {
        // offset 分页在新通知插入时会整体下移，翻页可能与上一页重叠；这里按 id 去重。
        setItems((prev) => {
          if (!append || !prev) return p.items;
          const seen = new Set(prev.map((n) => n.id));
          return [...prev, ...p.items.filter((n) => !seen.has(n.id))];
        });
        setTotal(p.total);
        setSize(p.size);
        setPage(pageNum);
        setError('');
      })
      .catch((e: Error) => {
        if (!append) setItems([]);
        setError(e.message);
      })
      .finally(() => {
        if (append) setLoadingMore(false);
      });
  };

  useEffect(() => {
    load();
    refreshUnread();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** 通知已读状态变化后：同时刷新本页与顶栏角标（审计 2-6：角标不再等 30 秒轮询）。 */
  const afterReadChange = () => {
    refreshUnread();
    window.dispatchEvent(new Event('co:notifications-changed'));
  };

  const readOne = async (id: number) => {
    // FE-10：接口失败时不能本地标已读，否则界面与后端不一致（刷新后又变回未读）
    try {
      await api.readOne(id);
    } catch {
      setError('标记已读失败，请重试');
      return;
    }
    setItems((prev) => prev?.map((n) => (n.id === id ? { ...n, read: true } : n)) ?? prev);
    setUnreadTotal((prev) => (prev === null ? null : Math.max(0, prev - 1)));
    afterReadChange();
  };

  const markAll = async () => {
    setMarkingAll(true);
    setError('');
    try {
      await api.readAll();
      await load();
      setUnreadTotal(0);
      afterReadChange();
    } catch (e) {
      setError((e as Error).message || '标记已读失败，请重试');
    } finally {
      setMarkingAll(false);
    }
  };

  const loadMore = () => {
    if (loadingMore || (items && items.length >= total)) return;
    void load(page + 1, true);
  };

  const unreadOnPage = items?.filter((n) => !n.read).length ?? 0;
  const unread = unreadTotal ?? unreadOnPage; // 服务端数量不可用时退化为本页计数

  return (
    <div className="form-page">
      <div className="page-head">
        <h1>通知</h1>
        <span className="muted">{unread > 0 ? `${unread} 条未读` : '全部已读'}</span>
      </div>

      <div className="actions">
        <button onClick={markAll} disabled={unread === 0 || markingAll}>
          {markingAll ? '标记中…' : '全部标记为已读'}
        </button>
        <button onClick={() => load()}>刷新</button>
      </div>

      {error && <p className="error">加载失败：{error}</p>}
      {items === null && <p className="muted">加载中…</p>}

      {items !== null && items.length === 0 && !error && (
        <div className="empty">
          <p className="muted">暂时没有通知。</p>
          <p className="muted">有人回答你的问题、采纳你的回答或在评论里 @ 你时，会在这里提醒。</p>
        </div>
      )}

      <ul className="plain">
        {items?.map((n) => (
          <li key={n.id} className={n.read ? 'muted' : ''}>
            <span className="notif-type">{typeLabel(n.type)}</span>
            <Link
              to={n.link}
              onClick={() => {
                if (!n.read) void readOne(n.id);
              }}
            >
              {n.title}
            </Link>
            <span className="muted"> · {new Date(n.createdAt).toLocaleString('zh-CN')}</span>
            {!n.read && (
              <button className="link-btn" onClick={() => readOne(n.id)}>
                标为已读
              </button>
            )}
          </li>
        ))}
      </ul>

      {items !== null && items.length < total && (
        <div className="actions">
          <button onClick={loadMore} disabled={loadingMore}>
            {loadingMore ? '加载中…' : `加载更多（已显示 ${items.length}/${total}）`}
          </button>
        </div>
      )}
    </div>
  );
}

function typeLabel(type: string): string {
  const map: Record<string, string> = {
    ANSWER_RECEIVED: '[回答]',
    ANSWER_ACCEPTED: '[采纳]',
    ANSWER_ENDORSED: '[认证]',
    MENTIONED: '[提及]',
    BOUNTY_AWARDED: '[悬赏]',
    BADGE_AWARDED: '[徽章]',
  };
  return map[type] ?? '[通知]';
}
