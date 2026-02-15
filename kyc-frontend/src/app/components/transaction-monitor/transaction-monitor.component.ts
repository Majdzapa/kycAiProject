import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TransactionService, FinancialTransaction } from '../../services/transaction.service';

@Component({
    selector: 'app-transaction-monitor',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatCardModule,
        MatButtonModule,
        MatInputModule,
        MatSelectModule,
        MatTableModule,
        MatIconModule,
        MatSnackBarModule
    ],
    templateUrl: './transaction-monitor.component.html',
    styleUrls: ['./transaction-monitor.component.css']
})
export class TransactionMonitorComponent {
    private fb = inject(FormBuilder);
    private transactionService = inject(TransactionService);
    private snackBar = inject(MatSnackBar);

    transactions = signal<FinancialTransaction[]>([]);
    displayedColumns = ['timestamp', 'type', 'amount', 'currency', 'source', 'destination'];

    searchForm = this.fb.group({
        customerId: ['', Validators.required]
    });

    transactionForm = this.fb.group({
        customerId: ['', Validators.required],
        amount: [1000, [Validators.required, Validators.min(1)]],
        currency: ['USD', Validators.required],
        type: ['TRANSFER', Validators.required],
        sourceCountry: ['US', Validators.required],
        destinationCountry: ['US', Validators.required]
    });

    transactionTypes = [
        'DEPOSIT', 'WITHDRAWAL', 'TRANSFER',
        'CRYPTO_PURCHASE', 'CRYPTO_SALE', 'PAYMENT'
    ];

    loadTransactions() {
        if (this.searchForm.valid) {
            const customerId = this.searchForm.value.customerId!;
            this.transactionService.getCustomerTransactions(customerId).subscribe({
                next: (data) => this.transactions.set(data),
                error: (err) => this.snackBar.open('Error loading transactions', 'Close', { duration: 3000 })
            });
        }
    }

    logTransaction() {
        if (this.transactionForm.valid) {
            const tx = this.transactionForm.value as any;

            this.transactionService.logTransaction(tx).subscribe({
                next: (created) => {
                    this.snackBar.open('Transaction logged', 'Close', { duration: 3000 });
                    // If viewing the same customer, refresh list
                    if (this.searchForm.value.customerId === created.customerId) {
                        this.transactions.update(list => [created, ...list]);
                    }
                },
                error: (err) => this.snackBar.open('Failed to log transaction', 'Close', { duration: 3000 })
            });
        }
    }
}
