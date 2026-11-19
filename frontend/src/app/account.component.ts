import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { BankService, Txn } from './bank.service';

type Mode = 'DEPOSIT' | 'WITHDRAW';
type Filter = 'ALL' | 'DEPOSIT' | 'WITHDRAW';
interface Toast { kind: 'ok' | 'err'; text: string; }

const STORE_KEY = 'smartbank_account';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.css'],
})
export class AccountComponent implements OnInit, OnDestroy {
  acc = '';
  accInput = '';
  balance = 0;
  shownBalance = 0;
  history: Txn[] = [];

  mode: Mode = 'DEPOSIT';
  amount: number | null = null;
  filter: Filter = 'ALL';
  chips = [500, 1000, 5000, 10000];

  busy = false;
  loading = false;
  copied = false;
  toast: Toast | null = null;

  private raf = 0;
  private toastTimer: any;

  constructor(private bank: BankService) {}

  ngOnInit() {
    try {
      const saved = localStorage.getItem(STORE_KEY);
      if (saved) { this.acc = saved; this.refresh(); }
    } catch { /* storage unavailable, ignore */ }
  }

  ngOnDestroy() {
    cancelAnimationFrame(this.raf);
    clearTimeout(this.toastTimer);
  }

  // ---------- derived values ----------
  get prettyAcc(): string {
    return this.acc.length === 8 ? `${this.acc.slice(0, 2)} ${this.acc.slice(2, 5)} ${this.acc.slice(5)}` : this.acc;
  }
  get filtered(): Txn[] {
    return this.filter === 'ALL' ? this.history : this.history.filter(t => t.type === this.filter);
  }
  get totalIn(): number {
    return this.history.filter(t => t.type === 'DEPOSIT').reduce((s, t) => s + t.amount, 0);
  }
  get totalOut(): number {
    return this.history.filter(t => t.type === 'WITHDRAW').reduce((s, t) => s + t.amount, 0);
  }
  get canSubmit(): boolean {
    return !this.busy && this.amount !== null && this.amount > 0;
  }
  get submitLabel(): string {
    if (this.busy) { return 'Working...'; }
    const verb = this.mode === 'DEPOSIT' ? 'Deposit' : 'Withdraw';
    return this.amount && this.amount > 0
      ? `${verb} ₹${new Intl.NumberFormat('en-IN').format(this.amount)}`
      : verb;
  }

  when(t: Txn): Date {
    // SQLite stores UTC as "YYYY-MM-DD HH:MM:SS"
    return new Date(t.time.replace(' ', 'T') + 'Z');
  }

  // ---------- actions ----------
  openAccount() {
    this.busy = true;
    this.bank.register('user' + Date.now(), 'demo123').subscribe({
      next: r => {
        this.busy = false;
        this.setAccount(r.account);
        this.balance = 0;
        this.shownBalance = 0;
        this.history = [];
        this.showToast('ok', 'Account opened. Make your first deposit.');
      },
      error: e => this.fail(e),
    });
  }

  useExisting() {
    const value = this.accInput.trim().toUpperCase();
    if (!value) { return; }
    this.busy = true;
    this.acc = value;
    this.refresh(true);
  }

  switchAccount() {
    this.clearAccount();
    this.history = [];
    this.balance = 0;
    this.shownBalance = 0;
  }

  setMode(m: Mode) { this.mode = m; }

  submit() {
    if (!this.canSubmit) { return; }
    const amt = this.amount as number;
    if (this.mode === 'WITHDRAW' && amt > this.balance) {
      this.showToast('err', 'That is more than your available balance.');
      return;
    }
    this.busy = true;
    const call = this.mode === 'DEPOSIT'
      ? this.bank.deposit(this.acc, amt)
      : this.bank.withdraw(this.acc, amt);
    const done = this.mode === 'DEPOSIT' ? 'Deposited' : 'Withdrew';
    call.subscribe({
      next: () => {
        this.amount = null;
        this.showToast('ok', `${done} ₹${new Intl.NumberFormat('en-IN').format(amt)}`);
        this.refresh();
      },
      error: e => this.fail(e),
    });
  }

  copy() {
    navigator.clipboard?.writeText(this.acc).then(() => {
      this.copied = true;
      setTimeout(() => (this.copied = false), 1500);
    });
  }

  // ---------- helpers ----------
  private refresh(initial = false) {
    this.loading = true;
    forkJoin({ b: this.bank.balance(this.acc), h: this.bank.history(this.acc) }).subscribe({
      next: ({ b, h }) => {
        this.loading = false;
        this.busy = false;
        this.balance = b.balance;
        this.history = h;
        if (initial) { this.setAccount(this.acc); }
        this.animateTo(b.balance);
      },
      error: e => {
        this.loading = false;
        this.busy = false;
        if (e?.error?.error === 'Account not found') { this.clearAccount(); }
        this.fail(e);
      },
    });
  }

  private setAccount(acc: string) {
    this.acc = acc;
    this.accInput = '';
    try { localStorage.setItem(STORE_KEY, acc); } catch { /* ignore */ }
  }

  private clearAccount() {
    this.acc = '';
    try { localStorage.removeItem(STORE_KEY); } catch { /* ignore */ }
  }

  private fail(e: any) {
    this.busy = false;
    const msg = e?.error?.error;
    this.showToast('err', msg === 'Account not found'
      ? 'That account was not found. Open a new one to continue.'
      : msg ?? 'Could not reach the server. It may be waking up, so try again in 30 seconds.');
  }

  private showToast(kind: 'ok' | 'err', text: string) {
    clearTimeout(this.toastTimer);
    this.toast = { kind, text };
    this.toastTimer = setTimeout(() => (this.toast = null), 3500);
  }

  private animateTo(target: number) {
    cancelAnimationFrame(this.raf);
    const from = this.shownBalance;
    if (from === target || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.shownBalance = target;
      return;
    }
    const start = performance.now();
    const duration = 700;
    const tick = (now: number) => {
      const p = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - p, 3);
      this.shownBalance = from + (target - from) * eased;
      if (p < 1) { this.raf = requestAnimationFrame(tick); } else { this.shownBalance = target; }
    };
    this.raf = requestAnimationFrame(tick);
  }
}
