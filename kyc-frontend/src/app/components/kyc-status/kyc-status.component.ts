import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';

import { AuthStateService } from '../../services/auth-state.service';
import { KycStateService } from '../../services/kyc-state.service';
import { KycService } from '../../services/kyc.service';

@Component({
  selector: 'app-kyc-status',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatIconModule,
    MatProgressBarModule,
    MatButtonModule
  ],
  templateUrl: './kyc-status.component.html',
  styleUrls: ['./kyc-status.component.css']
})
export class KycStatusComponent implements OnInit {
  authState = inject(AuthStateService);
  kycState = inject(KycStateService);
  private kycService = inject(KycService);

  displayedColumns = ['id', 'status', 'risk', 'confidence', 'date'];

  ngOnInit(): void {
    const user = this.authState.currentUser();
    if (user?.customerId) {
      this.kycService.getKycStatus(user.customerId).subscribe();
    }
  }

  getRiskClass(riskLevel?: string): string {
    if (!riskLevel) return 'risk-low';
    switch (riskLevel.toUpperCase()) {
      case 'LOW': return 'risk-low';
      case 'MEDIUM': return 'risk-medium';
      case 'HIGH': return 'risk-high';
      case 'CRITICAL': return 'risk-critical';
      default: return 'risk-low';
    }
  }

  getConfidenceClass(score?: number): string {
    if (!score) return 'conf-low';
    if (score > 0.9) return 'conf-high';
    if (score > 0.7) return 'conf-medium';
    return 'conf-low';
  }
}
