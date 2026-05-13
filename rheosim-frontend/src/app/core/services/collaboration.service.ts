import { Injectable, inject, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export interface AwarenessUser {
  id: string;
  name: string;
  color: string;
  cursor?: { field: string; position: number };
  lastActive: string;
}

export interface CollaborationState {
  connected: boolean;
  users: AwarenessUser[];
  documentId: string | null;
}

@Injectable({ providedIn: 'root' })
export class CollaborationService {
  private readonly authService = inject(AuthService);
  private ws: WebSocket | null = null;
  private documentId: string | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;

  state = signal<CollaborationState>({
    connected: false,
    users: [],
    documentId: null,
  });

  private changeCallbacks: Array<(data: ArrayBuffer) => void> = [];

  connect(documentId: string, user: AwarenessUser): void {
    this.documentId = documentId;
    const token = this.authService.getAccessToken();
    if (!token) {
      return;
    }
    const baseWsUrl = environment.wsUrl || `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`;
    const wsUrl = `${baseWsUrl}/ws/collaboration/${encodeURIComponent(documentId)}?token=${encodeURIComponent(token)}`;
    this.ws = new WebSocket(wsUrl);
    this.ws.binaryType = 'arraybuffer';

    this.ws.onopen = () => {
      this.reconnectAttempts = 0;
      this.state.set({ ...this.state(), connected: true, documentId });
      this.sendAwareness(user);
    };

    this.ws.onmessage = (event) => {
      if (event.data instanceof ArrayBuffer) {
        this.changeCallbacks.forEach(cb => cb(event.data));
      } else if (typeof event.data === 'string') {
        this.handleAwarenessMessage(event.data);
      }
    };

    this.ws.onclose = () => {
      this.state.set({ ...this.state(), connected: false });
      this.attemptReconnect(documentId, user);
    };

    this.ws.onerror = () => {
      this.ws?.close();
    };
  }

  disconnect(): void {
    this.ws?.close();
    this.ws = null;
    this.documentId = null;
    this.state.set({ connected: false, users: [], documentId: null });
  }

  sendUpdate(data: ArrayBuffer): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(data);
    }
  }

  sendAwareness(user: AwarenessUser): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const msg = JSON.stringify({ type: 'awareness', user });
      this.ws.send(msg);
    }
  }

  onRemoteChange(callback: (data: ArrayBuffer) => void): void {
    this.changeCallbacks.push(callback);
  }

  removeChangeCallback(callback: (data: ArrayBuffer) => void): void {
    this.changeCallbacks = this.changeCallbacks.filter(cb => cb !== callback);
  }

  private handleAwarenessMessage(data: string): void {
    try {
      const msg = JSON.parse(data);
      if (msg.type === 'awareness') {
        const users = [...this.state().users];
        const existingIdx = users.findIndex(u => u.id === msg.user.id);
        if (existingIdx >= 0) {
          users[existingIdx] = msg.user;
        } else {
          users.push(msg.user);
        }
        this.state.set({ ...this.state(), users });
      }
    } catch {
      // Ignore non-JSON messages
    }
  }

  private attemptReconnect(documentId: string, user: AwarenessUser): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
      setTimeout(() => this.connect(documentId, user), delay);
    }
  }
}
