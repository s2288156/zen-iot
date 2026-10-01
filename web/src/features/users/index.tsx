import { Card, Typography } from 'antd'

// Component 具名导出供路由 lazy 装载。用户管理页实装见 Task 5.2。
export function Component() {
  return (
    <Card title="用户管理" style={{ maxWidth: 960 }}>
      <Typography.Paragraph type="secondary">
        骨架占位页（Task 5.2 实装：查询、创建/编辑、启停、删除、重置口令、分配角色）。
      </Typography.Paragraph>
    </Card>
  )
}
