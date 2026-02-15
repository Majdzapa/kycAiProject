import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';

import { AuthStateService } from '../../services/auth-state.service';
import { KycStateService } from '../../services/kyc-state.service';
import { KycService } from '../../services/kyc.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule,
    MatDividerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  authState = inject(AuthStateService);
  kycState = inject(KycStateService);
  private kycService = inject(KycService);

  ngOnInit(): void {
    const user = this.authState.currentUser();
    if (user?.customerId) {
      this.kycService.getKycStatus(user.customerId).subscribe();
    }
  }

  getRiskLevel(level?: string): string {
    if (!level) return 'low';
    return level.toLowerCase();
  }

  getConfidenceClass(score?: number): string {
    if (!score) return 'conf-low';
    if (score > 0.9) return 'conf-high';
    if (score > 0.7) return 'conf-medium';
    return 'conf-low';
  }
}
