import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./components/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./components/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'kyc-submit',
    loadComponent: () => import('./components/kyc-submit/kyc-submit.component').then(m => m.KycSubmitComponent)
  },
  {
    path: 'kyc-status',
    loadComponent: () => import('./components/kyc-status/kyc-status.component').then(m => m.KycStatusComponent)
  },
  {
    path: 'chatbot',
    loadComponent: () => import('./components/chatbot/chatbot.component').then(m => m.ChatbotComponent)
  },
  {
    path: 'gdpr',
    loadComponent: () => import('./components/gdpr/gdpr.component').then(m => m.GdprComponent)
  },
  {
    path: 'admin',
    loadComponent: () => import('./components/admin/admin.component').then(m => m.AdminComponent),
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'admin/customers',
    loadComponent: () => import('./components/customer-management/customer-management.component').then(m => m.CustomerManagementComponent),
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'admin/customers/:id',
    loadComponent: () => import('./components/customer-detail/customer-detail.component').then(m => m.CustomerDetailComponent),
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'admin/knowledge-base-upload',
    loadComponent: () => import('./components/knowledge-base-upload/knowledge-base-upload.component').then(m => m.KnowledgeBaseUploadComponent),
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'admin/products',
    loadComponent: () => import('./components/product-management/product-management.component').then(m => m.ProductManagementComponent),
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'admin/transactions',
    loadComponent: () => import('./components/transaction-monitor/transaction-monitor.component').then(m => m.TransactionMonitorComponent),
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'admin/audit-logs',
    loadComponent: () => import('./components/admin/admin.component').then(m => m.AdminComponent), // Reusing Admin component as placeholder
    canActivate: [authGuard, adminGuard]
  },
  {
    path: '**',
    loadComponent: () => import('./components/not-found/not-found.component').then(m => m.NotFoundComponent)
  }
];
