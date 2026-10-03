import { Alert, Form, Input, Modal, Select, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { errorText } from '../../api/errors'
import { assignUserRoles, resetUserPassword } from '../../api/users'
import { useAction } from '../../hooks/useAction'
import { spacing } from '../../theme/tokens'
import type { UserView } from '../../types/admin'
import type { RoleView } from '../../types/auth'

/**
 * 用户行上的两个独立任务弹窗（Task 5.2）。
 *
 * 与 `UserFormModal` 不同，这两个组件由页面在 `target !== null` 时才挂载，挂载即打开，
 * 故不需要 `forceRender` 去规避 `useForm` 未连接的告警；关闭后保留挂载，退出动画期间内容不会瞬间变白。
 */

interface PasswordFormValues {
  password?: string
  confirm?: string
}

export interface ResetPasswordModalProps {
  open: boolean
  user: UserView
  onClose: () => void
}

/**
 * 重置他人口令。成功后 `UserService#resetPassword` 会 `revokeAllForUser`，
 * 该用户的**全部会话（含 refresh Token）立即下线**，这一点必须写在成功提示里，否则管理员不知道对方被踢了。
 */
export function ResetPasswordModal({ open, user, onClose }: ResetPasswordModalProps) {
  const [form] = Form.useForm<PasswordFormValues>()
  const [error, setError] = useState<string | null>(null)

  const handleClose = () => {
    setError(null)
    onClose()
  }

  useEffect(() => {
    if (open) form.resetFields()
  }, [open, user, form])

  const save = useAction<void, PasswordFormValues>(
    (values) => resetUserPassword(user.id, { password: values.password ?? '' }),
    { domain: 'users', success: `口令已重置，${user.username} 的全部会话已下线`, silentError: true },
  )

  return (
    <Modal
      open={open}
      mask={{ closable: false }}
      title={`重置口令 · ${user.username}`}
      okText="重置"
      cancelText="取消"
      okButtonProps={{ danger: true }}
      confirmLoading={save.isPending}
      onOk={() => void form.submit()}
      onCancel={handleClose}
    >
      {error === null ? null : <Alert type="error" showIcon title={error} style={{ marginBottom: spacing.md }} />}
      <Typography.Paragraph type="secondary">
        不校验旧口令。重置成功后该用户的全部会话会被立即吊销，需要重新登录。
      </Typography.Paragraph>
      <Form<PasswordFormValues>
        form={form}
        layout="vertical"
        disabled={save.isPending}
        onFinish={(values) =>
          save.mutate(values, {
            onSuccess: handleClose,
            onError: (cause) => setError(errorText(cause, '重置失败，请稍后重试')),
          })
        }
      >
        <Form.Item
          label="新口令"
          name="password"
          rules={[
            { required: true, message: '请输入新口令' },
            { min: 8, max: 72, message: '口令长度需在 8 ~ 72 个字符之间' },
          ]}
        >
          <Input.Password placeholder="至少 8 位" autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          label="确认新口令"
          name="confirm"
          dependencies={['password']}
          rules={[
            { required: true, message: '请再次输入新口令' },
            ({ getFieldValue }) => ({
              validator: (_rule, value: unknown) =>
                value === getFieldValue('password')
                  ? Promise.resolve()
                  : Promise.reject(new Error('两次输入的口令不一致')),
            }),
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

interface RolesFormValues {
  /** 打开时由 `setFieldsValue` 回填成 `user.roleIds`；留可选是因为 antd 对未触碰过的字段不给非空保证 */
  roleIds?: number[]
}

export interface AssignRolesModalProps {
  open: boolean
  user: UserView
  /** 角色候选，由页面从 `/roles/page` 一次拉全量后共享 */
  roles: RoleView[]
  onClose: () => void
}

/**
 * 分配角色。`PUT /users/{id}/roles` 是全量覆盖，`[]` 即清空。
 *
 * 用户的模块权限来自其角色，而角色只在**登录时**被读进 Token 快照，所以改完必须提示重新登录才生效。
 */
export function AssignRolesModal({ open, user, roles, onClose }: AssignRolesModalProps) {
  const [form] = Form.useForm<RolesFormValues>()
  const [error, setError] = useState<string | null>(null)

  const handleClose = () => {
    setError(null)
    onClose()
  }

  useEffect(() => {
    if (!open) return
    form.resetFields()
    form.setFieldsValue({ roleIds: user.roleIds })
  }, [open, user, form])

  const save = useAction<UserView, RolesFormValues>((values) => assignUserRoles(user.id, values.roleIds ?? []), {
    domain: 'users',
    success: `已更新 ${user.username} 的角色，该用户需重新登录才会生效`,
    silentError: true,
  })

  return (
    <Modal
      open={open}
      mask={{ closable: false }}
      title={`分配角色 · ${user.username}`}
      okText="保存"
      cancelText="取消"
      confirmLoading={save.isPending}
      onOk={() => void form.submit()}
      onCancel={handleClose}
    >
      {error === null ? null : <Alert type="error" showIcon title={error} style={{ marginBottom: spacing.md }} />}
      <Form<RolesFormValues>
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
        <Form.Item label="角色" name="roleIds" extra="全量覆盖，清空即收回该用户的全部角色">
          <Select
            mode="multiple"
            allowClear
            placeholder="未分配"
            showSearch={{ optionFilterProp: 'label' }}
            options={roles.map((role) => ({ label: `${role.roleName}（${role.roleCode}）`, value: role.id }))}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
