import { describe, expect, it } from 'vitest';
import { errorKind, errorMessage } from './errors';
import { ApiError } from './api';

// 审计 P-12/P-08：错误码映射与「不透出英文原文」此前完全没有自动化测试。
describe('errorMessage 业务码映射', () => {
  it('已知业务码映射为中文', () => {
    expect(errorMessage(new ApiError(422, 'QA-1001', 'Title too short'))).toContain('标题长度不合法');
    expect(errorMessage(new ApiError(409, 'REP-2002', 'conflict'))).toContain('已有进行中的悬赏');
    expect(errorMessage(new ApiError(403, 'REP-3001', 'forbidden'))).toContain('课程管理者');
  });

  it('未知码按 HTTP 状态兜底', () => {
    expect(errorMessage(new ApiError(500, 'SYS-999', 'boom'))).toContain('服务器开小差');
    expect(errorMessage(new ApiError(404, 'SYS-999', 'gone'))).toContain('不存在');
  });

  it('既无映射、后端文案又是英文时，不透出英文', () => {
    const msg = errorMessage(new ApiError(418, 'SYS-999', 'I am a teapot'));
    expect(msg).toBe('请求失败，请稍后重试');
    expect(msg).not.toContain('teapot');
  });

  it('后端文案是中文时原样透出', () => {
    expect(errorMessage(new ApiError(400, 'SYS-999', '工号格式不正确'))).toBe('工号格式不正确');
  });

  it('fetch 失败等英文 Error 统一翻成中文', () => {
    expect(errorMessage(new Error('Failed to fetch'))).toBe('网络异常，请检查网络后重试');
    expect(errorMessage(new Error('NetworkError when attempting to fetch resource.'))).not.toContain(
      'NetworkError',
    );
  });

  it('非 Error 值给通用中文提示', () => {
    expect(errorMessage('oops')).toBe('未知错误，请稍后重试');
  });
});

describe('errorKind 页面级状态判定', () => {
  it('404 判为 notfound、403 判为 forbidden', () => {
    expect(errorKind(new ApiError(404, 'QA-4001', 'x'))).toBe('notfound');
    expect(errorKind(new ApiError(403, 'QA-3006', 'x'))).toBe('forbidden');
  });

  it('其它错误归类为 other', () => {
    expect(errorKind(new ApiError(500, 'SYS-000', 'x'))).toBe('other');
    expect(errorKind(new Error('boom'))).toBe('other');
  });
});
