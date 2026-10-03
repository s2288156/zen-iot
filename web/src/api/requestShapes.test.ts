import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { changePassword } from './auth'
import { assignRoleModules, createRole, deleteRole, updateRole } from './roles'
import { kickoutSession } from './sessions'
import { assignUserRoles, changeUserStatus, createUser, deleteUser, resetUserPassword, updateUser } from './users'

interface Call {
  method: string
  url: string
  /** `undefined` 表示没有请求体（GET / DELETE）；空集合会被序列化成 `'[]'`，不能退化成 `undefined` */
  body: string | undefined
}

const calls: Call[] = []

/** 一律回成功信封：这组用例只断言「发出去的东西对不对」，响应处理已在 `http.test.ts` 覆盖 */
function mockFetch(): void {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown, init?: unknown) => {
      const options = init as { method?: string; body?: string } | undefined
      calls.push({ method: options?.method ?? 'GET', url: String(input), body: options?.body })
      return { status: 200, ok: true, text: () => Promise.resolve(JSON.stringify({ code: 200, message: '成功' })) }
    }),
  )
}

function sent(): Call {
  expect(calls).toHaveLength(1)
  return calls[0]
}

beforeEach(() => {
  calls.length = 0
  mockFetch()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('集合型写接口的请求体是裸数组', () => {
  // 后端签名是 `@RequestBody Set<Long>` / `Set<String>`，包一层 `{ roleIds: [...] }` 会被 Jackson 拒成 400
  it('分配角色：`PUT /users/{id}/roles` 发 `[1,3]`', async () => {
    await assignUserRoles(7, [1, 3])
    expect(sent()).toEqual({ method: 'PUT', url: '/api/admin/users/7/roles', body: '[1,3]' })
  })

  it('分配角色：清空也要真的发出 `[]`', async () => {
    await assignUserRoles(7, [])
    expect(sent()).toEqual({ method: 'PUT', url: '/api/admin/users/7/roles', body: '[]' })
  })

  it('模块授权：`PUT /roles/{id}/modules` 发 `["admin","wcs"]`', async () => {
    await assignRoleModules(2, ['admin', 'wcs'])
    expect(sent()).toEqual({ method: 'PUT', url: '/api/admin/roles/2/modules', body: '["admin","wcs"]' })
  })

  it('模块授权：收回全部模块发 `[]`', async () => {
    await assignRoleModules(2, [])
    expect(sent()).toEqual({ method: 'PUT', url: '/api/admin/roles/2/modules', body: '[]' })
  })
})

describe('对象型写接口的字段名逐字对齐后端 DTO', () => {
  it('创建用户带上全部六个字段，选填项用 null 而不是省略', async () => {
    await createUser({
      username: 'ops',
      password: 'p@ssw0rd!',
      nickname: null,
      email: null,
      phone: null,
      avatar: null,
    })
    expect(sent()).toEqual({
      method: 'POST',
      url: '/api/admin/users',
      body: '{"username":"ops","password":"p@ssw0rd!","nickname":null,"email":null,"phone":null,"avatar":null}',
    })
  })

  it('编辑用户不含 username（创建后不可改）', async () => {
    await updateUser(7, { nickname: '运维', email: null, phone: null, avatar: null })
    expect(sent()).toEqual({
      method: 'PUT',
      url: '/api/admin/users/7',
      body: '{"nickname":"运维","email":null,"phone":null,"avatar":null}',
    })
  })

  it('启停用 PATCH 且只带 status', async () => {
    await changeUserStatus(7, { status: 0 })
    expect(sent()).toEqual({ method: 'PATCH', url: '/api/admin/users/7/status', body: '{"status":0}' })
  })

  it('重置口令走 PUT /users/{id}/password', async () => {
    await resetUserPassword(7, { password: 'p@ssw0rd!' })
    expect(sent()).toEqual({ method: 'PUT', url: '/api/admin/users/7/password', body: '{"password":"p@ssw0rd!"}' })
  })

  it('创建角色带 modules，编辑角色不带（modules 走独立接口）', async () => {
    await createRole({ roleCode: 'ops', roleName: '运维', description: null, modules: ['admin'] })
    expect(sent()).toEqual({
      method: 'POST',
      url: '/api/admin/roles',
      body: '{"roleCode":"ops","roleName":"运维","description":null,"modules":["admin"]}',
    })

    calls.length = 0
    await updateRole(2, { roleName: '运维二组', description: null })
    expect(sent()).toEqual({
      method: 'PUT',
      url: '/api/admin/roles/2',
      body: '{"roleName":"运维二组","description":null}',
    })
  })

  it('自助改密同时带旧口令与新口令', async () => {
    await changePassword('old-pass', 'new-pass')
    expect(sent()).toEqual({
      method: 'POST',
      url: '/api/admin/auth/change-password',
      body: '{"oldPassword":"old-pass","newPassword":"new-pass"}',
    })
  })
})

describe('无请求体的写接口', () => {
  it('删除用户不发 body', async () => {
    await deleteUser(7)
    expect(sent()).toEqual({ method: 'DELETE', url: '/api/admin/users/7', body: undefined })
  })

  it('删除角色不发 body', async () => {
    await deleteRole(2)
    expect(sent()).toEqual({ method: 'DELETE', url: '/api/admin/roles/2', body: undefined })
  })

  // 路径参数是 sessionId（UUID），不是数值 ID
  it('强制下线按 sessionId 走 DELETE /sessions/{sessionId}', async () => {
    await kickoutSession('9f1c0a7e-2b6d-4c11-9a5e-0d3f7b2c8a41')
    expect(sent()).toEqual({
      method: 'DELETE',
      url: '/api/admin/sessions/9f1c0a7e-2b6d-4c11-9a5e-0d3f7b2c8a41',
      body: undefined,
    })
  })
})
