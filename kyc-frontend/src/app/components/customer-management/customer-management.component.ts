import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CustomerService } from '../../services/customer.service';
import { Customer } from '../../models/customer.model';
import { CustomerDialogComponent } from '../customer-dialog/customer-dialog.component';

@Component({
  selector: 'app-customer-management',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule
  ],
  templateUrl: './customer-management.component.html',
  styleUrls: ['./customer-management.component.css']
})
export class CustomerManagementComponent implements OnInit {
  customerService = inject(CustomerService);
  dialog = inject(MatDialog);
  displayedColumns = ['fullName', 'nationality', 'residence', 'occupation', 'actions'];

  ngOnInit() {
    this.customerService.loadCustomers().subscribe();
  }

  deleteCustomer(id: string) {
    if (confirm('Are you sure you want to delete this customer?')) {
      this.customerService.deleteCustomer(id).subscribe();
    }
  }

  openCustomerDialog(customer?: Customer) {
    const dialogRef = this.dialog.open(CustomerDialogComponent, {
      width: '500px',
      data: { customer }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        if (customer && customer.id) {
          this.customerService.updateCustomer(customer.id, result).subscribe();
        } else {
          this.customerService.createCustomer(result).subscribe();
        }
      }
    });
  }
}
