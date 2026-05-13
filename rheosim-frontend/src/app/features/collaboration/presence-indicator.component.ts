import { Component, inject } from '@angular/core';
import { CollaborationService, AwarenessUser } from '../../core/services/collaboration.service';

@Component({
  selector: 'app-presence-indicator',
  standalone: true,
  template: `
    <div class="presence-bar">
      @if (collaboration.state().connected) {
        <span class="status-dot connected"></span>
        <span class="status-text">Live</span>
      } @else {
        <span class="status-dot disconnected"></span>
        <span class="status-text">Offline</span>
      }

      <div class="avatars">
        @for (user of collaboration.state().users; track user.id) {
          <div class="avatar" [style.background-color]="user.color" [title]="user.name">
            {{ user.name.charAt(0).toUpperCase() }}
          </div>
        }
      </div>

      @if (collaboration.state().users.length > 0) {
        <span class="user-count">{{ collaboration.state().users.length }} online</span>
      }
    </div>
  `,
  styles: [`
    .presence-bar {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.375rem 0.75rem;
      background: #1a1a2e;
      border-radius: 20px;
      border: 1px solid #2a2a4a;
    }
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    .status-dot.connected { background: #4caf50; box-shadow: 0 0 4px #4caf50; }
    .status-dot.disconnected { background: #6c6c80; }
    .status-text { color: #a8a8b3; font-size: 0.75rem; }
    .avatars { display: flex; margin-left: 0.5rem; }
    .avatar {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 0.7rem;
      font-weight: 600;
      margin-left: -6px;
      border: 2px solid #1a1a2e;
    }
    .avatar:first-child { margin-left: 0; }
    .user-count { color: #6c6c80; font-size: 0.7rem; margin-left: 0.25rem; }
  `]
})
export class PresenceIndicatorComponent {
  collaboration = inject(CollaborationService);
}
