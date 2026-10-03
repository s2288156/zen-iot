import { ReloadOutlined } from '@ant-design/icons'
import { useQueryClient } from '@tanstack/react-query'
import { Button, Card } from 'antd'
import { invalidateDomain } from '../../api/queryKeys'
import { kickoutSession } from '../../api/sessions'
import { PageTable } from '../../components/PageTable'
import { useAction } from '../../hooks/useAction'
import { useAuthStore } from '../../stores/auth'
import type { SessionView } from '../../types/admin'
import { buildSessionColumns } from './sessionColumns'

/**
 * 在线会话页（Task 5.4）：观测谁在线，并把别人的会话踢下线。
 *
 * 列表接口是 `GET /sessions`（不是 `/sessions/page`），只吃 `PageQuery`，没有任何过滤条件，
 * 且服务端恒按过期时刻升序，所以这里既不给 filters 也不给 sortable 列。
 *
 * 全局关了 `refetchOnWindowFocus`，而在线会话是会在没人操作时自己变的（别人登录/登出/被踢），
 * 故给一个显式「刷新」按钮，而不是偷偷开轮询。
 */
export function Component() {
  const currentUserId = useAuthStore((state) => state.user?.id) ?? null
  const client = useQueryClient()

  const kickout = useAction<void, string>((sessionId) => kickoutSession(sessionId), {
    domain: 'sessions',
    success: '该会话已强制下线',
  })

  const columns = buildSessionColumns({
    currentUserId,
    onKickout: (session) => kickout.mutate(session.sessionId),
  })

  return (
    <Card title="在线会话">
      <PageTable<SessionView>
        domain="sessions"
        path="/sessions"
        rowKey="sessionId"
        columns={columns}
        scroll={{ x: 1280 }}
        toolbar={
          <Button icon={<ReloadOutlined />} onClick={() => void invalidateDomain(client, 'sessions')}>
            刷新
          </Button>
        }
      />
    </Card>
  )
}
