import { ApiError } from './api';

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const money = (v: number) => brl.format(v);
export const when = (iso: string) => new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export function Err({ error }: { error: unknown }) {
  if (!error) return null;
  const e = error instanceof ApiError ? error : new ApiError(0, 'Não foi possível falar com a API. Confira se ela está no ar.');
  return (
    <div className="err" role="alert">
      <strong>{e.message}</strong>
      {e.details.length > 0 && <ul>{e.details.map((d) => <li key={d}>{d}</li>)}</ul>}
    </div>
  );
}
