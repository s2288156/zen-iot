import { Alert, Form, Input, Modal } from 'antd'
import { useEffect, useState } from 'react'
import { errorText } from '../../api/errors'
import { createUser, updateUser } from '../../api/users'
import { useAction } from '../../hooks/useAction'
import { spacing } from '../../theme/tokens'
import type { UserView } from '../../types/admin'
import { textOrNull } from '../../utils/form'

/** 表单值一律是可选字符串；空串与未填在提交时统一归一成 `null`（后端会把 `''` 原样存成空串而不是 NULL） */
interface UserFormValues {
  username?: string
  password?: string
  nickname?: string
  email?: string
  phone?: string
}

export interface UserFormModalProps {
  open: boolean
  /** `null` 为创建态；编辑态下 `username` 不可改（`UserUpdateRequest` 里没有这个字段） */
  user: UserView | null
  onClose: () => void
}

/**
 * 用户创建/编辑弹窗（Task 5.2）。
 *
 * `forceRender` 是必需的：本组件由页面常驻挂载（新建与编辑共用一个实例），关闭时 Modal 不渲染子树，
 * 而 `@rc-component/form` 的 `useForm` 在 `resetFields` 等调用上会打出
 * 「Instance created by `useForm` is not connected to any Form element」的开发期警告。
 *
 * `initialValues` 只在挂载时读取一次，所以编辑态的回填走 effect：`open` 变 true 时先 `resetFields`
 * 再 `setFieldsValue`，这样「编辑 A → 新建 → 编辑 B」不会串值。
 *
 * 校验规则逐字对齐后端 DTO 的 `@NotBlank`/`@Size`/`@Email`；`phone` 刻意不加格式正则，
 * 因为 `UserCreateRequest#phone` 的注释明写「不做格式强校验，长度兜底」。
 */
export function UserFormModal({ open, user, onClose }: UserFormModalProps) {
  const [form] = Form.useForm<UserFormValues>()
  const [error, setError] = useState<string | null>(null)

  // 关闭时清错误而不是打开时清：effect 里同步 setState 会触发级联渲染（React Compiler 规则），
  // 而「先关再开」是唯一的换目标路径，关闭时清一次就够。
  const handleClose = () => {
    setError(null)
    onClose()
  }

  useEffect(() => {
    if (!open) return
    form.resetFields()
    if (user !== null) {
      form.setFieldsValue({
        username: user.username,
        nickname: user.nickname ?? undefined,
        email: user.email ?? undefined,
        phone: user.phone ?? undefined,
      })
    }
  }, [open, user, form])

  const save = useAction<UserView, UserFormValues>(
    (values) => {
      const nickname = textOrNull(values.nickname)
      const email = textOrNull(values.email)
      const phone = textOrNull(values.phone)
      if (user === null) {
        return createUser({
          username: values.username?.trim() ?? '',
          password: values.password ?? '',
          nickname,
          email,
          phone,
          avatar: null,
        })
      }
      // `PUT /users/{id}` 是全量覆盖：`avatar` 不在本表单里，必须原样回填，否则会被写成 NULL
      return updateUser(user.id, { nickname, email, phone, avatar: user.avatar })
    },
    { domain: 'users', success: user === null ? '用户已创建' : '用户已更新', silentError: true },
  )

  return (
    <Modal
      open={open}
      forceRender
      mask={{ closable: false }}
      title={user === null ? '新建用户' : `编辑用户 · ${user.username}`}
      okText="保存"
      cancelText="取消"
      confirmLoading={save.isPending}
      onOk={() => void form.submit()}
      onCancel={handleClose}
    >
      {error === null ? null : <Alert type="error" showIcon title={error} style={{ marginBottom: spacing.md }} />}
      <Form<UserFormValues>
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
          label="用户名"
          name="username"
          extra={user === null ? undefined : '用户名创建后不可修改'}
          rules={[
            { required: true, message: '请输入用户名' },
            { max: 64, message: '用户名最长 64 个字符' },
          ]}
        >
          {/* 创建态传 `undefined` 而非 `false`：显式 `false` 会盖掉 Form 级的 `disabled` */}
          <Input placeholder="登录账号" autoComplete="off" disabled={user === null ? undefined : true} />
        </Form.Item>

        {user === null ? (
          <Form.Item
            label="密码"
            name="password"
            extra="72 是 BCrypt 的输入上限，超出部分不参与散列"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 8, max: 72, message: '密码长度需在 8 ~ 72 个字符之间' },
            ]}
          >
            <Input.Password placeholder="至少 8 位" autoComplete="new-password" />
          </Form.Item>
        ) : null}

        <Form.Item label="昵称" name="nickname" rules={[{ max: 64, message: '昵称最长 64 个字符' }]}>
          <Input placeholder="选填" autoComplete="off" />
        </Form.Item>

        <Form.Item
          label="邮箱"
          name="email"
          rules={[
            { type: 'email', message: '邮箱格式不正确' },
            { max: 128, message: '邮箱最长 128 个字符' },
          ]}
        >
          <Input placeholder="选填，后端不校验唯一性" autoComplete="off" />
        </Form.Item>

        <Form.Item label="手机号" name="phone" rules={[{ max: 32, message: '手机号最长 32 个字符' }]}>
          <Input placeholder="选填" autoComplete="off" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
