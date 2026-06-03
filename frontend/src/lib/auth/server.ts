import { cookies } from 'next/headers'
import { TOKEN_COOKIE } from './session'

/** Read the auth token from cookies in a Server Component / layout. */
export async function getServerToken(): Promise<string | null> {
  const store = await cookies()
  return store.get(TOKEN_COOKIE)?.value ?? null
}

/** True when a non-empty token cookie is present (middleware enforces expiry). */
export async function isAuthenticated(): Promise<boolean> {
  return (await getServerToken()) !== null
}
