import { Card, Typography } from 'antd'

// Component 具名导出供路由 lazy 装载。个人中心实装见 Task 5.4。
export function Component() {
  return (
    <Card title="个人中心" style={{ maxWidth: 960 }}>
      <Typography.Paragraph type="secondary">骨架占位页（Task 5.4 实装：自助改密 + 登出）。</Typography.Paragraph>
    </Card>
  )
}
