import type {
  AdminUser,
  Bounty,
  Course,
  LeaderboardEntry,
  LedgerEntry,
  Me,
  Notification,
  Page,
  QuestionDetail,
  QuestionSummary,
  ReputationProfile,
  Role,
  SearchResult,
} from './types';

/** 后端以 RFC 9457 Problem Details 返回错误，这里统一转成带业务码的异常。 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

function cookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(`${name}=`))
    ?.split('=')[1];
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET') {
    // CSRF 双提交：从 XSRF-TOKEN Cookie 取出 token 放进请求头
    let token = cookie('XSRF-TOKEN');
    if (!token) {
      await fetch('/api/v1/auth/csrf', { credentials: 'include' });
      token = cookie('XSRF-TOKEN');
    }
    if (token) headers['X-XSRF-TOKEN'] = decodeURIComponent(token);
  }
  const res = await fetch(`/api${path}`, {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  let data: unknown;
  if (text) {
    // FE-06：网关（Nginx）对 502/504 会返回 HTML 而非 JSON，直接 JSON.parse 会抛
    // SyntaxError 并丢失 HTTP 状态；这里容错解析，失败时退化为「无 body」再按 status 报错。
    try {
      data = JSON.parse(text);
    } catch {
      data = undefined;
    }
  }
  if (!res.ok) {
    const detail =
      typeof data === 'object' && data !== null && 'detail' in data
        ? String((data as { detail: unknown }).detail)
        : `请求失败（HTTP ${res.status}）`;
    const code =
      typeof data === 'object' && data !== null && 'code' in data
        ? String((data as { code: unknown }).code)
        : 'SYS-000';
    throw new ApiError(res.status, code, detail);
  }
  return data as T;
}

const get = <T>(path: string) => request<T>('GET', path);

export const api = {
  csrf: () => get<{ token: string }>('/v1/auth/csrf'),
  register: (body: Record<string, string>) => request<{ id: number }>('POST', '/v1/auth/register', body),
  login: (username: string, password: string) =>
    request<Me>('POST', '/v1/auth/login', { username, password }),
  logout: () => request<void>('POST', '/v1/auth/logout'),
  me: () => get<Me>('/v1/auth/me'),

  questions: (page = 1, courseId?: number, size = 20) =>
    get<Page<QuestionSummary>>(
      `/v1/questions?page=${page}&size=${size}${courseId ? `&courseId=${courseId}` : ''}`,
    ),
  /** countView=false 用于写操作后的重载，避免同一次访问重复计入浏览量（审计 2-2） */
  question: (id: number, countView = true) =>
    get<QuestionDetail>(`/v1/questions/${id}?countView=${countView}`),
  ask: (body: { courseId?: number; title: string; body: string; tags: string[] }) =>
    request<{ id: number }>('POST', '/v1/questions', body),
  editQuestion: (id: number, body: { title: string; body: string; tags: string[] }) =>
    request<void>('PUT', `/v1/questions/${id}`, body),
  deleteQuestion: (id: number) => request<void>('DELETE', `/v1/questions/${id}`),
  editAnswer: (answerId: number, body: string) => request<void>('PUT', `/v1/answers/${answerId}`, { body }),
  deleteAnswer: (answerId: number) => request<void>('DELETE', `/v1/answers/${answerId}`),
  answer: (questionId: number, body: string) =>
    request<{ id: number }>('POST', `/v1/questions/${questionId}/answers`, { body }),
  accept: (questionId: number, answerId: number) =>
    request<void>('PUT', `/v1/questions/${questionId}/accepted-answer`, { answerId }),
  endorse: (answerId: number) => request<void>('POST', `/v1/answers/${answerId}/endorsement`, {}),
  vote: (targetType: 'QUESTION' | 'ANSWER', targetId: number, value: number) =>
    request<{ myVote: number; scoreDelta: number }>('PUT', '/v1/votes', { targetType, targetId, value }),
  comment: (targetType: 'QUESTION' | 'ANSWER', targetId: number, body: string, parentId?: number) =>
    request<{ id: number }>('POST', '/v1/comments', { targetType, targetId, body, parentId }),

  // 课程：列表带 myRole（null 表示未加入），加入成功后返回 204；未登录也能浏览列表。
  courses: () => get<Course[]>('/v1/courses'),
  myCourses: () => get<Course[]>('/v1/courses/mine'),
  joinCourse: (courseId: number) => request<void>('POST', `/v1/courses/${courseId}/join`, {}),

  search: (
    q: string,
    params: {
      tag?: string;
      sort?: string;
      unanswered?: boolean;
      bounty?: boolean;
      courseId?: number;
      page?: number;
      size?: number;
    } = {},
  ) => {
    const search = new URLSearchParams({ page: String(params.page ?? 1) });
    // 关键词留空时只按筛选条件查询，不传空的 q（后端不会因此报 2 字符限制）
    if (q.trim()) search.set('q', q.trim());
    if (params.tag) search.set('tag', params.tag);
    if (params.sort) search.set('sort', params.sort);
    if (params.unanswered) search.set('unanswered', 'true');
    if (params.bounty) search.set('bounty', 'true');
    if (params.courseId) search.set('courseId', String(params.courseId));
    if (params.size) search.set('size', String(params.size));
    return get<Page<SearchResult>>(`/v1/search/questions?${search}`);
  },
  related: (questionId: number) =>
    get<{ id: number; title: string; answerCount: number; accepted: boolean; similarity: number }[]>(
      `/v1/questions/${questionId}/related`,
    ),
  popularTags: () => get<{ name: string; questionCount: number }[]>('/v1/tags/popular?limit=15'),

  leaderboard: (period: string) => get<LeaderboardEntry[]>(`/v1/leaderboard?period=${period}`),
  reputation: (userId: number) => get<ReputationProfile>(`/v1/users/${userId}/reputation`),
  ledger: (userId: number, page = 1, size = 20) =>
    get<Page<LedgerEntry>>(`/v1/users/${userId}/reputation/ledger?page=${page}&size=${size}`),
  /** 问题最近一次悬赏；无悬赏时后端返回 204，这里统一转成 null。
   *  FE-06：只把「问题不存在（404）」当作「没有悬赏」，5xx 等真实故障必须向上抛，
   *  避免把服务端错误误判成「没有悬赏」。 */
  bountyOf: (questionId: number) =>
    request<Bounty | undefined>('GET', `/v1/questions/${questionId}/bounty`)
      .then((b) => b ?? null)
      .catch((e: unknown) => {
        if (e instanceof ApiError && e.status === 404) return null;
        throw e;
      }),
  bounty: (questionId: number, points: number, days: number) =>
    request<{ id: number }>('POST', `/v1/questions/${questionId}/bounty`, { points, days }),

  notifications: (page = 1, size = 20) =>
    get<Page<Notification>>(`/v1/notifications?page=${page}&size=${size}`),
  unreadCount: () => get<{ count: number }>('/v1/notifications/unread-count'),
  readOne: (id: number) => request<void>('POST', `/v1/notifications/${id}/read`, {}),
  readAll: () => request<{ updated: number }>('POST', '/v1/notifications/read-all', {}),

  // 管理页（T4-05）：后端列表参数是 verified，不是 status=PENDING。
  adminUsers: (page = 1, size = 20, verified = false) =>
    get<Page<AdminUser>>(`/v1/admin/users?verified=${verified}&page=${page}&size=${size}`),
  verifyUser: (userId: number) => request<void>('POST', `/v1/admin/users/${userId}/verify`, {}),
  createStaff: (body: {
    username: string;
    displayName: string;
    email: string;
    password: string;
    role: Role;
    college?: string;
  }) => request<{ id: number }>('POST', '/v1/admin/users', body),
  changeRole: (userId: number, role: Role) =>
    request<void>('PUT', `/v1/admin/users/${userId}/role`, { role }),
  /** 禁用违规账号（T4-02）：后端会同时撤销该账号的会话 */
  disableUser: (userId: number) => request<void>('PUT', `/v1/admin/users/${userId}/disable`, {}),
};
