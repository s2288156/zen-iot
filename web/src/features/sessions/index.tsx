import { Card, Typography } from 'antd'

// Component 具名导出供路由 lazy 装载。会话管理页实装见 Task 5.4。
export function Component() {
  return (
    <Card title="会话管理" style={{ maxWidth: 960 }}>
      <Typography.Paragraph type="secondary">骨架占位页（Task 5.4 实装：会话列表 + 强制下线）。</Typography.Paragraph>
    </Card>
  )
}
