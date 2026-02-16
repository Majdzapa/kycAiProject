import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { CustomerService } from '../../services/customer.service';
import { CustomerFullProfile } from '../../models/customer.model';

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatTabsModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatChipsModule
  ],
  templateUrl: './customer-detail.component.html',
  styleUrls: ['./customer-detail.component.css']
})
export class CustomerDetailComponent implements OnInit {
  route = inject(ActivatedRoute);
  customerService = inject(CustomerService);
  profile: CustomerFullProfile | null = null;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.customerService.getCustomerFullProfile(id).subscribe(
        res => this.profile = res
      );
    }
  }

  getRiskColor(risk: string): string {
    switch (risk) {
      case 'LOW': return 'primary';
      case 'MEDIUM': return 'accent';
      case 'HIGH':
      case 'CRITICAL': return 'warn';
      default: return '';
    }
  }

  isCredit(tx: any): boolean {
    return tx.type === 'DEPOSIT' || tx.type === 'CRYPTO_SALE' || tx.type === 'CASH_DEPOSIT';
  }
}
