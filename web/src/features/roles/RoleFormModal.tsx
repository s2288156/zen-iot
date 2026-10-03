import { Alert, Checkbox, Form, Input, Modal, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { errorText } from '../../api/errors'
import { createRole, updateRole } from '../../api/roles'
import { useAction } from '../../hooks/useAction'
import { spacing } from '../../theme/tokens'
import type { ModuleCode, RoleView } from '../../types/auth'
import { MODULE_OPTIONS } from '../../types/auth'
import { textOrNull } from '../../utils/form'

interface RoleFormValues {
  roleCode?: string
  roleName?: string
  description?: string
  modules?: ModuleCode[]
}

export interface RoleFormModalProps {
  open: boolean
  /** `null` 为创建态；编辑态下 `roleCode` 不可改、`modules` 走独立的「模块」入口 */
  role: RoleView | null
  onClose: () => void
}

/**
 * 角色创建/编辑弹窗（Task 5.3）。
 *
 * 创建与编辑的字段集**刻意不同**，因为后端就是两个不同的 DTO：`RoleCreateRequest` 带 `modules`，
 * 而 `RoleUpdateRequest` 只有 `roleName` / `description`，模块授权必须走 `PUT /roles/{id}/modules`。
 * 若把 modules 塞进编辑表单，保存就变成两次串行写，第二次失败会留下「名称已改、模块没改」的半成品，
 * 所以编辑态只给一行指路文案。
 *
 * `forceRender` 与 `UserFormModal` 同理：本组件由页面常驻挂载，不预渲染子树的话
 * `resetFields` 会触发 `@rc-component/form` 的「useForm 未连接到任何 Form 元素」开发期告警。
 */
export function RoleFormModal({ open, role, onClose }: RoleFormModalProps) {
  const [form] = Form.useForm<RoleFormValues>()
  const [error, setError] = useState<string | null>(null)

  // 关闭时清错误而不是打开时清：effect 里同步 setState 会触发级联渲染（React Compiler 规则）
  const handleClose = () => {
    setError(null)
    onClose()
  }

  useEffect(() => {
    if (!open) return
    form.resetFields()
    if (role !== null) {
      form.setFieldsValue({
        roleCode: role.roleCode,
        roleName: role.roleName,
        description: role.description ?? undefined,
      })
    }
  }, [open, role, form])

  const save = useAction<RoleView, RoleFormValues>(
    (values) => {
      const description = textOrNull(values.description)
      if (role === null) {
        return createRole({
          roleCode: values.roleCode?.trim() ?? '',
          roleName: values.roleName?.trim() ?? '',
          description,
          modules: values.modules ?? [],
        })
      }
      return updateRole(role.id, { roleName: values.roleName?.trim() ?? '', description })
    },
    { domain: 'roles', success: role === null ? '角色已创建' : '角色已更新', silentError: true },
  )

  return (
    <Modal
      open={open}
      forceRender
      mask={{ closable: false }}
      title={role === null ? '新建角色' : `编辑角色 · ${role.roleCode}`}
      okText="保存"
      cancelText="取消"
      confirmLoading={save.isPending}
      onOk={() => void form.submit()}
      onCancel={handleClose}
    >
      {error === null ? null : <Alert type="error" showIcon title={error} style={{ marginBottom: spacing.md }} />}
      <Form<RoleFormValues>
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
        <Form.Item
          label="角色编码"
          name="roleCode"
          extra={role === null ? '全库唯一，创建后不可修改' : undefined}
          rules={[
            { required: true, message: '请输入角色编码' },
            { max: 64, message: '角色编码最长 64 个字符' },
          ]}
        >
          {/* 创建态传 `undefined` 而非 `false`：显式 `false` 会盖掉 Form 级的 `disabled` */}
          <Input placeholder="如 wcs-operator" autoComplete="off" disabled={role === null ? undefined : true} />
        </Form.Item>

        <Form.Item
          label="角色名称"
          name="roleName"
          rules={[
            { required: true, message: '请输入角色名称' },
            { max: 64, message: '角色名称最长 64 个字符' },
          ]}
        >
          <Input placeholder="展示用，如 WCS 操作员" autoComplete="off" />
        </Form.Item>

        <Form.Item label="描述" name="description" rules={[{ max: 255, message: '描述最长 255 个字符' }]}>
          <Input.TextArea rows={2} placeholder="选填" autoComplete="off" />
        </Form.Item>

        {role === null ? (
          <Form.Item label="模块授权" name="modules" extra="不勾选即不授予任何模块，创建后仍可在「模块」入口调整">
            <Checkbox.Group<ModuleCode> options={MODULE_OPTIONS} />
          </Form.Item>
        ) : (
          <Typography.Paragraph type="secondary">
            模块授权不在这里改：后端 <Typography.Text code>PUT /roles/&#123;id&#125;</Typography.Text>{' '}
            只接受名称与描述， 请保存后使用列表上的「模块」入口。
          </Typography.Paragraph>
        )}
      </Form>
    </Modal>
  )
}
