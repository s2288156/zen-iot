import { AppstoreOutlined, IdcardOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'
import type { ModuleCode } from '../types/auth'

/** 菜单项模型：`path` 同时充当 `Menu` 的 key 与导航目标 */
export interface NavItem {
  path: string
  label: string
  icon: ReactNode
  /** 需要的模块码；`null` 表示对所有已登录账号开放（个人中心自助改密不依赖授权） */
  module: ModuleCode | null
}

/**
 * 侧栏菜单与路由的对应关系：新增业务页只改这张表 + `routes/index.tsx`，两处键值必须一致。
 * `module` 与路由侧 `RequireModule` 同源，菜单隐藏只是免误点，最终判定仍由守卫负责。
 */
export const NAV_ITEMS: readonly NavItem[] = [
  { path: '/users', label: '用户管理', icon: <UserOutlined />, module: 'admin' },
  { path: '/roles', label: '角色管理', icon: <SafetyCertificateOutlined />, module: 'admin' },
  { path: '/sessions', label: '会话管理', icon: <AppstoreOutlined />, module: 'admin' },
  { path: '/profile', label: '个人中心', icon: <IdcardOutlined />, module: null },
]

/** 按会话的模块并集过滤菜单 */
export function visibleNavItems(modules: readonly ModuleCode[]): NavItem[] {
  return NAV_ITEMS.filter((item) => item.module === null || modules.includes(item.module))
}

/** 当前路径命中的菜单项；`/` 结尾用于排除 `/users` 误匹配 `/users-export` 这类前缀 */
export function activeNavItem(items: readonly NavItem[], pathname: string): NavItem | undefined {
  return items.find((item) => pathname === item.path || pathname.startsWith(`${item.path}/`))
}

/**
 * 路由 `handle` 只承载一个布局开关：`fullScreen` 隐藏侧栏（留给未来的编辑器类页面）。
 * 刻意不做面包屑——层级已由侧栏表达，两处导航会互相打架。
 */
export interface RouteHandle {
  fullScreen?: boolean
}
