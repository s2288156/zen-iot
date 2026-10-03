import { IdcardOutlined, LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined, UserOutlined } from '@ant-design/icons'
import { App, Avatar, Button, Dropdown, Layout as AntdLayout, Menu, Typography } from 'antd'
import type { MenuProps } from 'antd'
import { useState } from 'react'
import { Outlet, useLocation, useMatches, useNavigate } from 'react-router'
import { ApiError } from '../api/errors'
import { activeNavItem, type RouteHandle, visibleNavItems } from '../routes/navigation'
import { useAuthStore } from '../stores/auth'
import { layout, spacing } from '../theme/tokens'

const { Header, Sider, Content } = AntdLayout

const BRAND = 'zen-iot'

/**
 * 控制台骨架布局（Task 4.1）：可收起 Sider + 顶栏用户区 + 内容 `Outlet`。
 *
 * 侧栏与菜单**一律 light**（口径 A）：Menu 的深色态是另一套 `dark*` 组件 token，
 * `tokens.ts` 定制的 `itemSelectedBg` / `itemSelectedColor` 在深色下全部失效，
 * 且深色底要新增色板里没有的层级色。light 下侧栏底色即 `colorBgContainer`，与灰色内容区天然分层。
 */
export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { message } = App.useApp()
  const user = useAuthStore((state) => state.user)
  const modules = useAuthStore((state) => state.modules)
  const logout = useAuthStore((state) => state.logout)

  // 全屏开关取自路由 `handle`：隐藏侧栏、保留顶栏与用户区，供未来的编辑器类页面使用
  const fullScreen = useMatches().some((match) => (match.handle as RouteHandle | undefined)?.fullScreen === true)

  const items = visibleNavItems(modules)
  const active = activeNavItem(items, pathname)
  const displayName = user?.nickname ?? user?.username ?? '未登录'

  const userMenu: MenuProps = {
    items: [
      { key: 'profile', icon: <IdcardOutlined />, label: '个人中心' },
      { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' },
    ],
    onClick: ({ key }) => {
      if (key === 'profile') void navigate('/profile')
      else void handleLogout()
    },
  }

  async function handleLogout(): Promise<void> {
    try {
      await logout()
    } catch (error) {
      // store 语义：本地会话已断，服务端撤销失败才抛上来。静默吞会让人觉得服务端也退干净了
      message.warning(
        error instanceof ApiError
          ? `本地已退出登录，但服务端会话撤销未成功（${error.code}），该会话可能仍在线`
          : '本地已退出登录，服务端会话撤销结果未知',
      )
    }
    void navigate('/login', { replace: true })
  }

  const header = (
    <Header style={{ display: 'flex', alignItems: 'center', gap: spacing.sm }}>
      {fullScreen ? null : (
        <Button
          type="text"
          aria-label={collapsed ? '展开侧栏' : '收起侧栏'}
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={() => setCollapsed((value) => !value)}
        />
      )}
      <Typography.Text type="secondary">{BRAND} 管理控制台</Typography.Text>
      <div style={{ marginLeft: 'auto' }}>
        <Dropdown trigger={['click']} menu={userMenu}>
          <Button type="text" aria-label="用户菜单" style={{ display: 'flex', alignItems: 'center', gap: spacing.sm }}>
            <Avatar size="small" icon={<UserOutlined />} />
            <Typography.Text>{displayName}</Typography.Text>
          </Button>
        </Dropdown>
      </div>
    </Header>
  )

  return (
    <AntdLayout style={{ minHeight: '100vh' }}>
      {fullScreen ? null : (
        <Sider
          theme="light"
          collapsible
          trigger={null}
          collapsed={collapsed}
          onCollapse={setCollapsed}
          width={layout.siderWidth}
          collapsedWidth={layout.siderCollapsedWidth}
        >
          <Typography.Title level={5} style={{ margin: spacing.md, textAlign: 'center', whiteSpace: 'nowrap' }}>
            {collapsed ? BRAND : `${BRAND} 控制台`}
          </Typography.Title>
          <Menu
            mode="inline"
            theme="light"
            items={items.map((item) => ({ key: item.path, icon: item.icon, label: item.label }))}
            selectedKeys={active === undefined ? [] : [active.path]}
            onClick={({ key }) => void navigate(key)}
          />
        </Sider>
      )}
      <AntdLayout>
        {header}
        <Content style={{ padding: spacing.lg }}>
          <Outlet />
        </Content>
      </AntdLayout>
    </AntdLayout>
  )
}
