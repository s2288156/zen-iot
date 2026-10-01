import { Card, Typography } from 'antd'

// Component 具名导出供路由 lazy 装载。角色管理页实装见 Task 5.3。
export function Component() {
  return (
    <Card title="角色管理" style={{ maxWidth: 960 }}>
      <Typography.Paragraph type="secondary">骨架占位页（Task 5.3 实装：CRUD + 模块授权）。</Typography.Paragraph>
    </Card>
  )
}
