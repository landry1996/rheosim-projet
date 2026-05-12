import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, SidebarComponent],
  template: `
    @if (authService.isAuthenticated()) {
      <app-navbar />
      <div class="app-layout">
        <app-sidebar />
        <main class="main-content">
          <router-outlet />
        </main>
      </div>
    } @else {
      <router-outlet />
    }
  `,
  styles: [`
    :host { display: block; height: 100vh; }
    .app-layout {
      display: flex;
      min-height: calc(100vh - 60px);
    }
    .main-content {
      flex: 1;
      overflow-y: auto;
      background: #0f3460;
    }
  `]
})
export class App {
  readonly authService = inject(AuthService);
}
