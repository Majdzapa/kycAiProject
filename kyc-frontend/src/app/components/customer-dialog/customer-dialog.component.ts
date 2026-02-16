import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { Customer } from '../../models/customer.model';

@Component({
  selector: 'app-customer-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule
  ],
  template: `
    <h2 mat-dialog-title>{{data.customer ? 'Edit Customer' : 'Add New Customer'}}</h2>
    <mat-dialog-content>
      <form [formGroup]="customerForm" class="customer-form">
        <mat-form-field appearance="outline">
          <mat-label>Customer ID (e.g. CUS-123)</mat-label>
          <input matInput formControlName="id" [readonly]="!!data.customer">
          <mat-error *ngIf="customerForm.get('id')?.hasError('required')">ID is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Full Name</mat-label>
          <input matInput formControlName="fullName">
          <mat-error *ngIf="customerForm.get('fullName')?.hasError('required')">Name is required</mat-error>
        </mat-form-field>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Nationality</mat-label>
            <input matInput formControlName="nationality">
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Residence Country</mat-label>
            <input matInput formControlName="residenceCountry">
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline">
          <mat-label>Occupation</mat-label>
          <input matInput formControlName="occupation">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Industry Sector</mat-label>
          <input matInput formControlName="industrySector">
        </mat-form-field>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Income Range</mat-label>
            <mat-select formControlName="incomeRange">
              <mat-option value="0-50k">0 - 50k</mat-option>
              <mat-option value="50k-150k">50k - 150k</mat-option>
              <mat-option value="150k-500k">150k - 500k</mat-option>
              <mat-option value="500k+">500k+</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Entity Type</mat-label>
            <mat-select formControlName="entityType">
              <mat-option value="INDIVIDUAL">Individual</mat-option>
              <mat-option value="CORPORATION">Corporation</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="customerForm.invalid" (click)="onSave()">Save</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .customer-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 400px;
      padding-top: 10px;
    }
    .row {
      display: flex;
      gap: 16px;
    }
    .row mat-form-field {
      flex: 1;
    }
  `]
})
export class CustomerDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<CustomerDialogComponent>);
  public data = inject(MAT_DIALOG_DATA) as { customer?: Customer };

  customerForm: FormGroup;

  constructor() {
    const customer = this.data.customer;
    this.customerForm = this.fb.group({
      id: [customer?.id || '', Validators.required],
      fullName: [customer?.fullName || '', Validators.required],
      nationality: [customer?.nationality || '', Validators.required],
      residenceCountry: [customer?.residenceCountry || '', Validators.required],
      occupation: [customer?.occupation || ''],
      industrySector: [customer?.industrySector || ''],
      incomeRange: [customer?.incomeRange || ''],
      sourceOfWealth: [customer?.sourceOfWealth || ''],
      entityType: [customer?.entityType || 'INDIVIDUAL'],
      netWorth: [customer?.netWorth || 0],
      accountAge: [customer?.accountAge || 0],
      expectedMonthlyVolume: [customer?.expectedMonthlyVolume || 0]
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (this.customerForm.valid) {
      this.dialogRef.close(this.customerForm.value);
    }
  }
}