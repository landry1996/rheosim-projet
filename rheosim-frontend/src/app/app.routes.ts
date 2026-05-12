import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'auth',
    canActivate: [guestGuard],
    children: [
      { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
      { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) }
    ]
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'datasets',
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./features/datasets/list/dataset-list.component').then(m => m.DatasetListComponent) },
      { path: 'upload', loadComponent: () => import('./features/datasets/upload/dataset-upload.component').then(m => m.DatasetUploadComponent) }
    ]
  },
  {
    path: 'simulation',
    canActivate: [authGuard],
    loadComponent: () => import('./features/simulation/simulation.component').then(m => m.SimulationComponent)
  },
  {
    path: 'reports',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reports/reports.component').then(m => m.ReportsComponent)
  },
  { path: '**', redirectTo: 'dashboard' }
];
