import { Injectable, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';

export interface Notification {
  type: string;
  timestamp: string;
  [key: string]: string;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client: Client | null = null;
  readonly notifications = signal<Notification[]>([]);
  readonly connected = signal(false);

  connect(userId: string, token: string): void {
    this.client = new Client({
      webSocketFactory: () => new SockJS(`${environment.apiUrl.replace('/api/v1', '')}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        this.connected.set(true);
        this.subscribeToNotifications(userId);
      },
      onDisconnect: () => this.connected.set(false),
      onStompError: (frame) => console.error('STOMP error:', frame.headers['message']),
      reconnectDelay: 5000,
    });

    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.connected.set(false);
  }

  private subscribeToNotifications(userId: string): void {
    this.client?.subscribe(`/user/${userId}/queue/notifications`, (message: IMessage) => {
      const notification: Notification = JSON.parse(message.body);
      this.notifications.update(list => [notification, ...list].slice(0, 50));
    });
  }

  subscribeToSimulationProgress(simulationId: string, callback: (data: Notification) => void): void {
    this.client?.subscribe(`/topic/simulations/${simulationId}`, (message: IMessage) => {
      callback(JSON.parse(message.body));
    });
  }

  clearNotifications(): void {
    this.notifications.set([]);
  }
}
