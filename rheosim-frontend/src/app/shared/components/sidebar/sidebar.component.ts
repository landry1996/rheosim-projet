import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <nav class="sidebar-nav">
        <a routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" class="nav-item">
          <span class="nav-icon">&#9632;</span>
          <span>Dashboard</span>
        </a>
        <a routerLink="/datasets" routerLinkActive="active" class="nav-item">
          <span class="nav-icon">&#9776;</span>
          <span>Datasets</span>
        </a>
        <a routerLink="/simulation" routerLinkActive="active" class="nav-item">
          <span class="nav-icon">&#9881;</span>
          <span>Simulation</span>
        </a>
        <a routerLink="/reports" routerLinkActive="active" class="nav-item">
          <span class="nav-icon">&#9998;</span>
          <span>Reports</span>
        </a>
      </nav>
    </aside>
  `,
  styles: [`
    .sidebar {
      width: 220px;
      min-height: calc(100vh - 60px);
      background: #16213e;
      padding: 1rem 0;
    }
    .sidebar-nav {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .nav-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 1.5rem;
      color: #a8a8b3;
      text-decoration: none;
      font-size: 0.9rem;
      transition: all 0.2s;
    }
    .nav-item:hover { color: #fff; background: rgba(233, 69, 96, 0.1); }
    .nav-item.active {
      color: #e94560;
      background: rgba(233, 69, 96, 0.15);
      border-right: 3px solid #e94560;
    }
    .nav-icon { font-size: 1.1rem; }
  `]
})
export class SidebarComponent {}
