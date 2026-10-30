import { act } from 'react';
import { createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { API } from '../../app/lib/api-client';
import { NotificationBell } from '../../app/components/NotificationBell';

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

describe('NotificationBell', () => {
  let container: HTMLDivElement;
  let root: Root;

  beforeEach(() => {
    jest.useFakeTimers();
    jest.spyOn(API, 'getNotificationCount').mockResolvedValue({ count: 3 });
    jest.spyOn(API, 'getNotifications').mockResolvedValue({
      count: 1,
      items: [{
        id: 'unavail-10',
        type: 'pending_unavailability',
        title: 'Ana',
        subtitle: 'Indisponibilidade · 2026-07-01 a 2026-07-02',
        created_at: null,
        href: '/unavailability',
      }],
    });
    container = document.createElement('div');
    document.body.appendChild(container);
    root = createRoot(container);
  });

  afterEach(async () => {
    await act(async () => root.unmount());
    container.remove();
    jest.restoreAllMocks();
    jest.useRealTimers();
  });

  it('faz polling apenas da contagem e busca itens somente ao abrir', async () => {
    await act(async () => {
      root.render(createElement(NotificationBell));
    });

    await act(async () => Promise.resolve());
    expect(API.getNotificationCount).toHaveBeenCalledTimes(1);
    expect(API.getNotifications).not.toHaveBeenCalled();

    await act(async () => {
      jest.advanceTimersByTime(30_000);
      await Promise.resolve();
    });
    expect(API.getNotificationCount).toHaveBeenCalledTimes(2);
    expect(API.getNotifications).not.toHaveBeenCalled();

    const button = container.querySelector('button[aria-label="Notificações"]');
    expect(button).not.toBeNull();

    await act(async () => {
      button?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      await Promise.resolve();
    });

    expect(API.getNotifications).toHaveBeenCalledTimes(1);
    expect(container.textContent).toContain('Ana');
  });
});
