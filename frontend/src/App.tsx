import { FormEvent, useEffect, useState } from 'react';
import { Account, api } from './api';
import { Err, money } from './ui';
import Keys from './Keys';
import Transfer from './Transfer';
import Statement from './Statement';

const STORAGE_KEY = 'pix.accountId';
type Tab = 'transfer' | 'keys' | 'statement';
const TABS: [Tab, string][] = [['transfer', 'Transferir'], ['keys', 'Chaves PIX'], ['statement', 'Extrato']];

export default function App() {
  const [account, setAccount] = useState<Account | null>(null);
  const [tab, setTab] = useState<Tab>('transfer');
  const [error, setError] = useState<unknown>(null);
  const [form, setForm] = useState({ ownerName: '', ownerDocument: '', initialBalance: '1000' });
  const [openId, setOpenId] = useState('');

  const enter = (a: Account) => { localStorage.setItem(STORAGE_KEY, a.id); setAccount(a); setError(null); };
  const refresh = () => account && api.getAccount(account.id).then(setAccount).catch(setError);

  useEffect(() => {
    const id = localStorage.getItem(STORAGE_KEY);
    if (id) api.getAccount(id).then(setAccount).catch(() => localStorage.removeItem(STORAGE_KEY));
  }, []);

  const create = (e: FormEvent) => {
    e.preventDefault();
    api.createAccount({ ...form, initialBalance: Number(form.initialBalance) }).then(enter).catch(setError);
  };
  const open = (e: FormEvent) => { e.preventDefault(); api.getAccount(openId.trim()).then(enter).catch(setError); };
  const leave = () => { localStorage.removeItem(STORAGE_KEY); setAccount(null); };

  if (!account) {
    return (
      <main className="shell">
        <h1 className="brand">PIX Payments</h1>
        <p className="lead">Simulador de um provedor de pagamentos. Crie uma conta para começar a transferir.</p>
        <Err error={error} />
        <div className="grid2">
          <form className="panel" onSubmit={create}>
            <h2>Criar conta</h2>
            <label>Nome do titular<input required maxLength={150} value={form.ownerName} onChange={(e) => setForm({ ...form, ownerName: e.target.value })} /></label>
            <label>CPF ou CNPJ (só números)<input required inputMode="numeric" pattern="\d{11}|\d{14}" value={form.ownerDocument} onChange={(e) => setForm({ ...form, ownerDocument: e.target.value })} /></label>
            <label>Saldo inicial (R$)<input required type="number" min="0" step="0.01" value={form.initialBalance} onChange={(e) => setForm({ ...form, initialBalance: e.target.value })} /></label>
            <button className="primary">Criar conta</button>
          </form>
          <form className="panel" onSubmit={open}>
            <h2>Abrir conta existente</h2>
            <label>ID da conta<input required value={openId} onChange={(e) => setOpenId(e.target.value)} placeholder="e664e1b4-bb1a-..." /></label>
            <button className="primary">Abrir conta</button>
          </form>
        </div>
      </main>
    );
  }

  return (
    <main className="shell">
      <header className="balance">
        <div>
          <p className="who">{account.ownerName}</p>
          <p className="muted">ID {account.id}</p>
        </div>
        <div className="amount">{money(account.balance)}</div>
        <button className="ghost" onClick={leave}>Trocar de conta</button>
      </header>
      <nav className="tabs">
        {TABS.map(([id, label]) => (
          <button key={id} className={tab === id ? 'on' : ''} onClick={() => setTab(id)}>{label}</button>
        ))}
      </nav>
      <section className="panel">
        {tab === 'transfer' && <Transfer account={account} onDone={refresh} />}
        {tab === 'keys' && <Keys account={account} />}
        {tab === 'statement' && <Statement account={account} />}
      </section>
    </main>
  );
}
