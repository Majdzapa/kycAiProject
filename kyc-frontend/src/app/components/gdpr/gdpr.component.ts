import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

import { AuthStateService } from '../../services/auth-state.service';
import { GdprService } from '../../services/gdpr.service';

@Component({
  selector: 'app-gdpr',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './gdpr.component.html',
  styleUrls: ['./gdpr.component.css']
})
export class GdprComponent {
  private gdprService = inject(GdprService);
  private authState = inject(AuthStateService);
  private snackBar = inject(MatSnackBar);

  exportData(): void {
    const user = this.authState.currentUser();
    if (user?.customerId) {
      this.gdprService.exportData(user.customerId).subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `gdpr-export-${user.customerId}.json`;
          a.click();
          window.URL.revokeObjectURL(url);
          this.snackBar.open('Data export downloaded successfully', 'Close', { duration: 3000 });
        },
        error: () => {
          this.snackBar.open('Failed to export data', 'Close', { duration: 5000 });
        }
      });
    }
  }

  deleteData(): void {
    if (confirm('Are you sure you want to delete all your personal data? This action cannot be undone.')) {
      const user = this.authState.currentUser();
      if (user?.customerId) {
        this.gdprService.deleteData(user.customerId).subscribe({
          next: () => {
            this.snackBar.open('Data deletion request submitted', 'Close', { duration: 3000 });
          },
          error: () => {
            this.snackBar.open('Failed to submit deletion request', 'Close', { duration: 5000 });
          }
        });
      }
    }
  }

  withdrawConsent(): void {
    this.snackBar.open('Consent management coming soon', 'Close', { duration: 3000 });
  }
}
