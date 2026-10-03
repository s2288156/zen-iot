import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Flex, Form, Input, Typography, theme } from 'antd'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { errorText } from '../../api/errors'
import { useAuthStore } from '../../stores/auth'
import { layout, spacing } from '../../theme/tokens'

// Component 具名导出供路由 lazy 装载（react-router 约定）。

/** 标识区文案（Task 2.4 定案基线） */
const brand = {
  name: 'zen-iot 管理控制台',
  slogan: 'AGV 调度与设备管理的一体化运维入口',
} as const

interface LoginForm {
  username: string
  password: string
}

/**
 * `RequireAuth` 把原目标放在 `location.state.from`。它来自 history state，虽然是同源写入，
 * 但仍只接受站内绝对路径：`//host` 会被浏览器当成协议相对 URL，`http://…` 会让 navigate 去匹配路由。
 */
function redirectTarget(state: unknown): string {
  const from = (state as { from?: unknown } | null)?.from
  return typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : '/'
}

/**
 * 视觉基线（Task 2.4）：渐变门面 + 左标识区右卡片；表单卡片宽度取 layout.loginCardWidth（380px）；
 * 错误展示位为卡片顶部通栏 Alert（v6 口径用 title，不用已废弃的 message 属性）。
 *
 * 后端的登录失败文案本身已经够用，直显不加工：401「用户名或密码错误」（用户不存在也是同一句，
 * 后端刻意不区分，避免枚举账号）、403「账号已停用」、400 是字段级校验文案、429 是限流文案。
 */
export function Component() {
  const { token } = theme.useToken()
  const navigate = useNavigate()
  const location = useLocation()
  const login = useAuthStore((state) => state.login)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  const onFinish = async (values: LoginForm) => {
    setPending(true)
    setError(null)
    try {
      // store 内部先存 Token 再拉 /auth/me 与角色，返回即已 authenticated
      await login(values.username, values.password)
      void navigate(redirectTarget(location.state), { replace: true })
    } catch (cause) {
      // 停在登录页就地报错；此时 status 仍是 anonymous（apiLogin 失败不改状态），RequireAuth 不会介入
      setError(errorText(cause, '登录失败，请稍后重试'))
    } finally {
      setPending(false)
    }
  }

  return (
    <Flex
      vertical
      align="center"
      justify="center"
      gap={spacing.xl}
      style={{
        minHeight: '100vh',
        background: `linear-gradient(160deg, ${token.colorPrimaryBg} 0%, ${token.colorBgContainer} 60%)`,
        padding: spacing.lg,
      }}
    >
      <Flex vertical align="center" gap={spacing.sm}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          {brand.name}
        </Typography.Title>
        <Typography.Text type="secondary">{brand.slogan}</Typography.Text>
      </Flex>

      <Card styles={{ body: { padding: spacing.xl } }} style={{ width: layout.loginCardWidth }}>
        {error === null ? null : <Alert type="error" showIcon title={error} style={{ marginBottom: spacing.md }} />}
        <Form<LoginForm> layout="vertical" onFinish={onFinish} disabled={pending}>
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="登录账号" autoComplete="username" autoFocus />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="登录口令" autoComplete="current-password" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" block htmlType="submit" loading={pending}>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </Flex>
  )
}
