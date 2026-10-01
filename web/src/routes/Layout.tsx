import { NavLink, Outlet } from 'react-router'

const navItems = [
  { to: '/users', label: '用户管理' },
  { to: '/roles', label: '角色管理' },
  { to: '/sessions', label: '会话管理' },
  { to: '/profile', label: '个人中心' },
]

// 骨架布局：Task 4 以权限过滤的 AppLayout（可收起 Sider + 顶栏）替换本占位实现。
export function Layout() {
  return (
    <div className="app-shell">
      <aside className="app-sider">
        <div className="app-brand">zen-iot 管理控制台</div>
        <nav className="app-nav">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to}>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="app-main">
        <header className="app-header">骨架布局 · AppLayout 与守卫随 Task 3/4 实装</header>
        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
