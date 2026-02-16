import { Injectable, signal, computed } from '@angular/core';
import { User } from '../models/user.model';

export interface AuthState {
    isAuthenticated: boolean;
    user: User | null;
    token: string | null;
    refreshToken: string | null;
    loading: boolean;
    error: string | null;
}

@Injectable({
    providedIn: 'root'
})
export class AuthStateService {
    // Signals for reactive state management
    private _authState = signal<AuthState>({
        isAuthenticated: false,
        user: null,
        token: null,
        refreshToken: null,
        loading: false,
        error: null
    });

    // Public read-only signals
    readonly authState = this._authState.asReadonly();
    readonly isAuthenticated = computed(() => this._authState().isAuthenticated);
    readonly currentUser = computed(() => this._authState().user);
    readonly token = computed(() => this._authState().token);
    readonly loading = computed(() => this._authState().loading);
    readonly error = computed(() => this._authState().error);
    readonly isAdmin = computed(() =>
        this._authState().user?.roles?.some(role => role === 'ADMIN' || role === 'ROLE_ADMIN') ?? false
    );
    readonly isOperator = computed(() =>
        this._authState().user?.roles?.includes('OPERATOR') ?? false
    );

    constructor() {
        // Load auth state from localStorage on initialization
        this.loadAuthStateFromStorage();
    }

    // Actions
    setLoading(loading: boolean): void {
        this._authState.update(state => ({ ...state, loading }));
    }

    setError(error: string | null): void {
        this._authState.update(state => ({ ...state, error }));
    }

    loginSuccess(user: User, token: string, refreshToken: string): void {
        const newState: AuthState = {
            isAuthenticated: true,
            user,
            token,
            refreshToken,
            loading: false,
            error: null
        };
        this._authState.set(newState);
        this.saveAuthStateToStorage(newState);
    }

    logout(): void {
        const newState: AuthState = {
            isAuthenticated: false,
            user: null,
            token: null,
            refreshToken: null,
            loading: false,
            error: null
        };
        this._authState.set(newState);
        this.clearAuthStateFromStorage();
    }

    updateUser(user: User): void {
        this._authState.update(state => ({ ...state, user }));
        this.saveAuthStateToStorage(this._authState());
    }

    refreshTokens(token: string, refreshToken: string): void {
        this._authState.update(state => ({
            ...state,
            token,
            refreshToken
        }));
        this.saveAuthStateToStorage(this._authState());
    }

    // Storage operations
    private saveAuthStateToStorage(state: AuthState): void {
        if (state.token) {
            localStorage.setItem('auth_token', state.token);
        }
        if (state.refreshToken) {
            localStorage.setItem('refresh_token', state.refreshToken);
        }
        if (state.user) {
            localStorage.setItem('current_user', JSON.stringify(state.user));
        }
    }

    private loadAuthStateFromStorage(): void {
        const token = localStorage.getItem('auth_token');
        const refreshToken = localStorage.getItem('refresh_token');
        const userJson = localStorage.getItem('current_user');

        if (token && refreshToken && userJson) {
            try {
                const user = JSON.parse(userJson) as User;
                this._authState.set({
                    isAuthenticated: true,
                    user,
                    token,
                    refreshToken,
                    loading: false,
                    error: null
                });
            } catch (error) {
                console.error('Failed to parse user from localStorage', error);
                this.clearAuthStateFromStorage();
            }
        }
    }

    private clearAuthStateFromStorage(): void {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('current_user');
    }

    // Helper method to check if token is expired
    isTokenExpired(): boolean {
        const token = this.token();
        if (!token) return true;

        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const expiryTime = payload.exp * 1000; // Convert to milliseconds
            return Date.now() >= expiryTime;
        } catch (error) {
            return true;
        }
    }
}
