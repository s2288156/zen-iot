import { Popconfirm, Tooltip, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import type { SessionView } from '../../types/admin'
import { EMPTY_TEXT, formatDateTime } from '../../utils/datetime'

export interface SessionColumnsDeps {
  /** 当前登录用户 ID；`null` 表示会话尚未就绪，此时不做自我保护判定 */
  currentUserId: number | null
  onKickout: (session: SessionView) => void
}

/**
 * 在线会话列表的列定义（Task 5.4）。
 *
 * **任何一列都不能加 `sorter`**：`SessionService#page` 直接按 Redis ZSET 的过期时刻升序取页，
 * 完全忽略 `PageQuery.orderBy` / `orderDirection`。前端放出排序控件只会让人以为点了有效果。
 *
 * `sessionId` 是 `SessionService#recordLogin` 生成的独立 UUID，与 access/refresh 的 JWT `jti` 没有对应关系，
 * 后端也刻意不下发 jti，因此**无法**从列表里认出「哪一条是我自己现在这条会话」，只能按 `userId` 粗判。
 *
 * 而 `SessionService#kickout` 的自锁保护同样是按 `userId` 判的：下线自己名下**任何**一条会话都会
 * 400「不能强制下线自己的会话」，所以这里直接禁用整行而不是只禁某一条。
 */
export function buildSessionColumns(deps: SessionColumnsDeps): TableColumnsType<SessionView> {
  const { currentUserId, onKickout } = deps

  return [
    { title: '会话 ID', dataIndex: 'sessionId', key: 'sessionId', width: 160, ellipsis: true },
    { title: '用户', dataIndex: 'username', key: 'username', width: 140 },
    { title: 'IP', dataIndex: 'ip', key: 'ip', width: 140, render: (value: string | null) => value ?? EMPTY_TEXT },
    {
      title: 'User-Agent',
      dataIndex: 'userAgent',
      key: 'userAgent',
      width: 320,
      ellipsis: true,
      render: (value: string | null) => value ?? EMPTY_TEXT,
    },
    {
      title: '登录时间',
      dataIndex: 'issueTime',
      key: 'issueTime',
      width: 176,
      render: (value: string | null) => formatDateTime(value),
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 176,
      render: (value: string | null) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 120,
      render: (_value: unknown, record) =>
        record.userId === currentUserId ? (
          <Tooltip title="不能强制下线自己的会话：后端按 userId 拒绝（400），要断开当前会话请用右上角的「退出登录」">
            <Typography.Link disabled>强制下线</Typography.Link>
          </Tooltip>
        ) : (
          <Popconfirm
            title={`确认强制下线 ${record.username}？`}
            description="成对拉黑该会话的 access 与 refresh Token，对方下一次请求即 401 且无法续期。"
            okText="下线"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => onKickout(record)}
          >
            <Typography.Link type="danger">强制下线</Typography.Link>
          </Popconfirm>
        ),
    },
  ]
}
