import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { KnowledgeBaseService } from '../../services/knowledge-base.service';

@Component({
    selector: 'app-knowledge-base-upload',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatCardModule,
        MatButtonModule,
        MatInputModule,
        MatSelectModule,
        MatProgressBarModule,
        MatIconModule,
        MatSnackBarModule
    ],
    templateUrl: './knowledge-base-upload.component.html',
    styleUrls: ['./knowledge-base-upload.component.css']
})
export class KnowledgeBaseUploadComponent {
    private fb = inject(FormBuilder);
    private kbaseService = inject(KnowledgeBaseService);
    private snackBar = inject(MatSnackBar);

    uploadForm = this.fb.group({
        title: ['', Validators.required],
        version: ['', Validators.required],
        category: ['REGULATION', Validators.required],
        file: [null as File | null, Validators.required]
    });

    selectedFile: File | null = null;
    isUploading = signal(false);
    uploadProgress = signal(0); // For future progress tracking

    categories = [
        { value: 'REGULATION', label: 'Regulation (Law)' },
        { value: 'PROCEDURE', label: 'Procedure (Internal)' },
        { value: 'GUIDELINE', label: 'Guideline (Best Practice)' },
        { value: 'FAQ', label: 'FAQ (Example Q&A)' },
        { value: 'POLICY', label: 'Policy (Governance)' }
    ];

    onFileSelected(event: any) {
        const file: File = event.target.files[0];
        if (file) {
            // Validate file type
            if (file.type !== 'application/pdf' && file.type !== 'text/plain') {
                this.snackBar.open('Invalid file type. Only PDF and TXT allowed.', 'Close', { duration: 3000 });
                return;
            }
            this.selectedFile = file;
            this.uploadForm.patchValue({ file: file });
            this.uploadForm.get('file')?.updateValueAndValidity();
        }
    }

    onSubmit() {
        if (this.uploadForm.valid && this.selectedFile) {
            this.isUploading.set(true);
            const formValue = this.uploadForm.value;

            this.kbaseService.ingestDocument(
                this.selectedFile,
                formValue.category!,
                formValue.title!,
                formValue.version!
            ).subscribe({
                next: (res) => {
                    this.isUploading.set(false);
                    this.snackBar.open('Document ingested successfully!', 'Close', { duration: 3000 });
                    this.uploadForm.reset({ category: 'REGULATION' });
                    this.selectedFile = null;
                },
                error: (err) => {
                    this.isUploading.set(false);
                    this.snackBar.open('Failed to ingest document: ' + (err.error?.message || err.message), 'Close', { duration: 5000 });
                }
            });
        }
    }
}
