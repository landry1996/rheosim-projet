import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink],
  template: `
    <nav class="navbar">
      <div class="navbar-brand">
        <a routerLink="/dashboard" class="logo">
          <span class="logo-icon">&#9678;</span>
          <span class="logo-text">RheoSim</span>
        </a>
      </div>
      <div class="navbar-end">
        @if (authService.user(); as user) {
          <span class="user-info">{{ user.firstName }} {{ user.lastName }}</span>
          <button class="btn btn-sm btn-outline" (click)="authService.logout()">Logout</button>
        }
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 1.5rem;
      height: 60px;
      background: #1a1a2e;
      border-bottom: 1px solid #16213e;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      text-decoration: none;
      color: #e94560;
      font-weight: 700;
      font-size: 1.25rem;
    }
    .logo-icon { font-size: 1.5rem; }
    .navbar-end {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .user-info { color: #a8a8b3; font-size: 0.875rem; }
    .btn {
      padding: 0.375rem 0.75rem;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.8rem;
      border: 1px solid #e94560;
      color: #e94560;
      background: transparent;
      transition: all 0.2s;
    }
    .btn:hover { background: #e94560; color: #fff; }
  `]
})
export class NavbarComponent {
  readonly authService = inject(AuthService);
}
