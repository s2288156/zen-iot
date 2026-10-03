import { PlusOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Card } from 'antd'
import { useState } from 'react'
import { fetchPage, MAX_PAGE_SIZE } from '../../api/page'
import { changeUserStatus, deleteUser } from '../../api/users'
import { PageTable } from '../../components/PageTable'
import type { PageTableFilter } from '../../components/PageTable'
import { useAction } from '../../hooks/useAction'
import { useAuthStore } from '../../stores/auth'
import type { UserView } from '../../types/admin'
import type { RoleView } from '../../types/auth'
import { UserFormModal } from './UserFormModal'
import { AssignRolesModal, ResetPasswordModal } from './UserTaskModals'
import { buildUserColumns } from './userColumns'

/** 弹窗编排状态：关闭只翻 `open` 并保留 `target`，退出动画期间内容不会瞬间变白 */
interface ModalState {
  open: boolean
  target: UserView | null
}

const CLOSED: ModalState = { open: false, target: null }

/** `name` 必须与 `UserQuery` 的字段名逐字一致，否则后端接不到过滤条件 */
const FILTERS: PageTableFilter[] = [
  { name: 'username', label: '用户名', type: 'text', placeholder: '支持模糊匹配' },
  {
    name: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 },
    ],
  },
]

/** 用户管理页（Task 5.2）：查询、创建/编辑、启停、删除、重置口令、分配角色 */
export function Component() {
  const currentUserId = useAuthStore((state) => state.user?.id) ?? null
  const [formModal, setFormModal] = useState<ModalState>(CLOSED)
  const [resetModal, setResetModal] = useState<ModalState>(CLOSED)
  const [rolesModal, setRolesModal] = useState<ModalState>(CLOSED)

  // 角色候选与「角色」列共用一份全量列表：后端没有「不分页取全部角色」的接口，只能取最大页。
  // `/roles/page` 单页上限是 `MAX_PAGE_SIZE`，角色数超过它会静默截断——按当前规模（个位数）不构成问题。
  const rolesQuery = useQuery({
    queryKey: ['roles', 'all'],
    queryFn: () => fetchPage<RoleView>('/roles/page', { pageNum: 1, pageSize: MAX_PAGE_SIZE }),
    select: (page) => page.list,
  })
  const roles = rolesQuery.data ?? []
  const roleNames = new Map(roles.map((role) => [role.id, role.roleName]))

  const toggleStatus = useAction<UserView, { id: number; status: number }>(
    ({ id, status: next }) => changeUserStatus(id, { status: next }),
    { domain: 'users', success: ({ status: next }) => (next === 1 ? '用户已启用' : '用户已禁用') },
  )

  const remove = useAction<void, number>((id) => deleteUser(id), { domain: 'users', success: '用户已删除' })

  const columns = buildUserColumns({
    currentUserId,
    roleName: (id) => roleNames.get(id) ?? `#${id}`,
    onEdit: (user) => setFormModal({ open: true, target: user }),
    onResetPassword: (user) => setResetModal({ open: true, target: user }),
    onAssignRoles: (user) => setRolesModal({ open: true, target: user }),
    onChangeStatus: (user, next) => toggleStatus.mutate({ id: user.id, status: next }),
    onDelete: (user) => remove.mutate(user.id),
  })

  return (
    <Card title="用户管理">
      <PageTable<UserView>
        domain="users"
        path="/users/page"
        rowKey="id"
        columns={columns}
        filters={FILTERS}
        scroll={{ x: 1280 }}
        toolbar={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormModal({ open: true, target: null })}>
            新建用户
          </Button>
        }
      />

      <UserFormModal
        open={formModal.open}
        user={formModal.target}
        onClose={() => setFormModal((state) => ({ ...state, open: false }))}
      />
      {resetModal.target === null ? null : (
        <ResetPasswordModal
          open={resetModal.open}
          user={resetModal.target}
          onClose={() => setResetModal((state) => ({ ...state, open: false }))}
        />
      )}
      {rolesModal.target === null ? null : (
        <AssignRolesModal
          open={rolesModal.open}
          user={rolesModal.target}
          roles={roles}
          onClose={() => setRolesModal((state) => ({ ...state, open: false }))}
        />
      )}
    </Card>
  )
}
