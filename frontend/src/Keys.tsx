import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Account, KeyType, PixKey, api } from './api';
import { Err, when } from './ui';

const TYPES: KeyType[] = ['EVP', 'EMAIL', 'PHONE', 'CPF', 'CNPJ'];

export default function Keys({ account }: { account: Account }) {
  const [keys, setKeys] = useState<PixKey[]>([]);
  const [keyType, setKeyType] = useState<KeyType>('EVP');
  const [keyValue, setKeyValue] = useState('');
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(() => api.listKeys(account.id).then(setKeys).catch(setError), [account.id]);
  useEffect(() => { load(); }, [load]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    api.createKey({ accountId: account.id, keyType, keyValue: keyType === 'EVP' ? undefined : keyValue })
      .then(() => { setKeyValue(''); setError(null); return load(); })
      .catch(setError);
  };

  return (
    <>
      <h2>Chaves PIX</h2>
      <form className="row" onSubmit={submit}>
        <label>Tipo
          <select value={keyType} onChange={(e) => setKeyType(e.target.value as KeyType)}>
            {TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
        </label>
        <label>Valor
          <input disabled={keyType === 'EVP'} required={keyType !== 'EVP'} value={keyType === 'EVP' ? '' : keyValue}
            placeholder={keyType === 'EVP' ? 'Gerada automaticamente' : ''} onChange={(e) => setKeyValue(e.target.value)} />
        </label>
        <button className="primary">Criar chave</button>
      </form>
      <Err error={error} />
      {keys.length === 0 ? <p className="muted">Nenhuma chave ainda. Crie uma para receber transferências.</p> : (
        <ul className="list">
          {keys.map((k) => (
            <li key={k.id}>
              <span className="tag">{k.keyType}</span>
              <code>{k.keyValue}</code>
              <button className="ghost" onClick={() => navigator.clipboard.writeText(k.keyValue)}>Copiar</button>
              <span className="muted">{when(k.createdAt)}</span>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
