import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse, RegisterRequest, User } from '../models/user.model';
import { AuthStateService } from './auth-state.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private authState = inject(AuthStateService);
  private apiUrl = `${environment.apiUrl}/auth`;

  login(request: LoginRequest): Observable<LoginResponse> {
    this.authState.setLoading(true);
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap({
     next: (response) => {
       console.log('LOGIN RESPONSE:', response);
       this.authState.loginSuccess(response.user, response.token, response.refreshToken);
     },
        error: (error) => {
          this.authState.setLoading(false);
          this.authState.setError(error.message || error.error?.message || 'Login failed');
        }
      })
    );
  }

  register(request: RegisterRequest): Observable<any> {
    this.authState.setLoading(true);
    return this.http.post(`${this.apiUrl}/register`, request).pipe(
      tap({
        next: () => {
          this.authState.setLoading(false);
        },
        error: (error) => {
          this.authState.setLoading(false);
          this.authState.setError(error.message || error.error?.message || 'Registration failed');
        }
      })
    );
  }

  logout(): void {
    this.authState.logout();
  }

  refreshToken(): Observable<LoginResponse> {
    const refreshToken = this.authState.authState().refreshToken;
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    return this.http.post<LoginResponse>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      tap({
        next: (response) => {
          this.authState.refreshTokens(response.token, response.refreshToken);
        },
        error: () => {
          // If refresh fails, logout the user
          this.authState.logout();
        }
      })
    );
  }

  isAuthenticated(): boolean {
    return this.authState.isAuthenticated();
  }

  getToken(): string | null {
    return this.authState.token();
  }

  getCurrentUser(): User | null {
    return this.authState.currentUser();
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user?.roles.includes(role) ?? false;
  }
}
