import { Button, Card, ColorPicker, Flex, Form, Input, InputNumber, Select, Space, Table, Tag, theme } from 'antd'
import { palette, colorPrimary } from './tokens'

// 基调自检页（Task 2.3）：Button/Table/Form 最小合成页，验证 token 贯通。
// 仅 dev 环境经 /theme-self-check 挂载，不进生产路由；不纳入 Task 1.2 的业务域分层。

interface Row {
  key: string
  name: string
  status: 'ok' | 'warn' | 'error'
}

const rows: Row[] = [
  { key: '1', name: 'gateway-service', status: 'ok' },
  { key: '2', name: 'admin-service', status: 'warn' },
  { key: '3', name: 'ecs-service', status: 'error' },
]

const statusTag: Record<Row['status'], { color: string; text: string }> = {
  ok: { color: 'success', text: '正常' },
  warn: { color: 'warning', text: '告警' },
  error: { color: 'error', text: '异常' },
}

// Component 具名导出供路由 lazy 装载（react-router 约定）。
export function Component() {
  const { token } = theme.useToken()

  return (
    <Flex vertical gap={16} style={{ padding: 24 }}>
      <Card title="按钮与控件（主色/圆角/尺寸贯通）">
        <Space wrap>
          <Button type="primary">主要</Button>
          <Button>默认</Button>
          <Button type="dashed">虚线</Button>
          <Button type="text">文本</Button>
          <Input placeholder="输入框" style={{ width: 160 }} />
          <InputNumber placeholder="数字" style={{ width: 120 }} />
          <Select
            style={{ width: 160 }}
            defaultValue="admin"
            options={[
              { value: 'admin', label: 'admin' },
              { value: 'wcs', label: 'wcs' },
            ]}
          />
          <ColorPicker defaultValue={colorPrimary} showText />
        </Space>
      </Card>

      <Card title="表单（焦点/校验口径）">
        <Form layout="inline" initialValues={{ username: 'admin' }}>
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '必填' }]}>
            <Input />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">
              提交
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card title="表格（表头/悬浮/语义色）">
        <Table<Row>
          size="small"
          pagination={false}
          dataSource={rows}
          columns={[
            { title: '服务', dataIndex: 'name' },
            {
              title: '状态',
              dataIndex: 'status',
              render: (_, row) => <Tag color={statusTag[row.status].color}>{statusTag[row.status].text}</Tag>,
            },
          ]}
        />
      </Card>

      <Card title="主色色阶（来自 theme/tokens.ts，与派生 token 对照）">
        <Flex wrap gap={8}>
          {Object.entries(palette).map(([name, hex]) => (
            <Tag key={name} style={{ backgroundColor: hex, color: token.colorTextLightSolid, borderWidth: 0 }}>
              {name} {hex}
            </Tag>
          ))}
        </Flex>
      </Card>
    </Flex>
  )
}
