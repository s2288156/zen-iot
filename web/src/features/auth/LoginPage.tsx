import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Card, Flex, Form, Input, Typography, theme } from 'antd'
import { layout, spacing } from '../../theme/tokens'

// Component 具名导出供路由 lazy 装载（react-router 约定）。
// 登录实装见 Task 5.1：接 API 客户端后启用表单、401/403/429 message 透展示、成功回跳原目标。

/** 标识区文案（Task 2.4 定案基线） */
const brand = {
  name: 'zen-iot 管理控制台',
  slogan: 'AGV 调度与设备管理的一体化运维入口',
} as const

/**
 * 视觉基线（Task 2.4）：渐变门面 + 左标识区右卡片；
 * 表单卡片宽度取 layout.loginCardWidth（380px）；
 * 错误展示位为卡片顶部通栏 Alert（v6 口径用 title，不用已废弃的 message 属性），当前留空占位。
 */
export function Component() {
  const { token } = theme.useToken()

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
        {/* 错误展示位：Task 5.1 在有后端错误时渲染 Alert 于此 */}
        <Form layout="vertical" onFinish={() => undefined}>
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="登录账号" disabled />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="登录口令" disabled />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" block disabled>
              登录（Task 5.1 实装）
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </Flex>
  )
}
