// 登录凭证的本地存取
// 只碰 localStorage，不做任何网络请求，也不依赖 vue

const TOKEN_KEY = 'market_token'
const USER_KEY = 'market_user'

/** 取 token，未登录返回 null */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/** 存 token */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 取用户信息（不含 token），未登录或解析失败返回 null */
export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    // 存进去的不是合法 JSON（被人手动改过），当成没登录
    return null
  }
}

/** 存用户信息（只放 id/username/nickname/avatar/role，不放 token） */
export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/** 清空登录状态（退出登录 / token 失效时用） */
export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}