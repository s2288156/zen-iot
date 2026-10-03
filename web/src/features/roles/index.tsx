import { PlusOutlined } from '@ant-design/icons'
import { Button, Card } from 'antd'
import { useState } from 'react'
import { deleteRole } from '../../api/roles'
import { PageTable } from '../../components/PageTable'
import type { PageTableFilter } from '../../components/PageTable'
import { useAction } from '../../hooks/useAction'
import type { RoleView } from '../../types/auth'
import { RoleFormModal } from './RoleFormModal'
import { RoleModulesModal } from './RoleModulesModal'
import { buildRoleColumns } from './roleColumns'

/** 弹窗编排状态：关闭只翻 `open` 并保留 `target`，退出动画期间内容不会瞬间变白 */
interface ModalState {
  open: boolean
  target: RoleView | null
}

const CLOSED: ModalState = { open: false, target: null }

/** `name` 必须与 `RoleQuery` 的字段名逐字一致（只有 roleCode / roleName 两个模糊条件，没有状态过滤） */
const FILTERS: PageTableFilter[] = [
  { name: 'roleCode', label: '角色编码', type: 'text', placeholder: '支持模糊匹配' },
  { name: 'roleName', label: '角色名称', type: 'text', placeholder: '支持模糊匹配' },
]

/** 角色管理页（Task 5.3）：查询、创建/编辑、删除、模块授权 */
export function Component() {
  const [formModal, setFormModal] = useState<ModalState>(CLOSED)
  const [modulesModal, setModulesModal] = useState<ModalState>(CLOSED)

  // 删除失败（409「角色仍被用户引用,无法删除」）走 toast：行内操作没有就地的展示位
  const remove = useAction<void, number>((id) => deleteRole(id), { domain: 'roles', success: '角色已删除' })

  const columns = buildRoleColumns({
    onEdit: (role) => setFormModal({ open: true, target: role }),
    onAssignModules: (role) => setModulesModal({ open: true, target: role }),
    onDelete: (role) => remove.mutate(role.id),
  })

  return (
    <Card title="角色管理">
      <PageTable<RoleView>
        domain="roles"
        path="/roles/page"
        rowKey="id"
        columns={columns}
        filters={FILTERS}
        scroll={{ x: 1280 }}
        toolbar={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormModal({ open: true, target: null })}>
            新建角色
          </Button>
        }
      />

      <RoleFormModal
        open={formModal.open}
        role={formModal.target}
        onClose={() => setFormModal((state) => ({ ...state, open: false }))}
      />
      {modulesModal.target === null ? null : (
        <RoleModulesModal
          open={modulesModal.open}
          role={modulesModal.target}
          onClose={() => setModulesModal((state) => ({ ...state, open: false }))}
        />
      )}
    </Card>
  )
}
