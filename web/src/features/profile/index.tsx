import { Alert, Button, Card, Descriptions, Divider, Form, Input, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { changePassword } from '../../api/auth'
import { errorText } from '../../api/errors'
import { useAction } from '../../hooks/useAction'
import { useAuthStore } from '../../stores/auth'
import { spacing } from '../../theme/tokens'
import { MODULE_LABELS } from '../../types/auth'
import { EMPTY_TEXT } from '../../utils/datetime'

/** 表单值一律可选：antd 对未触碰过的字段不给非空保证，提交时再兜底 */
interface PasswordFormValues {
  oldPassword?: string
  newPassword?: string
  confirm?: string
}

/**
 * 个人中心（Task 5.4）：只读账号信息 + 自助改密。
 *
 * 账号信息取自 auth store（`GET /auth/me`），本页不重复发请求：这些数据只在登录与轮转时变化，
 * store 里那份就是权威副本。改昵称/邮箱要走「用户管理」的编辑入口，后端没有「自助改资料」接口。
 *
 * 改密的会话语义必须写进提示：`AuthService#changePassword` 成功后调 `revokeAllForUserExcept(id, 当前 jti)`，
 * 即**吊销本人除当前会话外的全部会话**，当前这条不受影响，所以成功后不跳登录页、也不需要重新登录。
 * 旧口令不符 → 400「旧口令不正确」，直显在表单上方的 Alert 里。
 */
export function Component() {
  const user = useAuthStore((state) => state.user)
  const modules = useAuthStore((state) => state.modules)
  const [form] = Form.useForm<PasswordFormValues>()
  const [error, setError] = useState<string | null>(null)

  // 失效 `sessions` 域：本人其他会话刚被吊销，在线会话列表已经过期
  const save = useAction<void, PasswordFormValues>(
    (values) => changePassword(values.oldPassword ?? '', values.newPassword ?? ''),
    { domain: 'sessions', success: '口令已修改，本账号的其他会话已下线', silentError: true },
  )

  return (
    <Card title="个人中心" style={{ maxWidth: 960 }}>
      <Descriptions
        column={2}
        items={[
          { key: 'username', label: '用户名', children: user?.username ?? EMPTY_TEXT },
          { key: 'nickname', label: '昵称', children: user?.nickname ?? EMPTY_TEXT },
          { key: 'email', label: '邮箱', children: user?.email ?? EMPTY_TEXT },
          { key: 'phone', label: '手机号', children: user?.phone ?? EMPTY_TEXT },
          {
            key: 'status',
            label: '状态',
            children: user === null ? EMPTY_TEXT : user.status === 1 ? '启用' : '禁用',
          },
          {
            key: 'modules',
            label: '模块权限',
            children:
              modules.length === 0 ? (
                <Typography.Text type="secondary">无</Typography.Text>
              ) : (
                <Space size={[spacing.xs, spacing.xs]} wrap>
                  {modules.map((code) => (
                    <Tag key={code} title={code}>
                      {MODULE_LABELS[code]}
                    </Tag>
                  ))}
                </Space>
              ),
          },
        ]}
      />

      <Divider />

      <Typography.Title level={5}>修改口令</Typography.Title>
      <Typography.Paragraph type="secondary">
        修改成功后，本账号在其他设备上的会话会被立即吊销，当前会话保持不变、无需重新登录。
      </Typography.Paragraph>

      {error === null ? null : <Alert type="error" showIcon title={error} style={{ marginBottom: spacing.md }} />}

      <Form<PasswordFormValues>
        form={form}
        layout="vertical"
        style={{ maxWidth: 380 }}
        disabled={save.isPending}
        onFinish={(values) =>
          save.mutate(values, {
            onSuccess: () => {
              setError(null)
              form.resetFields()
            },
            onError: (cause) => setError(errorText(cause, '修改失败，请稍后重试')),
          })
        }
      >
        <Form.Item
          label="当前口令"
          name="oldPassword"
          // 只校验必填、不校验长度：`ChangePasswordRequest#oldPassword` 刻意不设 `@Size` 下限，
          // 历史短口令用户不应被前端规则挡在改密入口外
          rules={[{ required: true, message: '请输入当前口令' }]}
        >
          <Input.Password autoComplete="current-password" />
        </Form.Item>

        <Form.Item
          label="新口令"
          name="newPassword"
          extra="72 是 BCrypt 的输入上限，超出部分不参与散列"
          rules={[
            { required: true, message: '请输入新口令' },
            { min: 8, max: 72, message: '新口令长度需在 8 ~ 72 个字符之间' },
          ]}
        >
          <Input.Password placeholder="至少 8 位" autoComplete="new-password" />
        </Form.Item>

        <Form.Item
          label="确认新口令"
          name="confirm"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '请再次输入新口令' },
            ({ getFieldValue }) => ({
              validator: (_rule, value: unknown) =>
                value === getFieldValue('newPassword')
                  ? Promise.resolve()
                  : Promise.reject(new Error('两次输入的口令不一致')),
            }),
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>

        <Form.Item style={{ marginBottom: 0 }}>
          <Space>
            {/* 必须有显式提交按钮：多个输入框时浏览器不会隐式提交表单，`onFinish` 也就不会被触发 */}
            <Button type="primary" htmlType="submit" loading={save.isPending}>
              修改口令
            </Button>
            <Typography.Link onClick={() => form.resetFields()}>清空</Typography.Link>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  )
}
