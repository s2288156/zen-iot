import { Button } from 'antd'
import { useNavigate } from 'react-router'
import { StatusPage } from './StatusPage'

/**
 * 404 状态页：由路由 catch-all（`path: '*'`，挂在 `AppLayout` 子树内）挂载，
 * 故 `fill={false}`——内容区自带高度，满视口会把侧栏挤出可视区。
 */
export function NotFoundPage() {
  const navigate = useNavigate()

  return (
    <StatusPage
      fill={false}
      status="404"
      title="页面不存在"
      subTitle="地址可能已经下线，或者你点开的是一个拼错的链接。"
      extra={
        <>
          <Button type="primary" onClick={() => void navigate(-1)}>
            返回上一页
          </Button>
          <Button onClick={() => void navigate('/')}>返回首页</Button>
        </>
      }
    />
  )
}
