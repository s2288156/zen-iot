import { Button, Result } from 'antd'
import { useNavigate } from 'react-router'

/**
 * 403 状态页：`RequireModule` 拦截时**渲染**（不重定向），对齐 spec「无权限访问受保护路由 SHALL 渲染 403 状态页」。
 * 与 Task 4.3 的 404 / ErrorBoundary 同属公共状态页，此处只落 403。
 */
export function ForbiddenPage() {
  const navigate = useNavigate()

  return (
    <Result
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
