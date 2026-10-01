import {
  AppstoreOutlined,
  IdcardOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Button, Layout as AntdLayout, Menu, Typography } from 'antd'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { layout } from '../theme/tokens'

const { Header, Sider, Content } = AntdLayout

const menuItems = [
  { key: '/users', icon: <UserOutlined />, label: '用户管理' },
  { key: '/roles', icon: <SafetyCertificateOutlined />, label: '角色管理' },
  { key: '/sessions', icon: <AppstoreOutlined />, label: '会话管理' },
  { key: '/profile', icon: <IdcardOutlined />, label: '个人中心' },
]

/**
 * 主题基调下的骨架布局：可收起 Sider（折叠为图标条）+ 顶栏 + 内容区。
 * 菜单按 ModuleCode 过滤、顶栏用户区（昵称/登出/个人中心入口）与守卫随 Task 3/4 实装。
 */
export function Layout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const selectedKey = menuItems.find((item) => pathname.startsWith(item.key))?.key ?? '/users'

  return (
    <AntdLayout style={{ minHeight: '100vh' }}>
      <Sider collapsible trigger={null} collapsed={collapsed} onCollapse={setCollapsed} width={layout.siderWidth}>
        <Typography.Title level={5} style={{ margin: 16, textAlign: 'center', whiteSpace: 'nowrap' }}>
          {collapsed ? 'zen-iot' : 'zen-iot 管理控制台'}
        </Typography.Title>
        <Menu
          mode="inline"
          theme="dark"
          items={menuItems}
          selectedKeys={[selectedKey]}
          onClick={({ key }) => void navigate(key)}
        />
      </Sider>
      <AntdLayout>
        <Header style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Button
            type="text"
            aria-label={collapsed ? '展开侧栏' : '收起侧栏'}
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed((value) => !value)}
          />
          <Typography.Text type="secondary">骨架布局 · 权限菜单与用户区随 Task 3/4 实装</Typography.Text>
        </Header>
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </AntdLayout>
    </AntdLayout>
  )
}
