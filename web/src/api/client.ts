/** An RFC 9457 problem returned by the API. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly reasons: string[]
  readonly fieldErrors: { field: string; message: string }[]

  constructor(status: number, body: Record<string, unknown> | null) {
    super((body?.detail as string | undefined) ?? `Error ${status}`)
    this.status = status
    this.code = (body?.code as string | undefined) ?? 'UNKNOWN'
    this.reasons = (body?.reasons as string[] | undefined) ?? []
    this.fieldErrors = (body?.errors as { field: string; message: string }[] | undefined) ?? []
  }
}

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  const response = await fetch(url, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    let problem: Record<string, unknown> | null = null
    try {
      problem = await response.json()
    } catch {
      // Not JSON: keep the status only.
    }
    throw new ApiError(response.status, problem)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export const api = {
  get: <T>(url: string) => request<T>('GET', url),
  post: <T>(url: string, body?: unknown) => request<T>('POST', url, body ?? {}),
  put: <T>(url: string, body: unknown) => request<T>('PUT', url, body),
  patch: <T>(url: string, body: unknown) => request<T>('PATCH', url, body),
  delete: (url: string) => request<void>('DELETE', url),
}
