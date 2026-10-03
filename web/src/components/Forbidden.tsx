import { Button } from 'antd'
import { useNavigate } from 'react-router'
import { StatusPage } from './StatusPage'

/**
 * 403 状态页：`RequireModule` 拦截时**渲染**（不重定向），对齐 spec「无权限访问受保护路由 SHALL 渲染 403 状态页」。
 * 落在 `AppLayout` 内容区内，故 `fill={false}`——满视口会把侧栏挤出可视区。
 */
export function ForbiddenPage() {
  const navigate = useNavigate()

  return (
    <StatusPage
      fill={false}
      status="403"
      title="无访问权限"
      subTitle="当前账号未被授予该页面所属模块，请联系管理员调整角色授权后重新登录。"
      extra={
        <Button type="primary" onClick={() => void navigate('/')}>
          返回首页
        </Button>
      }
    />
  )
}
