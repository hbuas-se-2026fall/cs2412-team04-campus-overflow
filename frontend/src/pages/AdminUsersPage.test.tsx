import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminUsersPage } from './AdminUsersPage';
import { api } from '../api';

vi.mock('../auth', () => ({
  useAuth: () => ({ me: { id: 99, role: 'ADMIN' } }),
}));

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
      adminUsers: vi.fn(),
      verifyUser: vi.fn(),
      changeRole: vi.fn(),
      disableUser: vi.fn(),
      createStaff: vi.fn(),
    },
  };
});

describe('AdminUsersPage 禁用账号', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('对已认证用户显示禁用按钮，确认后调用 disableUser 并提示会话失效', async () => {
    vi.mocked(api.adminUsers).mockResolvedValue({
      items: [
        {
          id: 1,
          maskedUsername: 'seed***26',
          displayName: 'SeedUser',
          role: 'STUDENT',
          verified: true,
          status: 'ACTIVE',
          college: '计算机工程学院',
          className: '计科2301',
          createdAt: '2026-11-27T00:00:00Z',
        },
      ],
      total: 1,
      page: 1,
      size: 20,
    });
    vi.mocked(api.disableUser).mockResolvedValue(undefined);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(<AdminUsersPage />);

    expect(await screen.findByText('SeedUser')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '禁用' }));

    await waitFor(() => expect(api.disableUser).toHaveBeenCalledWith(1));
    expect(await screen.findByText(/已禁用 SeedUser/)).toBeInTheDocument();
  });

  it('已禁用账号显示状态且不再提供操作按钮', async () => {
    vi.mocked(api.adminUsers).mockResolvedValue({
      items: [
        {
          id: 2,
          maskedUsername: 'test***nt',
          displayName: 'TestStudent',
          role: 'STUDENT',
          verified: false,
          status: 'DISABLED',
          college: '计算机工程学院',
          className: '计科2301',
          createdAt: '2026-11-27T00:00:00Z',
        },
      ],
      total: 1,
      page: 1,
      size: 20,
    });

    render(<AdminUsersPage />);

    expect(await screen.findByText('TestStudent')).toBeInTheDocument();
    expect(screen.getByText('已禁用')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '禁用' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '认证' })).not.toBeInTheDocument();
  });
});
