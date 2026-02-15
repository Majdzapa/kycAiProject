export interface User {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  customerId?: string;
  roles: string[];
  enabled: boolean;
  emailVerified: boolean;
  mfaEnabled: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: User;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}
