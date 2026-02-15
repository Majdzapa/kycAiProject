import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface FinancialTransaction {
    id?: string;
    customerId: string;
    amount: number;
    currency: string;
    type: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'CRYPTO_PURCHASE' | 'CRYPTO_SALE' | 'PAYMENT';
    sourceCountry: string;
    destinationCountry: string;
    timestamp?: string;
}

@Injectable({
    providedIn: 'root'
})
export class TransactionService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/transactions`;

    getCustomerTransactions(customerId: string): Observable<FinancialTransaction[]> {
        return this.http.get<FinancialTransaction[]>(`${this.apiUrl}/customer/${customerId}`);
    }

    logTransaction(transaction: FinancialTransaction): Observable<FinancialTransaction> {
        return this.http.post<FinancialTransaction>(this.apiUrl, transaction);
    }
}
