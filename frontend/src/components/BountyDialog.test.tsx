import { render, screen } from '@testing-library/react';
import { ApiError } from '../api';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BountyDialog } from './BountyDialog';
import { api } from '../api';

// 隔离 api 依赖，只测组件的门控逻辑（F-02）。
vi.mock('../api', () => {
  class ApiError extends Error {
    readonly status: number;
    readonly code: string;
    constructor(status: number, code: string, message: string) {
      super(message);
      this.status = status;
      this.code = code;
    }
  }
  return {
    ApiError,
    api: {
      bountyOf: vi.fn(),
      bounty: vi.fn(),
    },
  };
});

describe('BountyDialog 发起悬赏门控', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('公开问题（无课程归属）不显示「发起悬赏」入口', async () => {
    vi.mocked(api.bountyOf).mockResolvedValue(null);

    render(<BountyDialog questionId={1} courseId={undefined} accepted={false} canOpen />);

    expect(await screen.findByText('该问题还没有悬赏。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '发起悬赏' })).not.toBeInTheDocument();
  });

  it('已采纳的问题不显示「发起悬赏」入口', async () => {
    vi.mocked(api.bountyOf).mockResolvedValue(null);

    render(<BountyDialog questionId={1} courseId={100} accepted canOpen />);

    expect(await screen.findByText('该问题还没有悬赏。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '发起悬赏' })).not.toBeInTheDocument();
  });

  it('课程内且未采纳的问题显示「发起悬赏」入口', async () => {
    vi.mocked(api.bountyOf).mockResolvedValue(null);

    render(<BountyDialog questionId={1} courseId={100} accepted={false} canOpen />);

    expect(await screen.findByRole('button', { name: '发起悬赏' })).toBeInTheDocument();
  });

  it('无管理权限时不显示「发起悬赏」入口', async () => {
    vi.mocked(api.bountyOf).mockResolvedValue(null);

    render(<BountyDialog questionId={1} courseId={100} accepted={false} canOpen={false} />);

    expect(await screen.findByText('该问题还没有悬赏。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '发起悬赏' })).not.toBeInTheDocument();
  });
});

describe('BountyDialog 加载失败态（审计 2-11）', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('加载失败时显示失败与重试，而不是「该问题还没有悬赏」', async () => {
    vi.mocked(api.bountyOf).mockRejectedValue(new ApiError(500, 'SYS-000', 'boom'));

    render(<BountyDialog questionId={1} courseId={100} accepted={false} canOpen />);

    expect(await screen.findByText(/悬赏信息加载失败/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '重试' })).toBeInTheDocument();
    expect(screen.queryByText('该问题还没有悬赏。')).not.toBeInTheDocument();
  });

  it('加载失败时不允许发起悬赏（状态未知）', async () => {
    vi.mocked(api.bountyOf).mockRejectedValue(new ApiError(502, 'SYS-000', 'bad gateway'));

    render(<BountyDialog questionId={1} courseId={100} accepted={false} canOpen />);

    await screen.findByText(/悬赏信息加载失败/);
    expect(screen.queryByRole('button', { name: '发起悬赏' })).not.toBeInTheDocument();
  });
});
