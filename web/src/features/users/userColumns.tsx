import { Divider, Popconfirm, Space, Tag, Tooltip, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import { spacing } from '../../theme/tokens'
import type { UserView } from '../../types/admin'
import { EMPTY_TEXT, formatDateTime } from '../../utils/datetime'

export interface UserColumnsDeps {
  /** 当前登录用户 ID；`null` 表示会话尚未就绪，此时不做自我保护判定 */
  currentUserId: number | null
  /** 角色 ID → 显示名；角色列表未加载完成时由调用方兜底成 `#{id}` */
  roleName: (id: number) => string
  onEdit: (user: UserView) => void
  onResetPassword: (user: UserView) => void
  onAssignRoles: (user: UserView) => void
  onChangeStatus: (user: UserView, status: number) => void
  onDelete: (user: UserView) => void
}

/**
 * 用户列表的列定义（Task 5.2）。与页面组件分文件，既是为了单测能脱离路由直接断言渲染与自我保护分支，
 * 也是因为 `react-refresh/only-export-components` 要求一个文件只导出组件。
 *
 * `createTime` 的 `key` 必须是后端排序白名单里的字段名（`UserService#SORTABLE_FIELDS`），
 * 白名单外的值会让 `GET /users/page` 返回 400「不支持的排序字段」。
 *
 * 后端 `UserService#delete` / `#changeStatus` **都没有自删、自停保护**，而对当前登录用户执行这两个操作
 * 会留下一个 Token 仍然有效却再也登不进来的账号，所以这里直接禁用并说明原因。
 */
export function buildUserColumns(deps: UserColumnsDeps): TableColumnsType<UserView> {
  const { currentUserId, roleName, onEdit, onResetPassword, onAssignRoles, onChangeStatus, onDelete } = deps

  return [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 72 },
    { title: '用户名', dataIndex: 'username', key: 'username', width: 140 },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      width: 120,
      render: (value: string | null) => value ?? EMPTY_TEXT,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 200,
      render: (value: string | null) => value ?? EMPTY_TEXT,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 88,
      render: (value: number) => (value === 1 ? <Tag color="success">启用</Tag> : <Tag>禁用</Tag>),
    },
    {
      title: '角色',
      dataIndex: 'roleIds',
      key: 'roleIds',
      width: 200,
      render: (value: number[]) =>
        value.length === 0 ? (
          EMPTY_TEXT
        ) : (
          <Space size={[spacing.xs, spacing.xs]} wrap>
            {value.map((id) => (
              <Tag key={id}>{roleName(id)}</Tag>
            ))}
          </Space>
        ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 176,
      sorter: true,
      render: (value: string | null) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 280,
      render: (_value: unknown, record) => {
        const isSelf = record.id === currentUserId
        const enabled = record.status === 1
        return (
          <Space size={0} separator={<Divider vertical />}>
            <Typography.Link onClick={() => onEdit(record)}>编辑</Typography.Link>
            <Typography.Link onClick={() => onResetPassword(record)}>重置口令</Typography.Link>
            <Typography.Link onClick={() => onAssignRoles(record)}>角色</Typography.Link>
            {isSelf && enabled ? (
              <Tooltip title="不能停用当前登录的自己：后端没有自停保护，停用后你会拿着仍然有效的 Token 却再也登不进来">
                <Typography.Link disabled>禁用</Typography.Link>
              </Tooltip>
            ) : (
              <Popconfirm
                title={enabled ? `确认禁用 ${record.username}？` : `确认启用 ${record.username}？`}
                description={
                  enabled
                    ? '禁用只挡住新登录；已签发的 Token 到过期前仍然有效，需立刻断开请到「会话管理」强制下线。'
                    : undefined
                }
                okText={enabled ? '禁用' : '启用'}
                cancelText="取消"
                okButtonProps={enabled ? { danger: true } : undefined}
                onConfirm={() => onChangeStatus(record, enabled ? 0 : 1)}
              >
                <Typography.Link>{enabled ? '禁用' : '启用'}</Typography.Link>
              </Popconfirm>
            )}
            {isSelf ? (
              <Tooltip title="不能删除当前登录的自己：后端没有自删保护">
                <Typography.Link disabled>删除</Typography.Link>
              </Tooltip>
            ) : (
              <Popconfirm
                title={`确认删除 ${record.username}？`}
                description="逻辑删除，同样不会使已签发的 Token 立即失效。"
                okText="删除"
                cancelText="取消"
                okButtonProps={{ danger: true }}
                onConfirm={() => onDelete(record)}
              >
                <Typography.Link type="danger">删除</Typography.Link>
              </Popconfirm>
            )}
          </Space>
        )
      },
    },
  ]
}
