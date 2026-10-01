import { FormEvent, useState } from 'react';
import { Account, Transfer as Tx, api } from './api';
import { Err, money } from './ui';

export default function Transfer({ account, onDone }: { account: Account; onDone: () => void }) {
  const [targetPixKey, setTarget] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  // Uma chave por intenção de pagamento: reenviar com a mesma chave nunca debita duas vezes.
  const [idemKey, setIdemKey] = useState(() => crypto.randomUUID());
  const [result, setResult] = useState<Tx | null>(null);
  const [error, setError] = useState<unknown>(null);

  const send = () =>
    api.transfer({ sourceAccountId: account.id, targetPixKey: targetPixKey.trim(), amount: Number(amount), description: description || undefined }, idemKey)
      .then((r) => { setResult(r); setError(null); onDone(); })
      .catch((e) => { setResult(null); setError(e); });

  const submit = (e: FormEvent) => { e.preventDefault(); send(); };
  const reset = () => { setIdemKey(crypto.randomUUID()); setResult(null); setTarget(''); setAmount(''); setDescription(''); };

  return (
    <>
      <h2>Transferir por PIX</h2>
      <form onSubmit={submit}>
        <label>Chave do destinatário<input required value={targetPixKey} onChange={(e) => setTarget(e.target.value)} /></label>
        <label>Valor (R$)<input required type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} /></label>
        <label>Descrição (opcional)<input maxLength={280} value={description} onChange={(e) => setDescription(e.target.value)} /></label>
        <p className="muted">Chave de idempotência: <code>{idemKey}</code></p>
        <button className="primary">Enviar PIX</button>
      </form>
      <Err error={error} />
      {result && (
        <div className={result.status === 'COMPLETED' ? 'ok' : 'err'}>
          <strong>{result.status === 'COMPLETED' ? `${money(result.amount)} enviado` : `Transferência recusada: ${result.failureReason ?? 'motivo não informado'}`}</strong>
          <p className="muted">Transação {result.transactionId}</p>
          <div className="row">
            <button className="ghost" onClick={send}>Reenviar com a mesma chave</button>
            <button className="ghost" onClick={reset}>Nova transferência</button>
          </div>
          <p className="muted">Reenviar devolve a mesma transação e o saldo não muda.</p>
        </div>
      )}
    </>
  );
}
