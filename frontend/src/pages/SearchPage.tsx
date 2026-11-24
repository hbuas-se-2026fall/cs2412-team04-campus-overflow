import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { errorMessage } from '../errors';
import type { Page, SearchResult } from '../types';

/**
 * 搜索页（T3-10）：关键词全文检索 + 标签/未回答/悬赏筛选 + 四种排序 + 分页。
 *
 * 状态全部同步到 URL 查询参数，刷新页面/分享链接能复现同样的筛选结果。
 * 四种界面状态齐备：加载中（骨架）/ 空结果 / 出错可重试 / 正常列表。
 */

const SORTS = [
  ['relevance', '相关度'],
  ['newest', '最新'],
  ['active', '最近活跃'],
  ['hot', '得分最高'],
] as const;

const PAGE_SIZE = 20;

export function SearchPage() {
  const [params, setParams] = useSearchParams();

  const keyword = params.get('q') ?? '';
  const tag = params.get('tag') ?? '';
  const sort = params.get('sort') ?? 'relevance';
  const unanswered = params.get('unanswered') === 'true';
  const bounty = params.get('bounty') === 'true';
  const page = Math.max(1, Number(params.get('page') ?? 1));

  const [draft, setDraft] = useState(keyword);
  const [data, setData] = useState<Page<SearchResult> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  // 重试计数器：只改 URL 的 page 不会触发重新请求（FE-05）
  const [reloadKey, setReloadKey] = useState(0);
  const [tags, setTags] = useState<{ name: string; questionCount: number }[]>([]);

  useEffect(() => {
    setDraft(keyword);
  }, [keyword]);

  useEffect(() => {
    api
      .popularTags()
      .then(setTags)
      .catch(() => undefined);
  }, []);

  /** 改筛选条件统一走这里：除 page 外的改动都回到第 1 页 */
  const update = useCallback(
    (changes: Record<string, string | undefined>, resetPage = true) => {
      const next = new URLSearchParams(params);
      Object.entries(changes).forEach(([k, v]) => {
        if (v === undefined || v === '') next.delete(k);
        else next.set(k, v);
      });
      if (resetPage) next.delete('page');
      setParams(next, { replace: true });
    },
    [params, setParams],
  );

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    api
      .search(keyword, { tag: tag || undefined, sort, unanswered, bounty, page, size: PAGE_SIZE })
      .then((r) => {
        if (!cancelled) {
          setData(r);
          setLoading(false);
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(errorMessage(e));
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [keyword, tag, sort, unanswered, bounty, page, reloadKey]);

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  const activeFilters = useMemo(() => {
    const list: { key: string; label: string }[] = [];
    if (tag) list.push({ key: 'tag', label: `标签：${tag}` });
    if (unanswered) list.push({ key: 'unanswered', label: '仅看未回答' });
    if (bounty) list.push({ key: 'bounty', label: '仅看有悬赏' });
    return list;
  }, [tag, unanswered, bounty]);

  const clearAll = () => {
    setParams(new URLSearchParams(keyword ? { q: keyword } : {}), { replace: true });
  };

  /** 相关标签：把当前结果里出现的标签按出现次数聚合，作为「相关问题」的等价入口 */
  const relatedTags = useMemo(() => {
    const counter = new Map<string, number>();
    data?.items.forEach((q) => q.tags.forEach((t) => counter.set(t, (counter.get(t) ?? 0) + 1)));
    return [...counter.entries()]
      .filter(([name]) => name !== tag)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10);
  }, [data, tag]);

  return (
    <div className="layout">
      <section>
        <h1>搜索问题</h1>

        {/* ---- 关键词 + 排序 ---- */}
        <form
          className="toolbar"
          onSubmit={(e) => {
            e.preventDefault();
            update({ q: draft.trim() });
          }}
        >
          <input
            placeholder="输入关键词（中文至少 2 个字）"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
          />
          <select value={sort} onChange={(e) => update({ sort: e.target.value })}>
            {SORTS.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
          <button className="primary" type="submit">
            搜索
          </button>
        </form>

        {/* ---- 筛选条件 ---- */}
        <div className="filter-row">
          <label className="check">
            <input
              type="checkbox"
              checked={unanswered}
              onChange={(e) => update({ unanswered: e.target.checked ? 'true' : undefined })}
            />
            仅看未回答
          </label>
          <label className="check">
            <input
              type="checkbox"
              checked={bounty}
              onChange={(e) => update({ bounty: e.target.checked ? 'true' : undefined })}
            />
            仅看有悬赏
          </label>
          {activeFilters.length > 0 && (
            <button className="link-btn" onClick={clearAll}>
              清除全部筛选
            </button>
          )}
        </div>

        {/* 已选标签 */}
        {activeFilters.length > 0 && (
          <div className="chip-row">
            {activeFilters.map((f) => (
              <span className="chip" key={f.key}>
                {f.label}
                <button onClick={() => update({ [f.key]: undefined })} title="移除">
                  ×
                </button>
              </span>
            ))}
          </div>
        )}

        {/* ---- 结果 ---- */}
        {error && (
          <div className="empty">
            <p className="error">搜索失败：{error}</p>
            <button onClick={() => setReloadKey((k) => k + 1)}>重试</button>
          </div>
        )}

        {!error && loading && (
          <ul className="qlist" aria-busy="true">
            {[0, 1, 2].map((i) => (
              <li key={i} className="skeleton-row">
                <div className="skeleton skeleton-line short" />
                <div className="skeleton skeleton-line" />
              </li>
            ))}
          </ul>
        )}

        {!error && !loading && data && data.items.length > 0 && (
          <>
            <p className="muted">共 {data.total} 条结果</p>
            <ul className="qlist">
              {data.items.map((q) => (
                <li key={q.id}>
                  <div className="stats">
                    <span>
                      <b>{q.score}</b> 票
                    </span>
                    <span className={q.accepted ? 'accepted' : ''}>
                      <b>{q.answerCount}</b> 回答
                    </span>
                    {typeof q.viewCount === 'number' && <span>{q.viewCount} 浏览</span>}
                  </div>
                  <div className="qmain">
                    <Link className="qtitle" to={`/questions/${q.id}`}>
                      {q.title}
                    </Link>
                    {q.bountyOpen && <span className="tag bounty-tag">💰 悬赏中</span>}
                    <p className="excerpt">{q.excerpt}</p>
                    <div className="tagrow">
                      {q.tags.map((t) => (
                        <button key={t} className="tag" onClick={() => update({ tag: t })}>
                          {t}
                        </button>
                      ))}
                      <span className="muted">
                        {q.author.displayName} · {new Date(q.createdAt).toLocaleString('zh-CN')}
                      </span>
                    </div>
                  </div>
                </li>
              ))}
            </ul>

            {data.total > data.size && (
              <div className="pager">
                <button disabled={page <= 1} onClick={() => update({ page: String(page - 1) }, false)}>
                  上一页
                </button>
                <span className="muted">
                  第 {data.page} / {totalPages} 页
                </span>
                <button
                  disabled={page >= totalPages}
                  onClick={() => update({ page: String(page + 1) }, false)}
                >
                  下一页
                </button>
              </div>
            )}
          </>
        )}

        {/* ---- 空态：区分「没输入关键词」和「筛选无结果」 ---- */}
        {!error && !loading && data && data.items.length === 0 && (
          <div className="empty">
            {keyword.trim() || activeFilters.length > 0 ? (
              <>
                <p className="muted">没有找到匹配的问题。</p>
                <p className="muted">试试更少的关键词、换个说法，或清除筛选条件。</p>
                <button onClick={clearAll}>清除筛选条件</button>
              </>
            ) : (
              <>
                <p className="muted">输入关键词开始搜索。</p>
                <p className="muted">支持中文全文检索，每个词至少 2 个字。</p>
              </>
            )}
          </div>
        )}
      </section>

      <aside>
        {/* ---- 相关标签：来自本次搜索结果，点一下即按该标签继续筛 ---- */}
        {relatedTags.length > 0 && (
          <>
            <h3>相关标签</h3>
            <div className="tagrow">
              {relatedTags.map(([name, count]) => (
                <button key={name} className="tag" onClick={() => update({ tag: name })}>
                  {name}
                  <span className="count">{count}</span>
                </button>
              ))}
            </div>
          </>
        )}

        {/* ---- 相关问题：取结果里回答最多的一条作为入口，进入详情后右侧有完整的相关问题列表 ---- */}
        {data && data.items.length > 0 && (
          <>
            <h3>相关问题</h3>
            <ul className="plain">
              {[...data.items]
                .sort((a, b) => b.answerCount - a.answerCount)
                .slice(0, 5)
                .map((q) => (
                  <li key={q.id}>
                    <Link to={`/questions/${q.id}`}>{q.title}</Link>
                    <span className="muted"> · {q.answerCount} 回答</span>
                  </li>
                ))}
            </ul>
          </>
        )}

        {/* ---- 热门标签快捷入口 ---- */}
        {tags.length > 0 && (
          <>
            <h3>热门标签</h3>
            <div className="tagrow">
              {tags.map((t) => (
                <button
                  key={t.name}
                  className={`tag ${tag === t.name ? 'is-active' : ''}`}
                  onClick={() => update({ tag: t.name })}
                >
                  {t.name}
                  <span className="count">{t.questionCount}</span>
                </button>
              ))}
            </div>
          </>
        )}
      </aside>
    </div>
  );
}
