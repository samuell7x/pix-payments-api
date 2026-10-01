import { useEffect, useState } from 'react';
import { Account, Page, Transfer, api } from './api';
import { Err, money, when } from './ui';

export default function Statement({ account }: { account: Account }) {
  const [direction, setDirection] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Transfer> | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api.statement(account.id, { direction: direction || undefined, page, size: 10 })
      .then((d) => { setData(d); setError(null); }).catch(setError);
  }, [account.id, direction, page]);

  return (
    <>
      <h2>Extrato</h2>
      <label>Mostrar
        <select value={direction} onChange={(e) => { setDirection(e.target.value); setPage(0); }}>
          <option value="">Tudo</option><option value="OUT">Enviados</option><option value="IN">Recebidos</option>
        </select>
      </label>
      <Err error={error} />
      {data && data.content.length === 0 && <p className="muted">Nenhuma transação encontrada.</p>}
      {data && data.content.length > 0 && (
        <div className="scroll">
          <table>
            <thead><tr><th>Data</th><th>Tipo</th><th>Chave</th><th>Status</th><th className="num">Valor</th></tr></thead>
            <tbody>
              {data.content.map((t) => {
                const out = t.sourceAccountId === account.id;
                return (
                  <tr key={t.transactionId}>
                    <td>{when(t.createdAt)}</td>
                    <td>{out ? 'Enviado' : 'Recebido'}</td>
                    <td><code>{t.targetPixKey}</code></td>
                    <td>{t.status === 'COMPLETED' ? 'Concluído' : 'Recusado'}</td>
                    <td className={`num ${out ? 'neg' : 'pos'}`}>{out ? '−' : '+'}{money(t.amount)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      {data && data.totalPages > 1 && (
        <div className="row">
          <button className="ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</button>
          <span className="muted">Página {data.page + 1} de {data.totalPages}</span>
          <button className="ghost" disabled={data.last} onClick={() => setPage(page + 1)}>Próxima</button>
        </div>
      )}
    </>
  );
}
