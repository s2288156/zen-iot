import { Alert, Checkbox, Form, Modal, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { errorText } from '../../api/errors'
import { assignRoleModules } from '../../api/roles'
import { useAction } from '../../hooks/useAction'
import { spacing } from '../../theme/tokens'
import type { ModuleCode, RoleView } from '../../types/auth'
import { MODULE_OPTIONS } from '../../types/auth'

interface ModulesFormValues {
  /** 打开时由 `setFieldsValue` 回填成 `role.modules`；留可选是因为 antd 对未触碰过的字段不给非空保证 */
  modules?: ModuleCode[]
}

export interface RoleModulesModalProps {
  open: boolean
  role: RoleView
  onClose: () => void
}

/**
 * 角色模块授权（Task 5.3）。`PUT /roles/{id}/modules` 是覆盖式写入，清空即收回全部模块。
 *
 * 由页面在 `target !== null` 时才挂载，挂载即打开，故不需要 `forceRender`；
 * 关闭后保留挂载，退出动画期间内容不会瞬间变白。
 *
 * 后端**不阻止**把 `admin` 从自己所属的角色上摘掉，但模块集合是在登录时读进 Token 快照的，
 * 所以改完当前会话仍然有效、下次登录才失效——这一点必须写在成功提示里，否则管理员会以为改坏了没生效。
 */
export function RoleModulesModal({ open, role, onClose }: RoleModulesModalProps) {
  const [form] = Form.useForm<ModulesFormValues>()
  const [error, setError] = useState<string | null>(null)

  const handleClose = () => {
    setError(null)
    onClose()
  }

  useEffect(() => {
    if (!open) return
    form.resetFields()
    form.setFieldsValue({ modules: role.modules })
  }, [open, role, form])

  const save = useAction<RoleView, ModulesFormValues>((values) => assignRoleModules(role.id, values.modules ?? []), {
    domain: 'roles',
    success: `已更新 ${role.roleName} 的模块授权，持有该角色的用户需重新登录才会生效`,
    silentError: true,
  })

  return (
    <Modal
      open={open}
      mask={{ closable: false }}
      title={`模块授权 · ${role.roleName}`}
      okText="保存"
      cancelText="取消"
      confirmLoading={save.isPending}
      onOk={() => void form.submit()}
      onCancel={handleClose}
    >
      {error === null ? null : <Alert type="error" showIcon title={error} style={{ marginBottom: spacing.md }} />}
      <Typography.Paragraph type="secondary">
        覆盖式写入，全部取消勾选即收回该角色的所有模块权限。模块集合在登录时被读进 Token 快照，
        改动对已登录用户不生效，需要重新登录。
      </Typography.Paragraph>
      <Form<ModulesFormValues>
        form={form}
        layout="vertical"
        disabled={save.isPending}
        onFinish={(values) =>
          save.mutate(values, {
            onSuccess: handleClose,
            onError: (cause) => setError(errorText(cause, '保存失败，请稍后重试')),
          })
        }
      >
        <Form.Item label="模块" name="modules">
          <Checkbox.Group<ModuleCode> options={MODULE_OPTIONS} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
