import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// >>> CHANGE THIS ONE LINE to your Render URL (keep /api at the end, no trailing slash) <<<
const API_BASE = 'https://smartbank-api-ywc7.onrender.com/api';

export interface Txn { type: string; amount: number; time: string; }

@Injectable({ providedIn: 'root' })
export class BankService {
  constructor(private http: HttpClient) {}

  register(user: string, pass: string): Observable<{ account: string }> {
    return this.http.get<{ account: string }>(`${API_BASE}/register?user=${user}&pass=${pass}`);
  }
  balance(acc: string): Observable<{ balance: number }> {
    return this.http.get<{ balance: number }>(`${API_BASE}/balance?acc=${acc}`);
  }
  deposit(acc: string, amount: number) {
    return this.http.get(`${API_BASE}/deposit?acc=${acc}&amount=${amount}`);
  }
  withdraw(acc: string, amount: number) {
    return this.http.get(`${API_BASE}/withdraw?acc=${acc}&amount=${amount}`);
  }
  history(acc: string): Observable<Txn[]> {
    return this.http.get<Txn[]>(`${API_BASE}/history?acc=${acc}`);
  }
}
