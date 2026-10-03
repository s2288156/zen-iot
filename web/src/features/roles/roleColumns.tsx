import { Divider, Popconfirm, Space, Tag, Tooltip, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import { spacing } from '../../theme/tokens'
import { MODULE_LABELS } from '../../types/auth'
import type { RoleView } from '../../types/auth'
import { EMPTY_TEXT, formatDateTime } from '../../utils/datetime'

export interface RoleColumnsDeps {
  onEdit: (role: RoleView) => void
  onAssignModules: (role: RoleView) => void
  onDelete: (role: RoleView) => void
}

/**
 * 角色列表的列定义（Task 5.3）。与页面组件分文件，既让单测能脱离路由直接断言渲染，
 * 也是 `react-refresh/only-export-components` 的要求（一个文件只导出组件）。
 *
 * `createTime` 的 `key` 必须落在后端排序白名单里（`RoleService#SORTABLE_FIELDS` =
 * id/roleCode/roleName/createTime/updateTime），白名单外的值会让 `GET /roles/page` 返回 400「不支持的排序字段」。
 *
 * 后端 `RoleService` **没有任何自我保护**：把 `admin` 模块从自己的角色上摘掉是允许的，
 * 只是要等下次登录才生效（模块集合在登录时被读进 Token 快照），故在「模块」入口的弹窗里说明而非在此禁用。
 */
export function buildRoleColumns(deps: RoleColumnsDeps): TableColumnsType<RoleView> {
  const { onEdit, onAssignModules, onDelete } = deps

  return [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 72 },
    { title: '角色编码', dataIndex: 'roleCode', key: 'roleCode', width: 160 },
    { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 160 },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 240,
      ellipsis: true,
      render: (value: string | null) => value ?? EMPTY_TEXT,
    },
    {
      title: '模块',
      dataIndex: 'modules',
      key: 'modules',
      width: 240,
      render: (value: RoleView['modules']) =>
        value.length === 0 ? (
          <Tooltip title="未授予任何模块：持有该角色的用户登录后拿不到任何模块权限，访问管理接口一律 403">
            <Typography.Text type="secondary">未授权</Typography.Text>
          </Tooltip>
        ) : (
          <Space size={[spacing.xs, spacing.xs]} wrap>
            {value.map((code) => (
              <Tag key={code} title={code}>
                {MODULE_LABELS[code]}
              </Tag>
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
      width: 200,
      render: (_value: unknown, record) => (
        <Space size={0} separator={<Divider vertical />}>
          <Typography.Link onClick={() => onEdit(record)}>编辑</Typography.Link>
          <Typography.Link onClick={() => onAssignModules(record)}>模块</Typography.Link>
          <Popconfirm
            title={`确认删除 ${record.roleName}？`}
            description="逻辑删除。仍被用户引用时后端会拒绝（409），需先在用户上解除该角色。"
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => onDelete(record)}
          >
            <Typography.Link type="danger">删除</Typography.Link>
          </Popconfirm>
        </Space>
      ),
    },
  ]
}
