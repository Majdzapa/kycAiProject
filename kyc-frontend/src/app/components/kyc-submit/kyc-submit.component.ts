import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { AuthStateService } from '../../services/auth-state.service';
import { KycStateService } from '../../services/kyc-state.service';
import { KycService } from '../../services/kyc.service';
import { DocumentType } from '../../models/kyc.model';

@Component({
  selector: 'app-kyc-submit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatCheckboxModule
  ],
  templateUrl: './kyc-submit.component.html',
  styleUrls: ['./kyc-submit.component.css']
})
export class KycSubmitComponent {
  private fb = inject(FormBuilder);
  private kycService = inject(KycService);
  authState = inject(AuthStateService);
  kycState = inject(KycStateService);
  private snackBar = inject(MatSnackBar);

  submitForm: FormGroup;
  selectedFile: File | null = null;
  isDragging = false;

  documentTypes = [
    { value: DocumentType.ID_CARD, label: 'National ID Card', icon: 'badge' },
    { value: DocumentType.PASSPORT, label: 'Passport', icon: 'travel_explore' },
    { value: DocumentType.DRIVERS_LICENSE, label: "Driver's License", icon: 'directions_car' },
    { value: DocumentType.PROOF_OF_ADDRESS, label: 'Proof of Address', icon: 'home' },
    { value: DocumentType.UTILITY_BILL, label: 'Utility Bill', icon: 'receipt' },
    { value: DocumentType.BANK_STATEMENT, label: 'Bank Statement', icon: 'account_balance' }
  ];

  constructor() {
    this.submitForm = this.fb.group({
      docType: ['', Validators.required],
      file: [null, Validators.required],
      consent: [false, Validators.requiredTrue]
    });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  handleFile(file: File): void {
    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf'];
    if (!allowedTypes.includes(file.type)) {
      this.snackBar.open('Invalid file type. Please upload JPG, PNG, or PDF.', 'Close', { duration: 5000 });
      return;
    }

    // Validate file size (10MB)
    if (file.size > 10 * 1024 * 1024) {
      this.snackBar.open('File too large. Maximum size is 10MB.', 'Close', { duration: 5000 });
      return;
    }

    this.selectedFile = file;
    this.submitForm.patchValue({ file: file });
  }

  onSubmit(): void {
    this.submitForm.markAllAsTouched();

    if (this.submitForm.invalid || !this.selectedFile) {
      return;
    }

    const user = this.authState.currentUser();

    if (!user?.customerId) {
      this.snackBar.open('Customer ID not found. Please relogin.', 'Close', { duration: 4000 });
      return;
    }

    this.kycService.submitDocument(
      user.customerId,
      this.selectedFile,
      this.submitForm.value.docType
    ).subscribe({
      next: (response) => {
        if (response.kycStatus?.documentStatus === 'REJECTED') {
          this.snackBar.open('Document rejected. Please check findings and try again.', 'Close', { duration: 5000 });
        } else {
          this.snackBar.open('Document submitted successfully!', 'Close', { duration: 3000 });
          this.resetForm();
        }
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Submission failed',
          'Close',
          { duration: 5000 }
        );
      }
    });
  }

  resetForm(): void {
    this.submitForm.reset({
      docType: '',
      file: null,
      consent: false
    });
    this.selectedFile = null;
    this.kycState.clearError();
  }

  retry(): void {
    this.kycState.setCurrentKycStatus(null);
    this.selectedFile = null;
    this.submitForm.patchValue({ file: null });
  }
}
