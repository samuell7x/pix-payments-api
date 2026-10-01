export type KeyType = 'CPF' | 'CNPJ' | 'EMAIL' | 'PHONE' | 'EVP';
export type Account = { id: string; ownerName: string; ownerDocument: string; balance: number; createdAt: string };
export type PixKey = { id: string; keyType: KeyType; keyValue: string; accountId: string; createdAt: string };
export type Transfer = {
  transactionId: string; status: 'COMPLETED' | 'FAILED'; failureReason: string | null;
  sourceAccountId: string; targetAccountId: string; targetPixKey: string;
  amount: number; description: string | null; createdAt: string; completedAt: string | null;
};
export type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number; last: boolean };

export class ApiError extends Error {
  constructor(public status: number, message: string, public details: string[] = []) { super(message); }
}

async function req<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch('/api/v1' + path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init.headers },
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new ApiError(res.status, data?.message ?? 'Erro inesperado', data?.details ?? []);
  return data as T;
}
const post = <T,>(path: string, body: unknown, headers: Record<string, string> = {}) =>
  req<T>(path, { method: 'POST', body: JSON.stringify(body), headers });

export const api = {
  createAccount: (b: { ownerName: string; ownerDocument: string; initialBalance: number }) => post<Account>('/accounts', b),
  getAccount: (id: string) => req<Account>(`/accounts/${id}`),
  createKey: (b: { accountId: string; keyType: KeyType; keyValue?: string }) => post<PixKey>('/pix-keys', b),
  listKeys: (accountId: string) => req<PixKey[]>(`/pix-keys/accounts/${accountId}`),
  transfer: (b: { sourceAccountId: string; targetPixKey: string; amount: number; description?: string }, idempotencyKey: string) =>
    post<Transfer>('/pix/transfers', b, { 'Idempotency-Key': idempotencyKey }),
  statement: (accountId: string, p: { direction?: string; page: number; size: number }) => {
    const q = new URLSearchParams({ page: String(p.page), size: String(p.size) });
    if (p.direction) q.set('direction', p.direction);
    return req<Page<Transfer>>(`/accounts/${accountId}/statement?${q}`);
  },
};
