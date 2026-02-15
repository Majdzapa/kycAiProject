import { Component, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subject, takeUntil, filter } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AuthService } from '../../services/auth.service';
import { AuthStateService } from '../../services/auth-state.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnDestroy {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  authState = inject(AuthStateService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private destroy$ = new Subject<void>();

  loginForm: FormGroup;
  hidePassword = true;

  constructor() {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    // Show error in snackbar
    this.authState.authState().error;

    // Watch for errors
    this.watchForErrors();

    // Watch for successful authentication
    this.watchForAuthentication();
  }

  private watchForErrors(): void {
    // Since we're using signals, we need to watch the error signal
    setTimeout(() => {
      const subscription = new Subject<string | null>();
      let previousError = this.authState.error();

      const interval = setInterval(() => {
        const currentError = this.authState.error();
        if (currentError !== previousError && currentError) {
          this.snackBar.open(currentError, 'Close', {
            duration: 5000,
            horizontalPosition: 'center',
            verticalPosition: 'top',
            panelClass: ['error-snackbar']
          });
          previousError = currentError;
        }
      }, 100);

      this.destroy$.subscribe(() => clearInterval(interval));
    });
  }

  private watchForAuthentication(): void {
    // Watch for successful login
    setTimeout(() => {
      let wasAuthenticated = this.authState.isAuthenticated();

      const interval = setInterval(() => {
        const isAuthenticated = this.authState.isAuthenticated();
        if (!wasAuthenticated && isAuthenticated) {
          this.router.navigate(['/dashboard']);
        }
        wasAuthenticated = isAuthenticated;
      }, 100);

      this.destroy$.subscribe(() => clearInterval(interval));
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.authService.login(this.loginForm.value).subscribe();
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
    }
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}