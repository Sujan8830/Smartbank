import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BankService, Txn } from './bank.service';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2>SmartBank</h2>
    <button (click)="newAccount()">Create new account</button>
    <div>
      <input [(ngModel)]="acc" placeholder="Account no (e.g. SB123456)" />
      <input type="number" [(ngModel)]="amount" placeholder="Amount" />
    </div>
    <div>
      <button (click)="load()">Balance + History</button>
      <button (click)="deposit()">Deposit</button>
      <button (click)="withdraw()">Withdraw</button>
    </div>
    <p *ngIf="loading">Loading... (the server may take ~30s to wake up)</p>
    <p *ngIf="balance !== null"><b>Balance:</b> {{ balance | number:'1.2-2' }}</p>
    <p *ngIf="error" style="color:red">{{ error }}</p>
    <table *ngIf="history.length">
      <tr><th>Time</th><th>Type</th><th>Amount</th></tr>
      <tr *ngFor="let t of history">
        <td>{{ t.time }}</td><td>{{ t.type }}</td><td>{{ t.amount }}</td>
      </tr>
    </table>
  `,
})
export class AccountComponent {
  acc = '';
  amount = 0;
  balance: number | null = null;
  history: Txn[] = [];
  error = '';
  loading = false;

  constructor(private bank: BankService) {}

  private fail = (e: any) => { this.loading = false; this.error = e?.error?.error ?? 'Request failed'; };

  newAccount() {
    this.error = ''; this.loading = true;
    this.bank.register('user' + Date.now(), 'demo123').subscribe({
      next: r => { this.loading = false; this.acc = r.account; this.balance = null; this.history = []; },
      error: this.fail,
    });
  }
  load() {
    this.error = ''; this.loading = true;
    this.bank.balance(this.acc).subscribe({
      next: r => { this.loading = false; this.balance = r.balance; },
      error: this.fail,
    });
    this.bank.history(this.acc).subscribe({ next: h => (this.history = h), error: this.fail });
  }
  deposit()  { this.bank.deposit(this.acc, this.amount).subscribe({ next: () => this.load(), error: this.fail }); }
  withdraw() { this.bank.withdraw(this.acc, this.amount).subscribe({ next: () => this.load(), error: this.fail }); }
}
