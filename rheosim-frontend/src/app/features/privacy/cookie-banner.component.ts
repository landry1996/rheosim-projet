import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface ConsentState {
  necessary: boolean;
  analytics: boolean;
  marketing: boolean;
  functional: boolean;
}

@Component({
  selector: 'app-cookie-banner',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div *ngIf="showBanner" class="cookie-banner" role="dialog" aria-label="Cookie consent">
      <div class="cookie-banner__content">
        <h3>Gestion des cookies</h3>
        <p>
          Nous utilisons des cookies pour améliorer votre expérience.
          Les cookies nécessaires sont toujours actifs. Vous pouvez personnaliser
          vos préférences ci-dessous.
        </p>

        <div *ngIf="showDetails" class="cookie-banner__details">
          <label class="cookie-option">
            <input type="checkbox" [checked]="true" disabled />
            <span>Nécessaires (obligatoire)</span>
            <small>Requis pour le fonctionnement du site</small>
          </label>

          <label class="cookie-option">
            <input type="checkbox" [(ngModel)]="consents.analytics"
                   (ngModelChange)="onConsentChange()" />
            <span>Analytiques</span>
            <small>Mesure d'audience et amélioration du service</small>
          </label>

          <label class="cookie-option">
            <input type="checkbox" [(ngModel)]="consents.marketing"
                   (ngModelChange)="onConsentChange()" />
            <span>Marketing</span>
            <small>Publicités personnalisées</small>
          </label>

          <label class="cookie-option">
            <input type="checkbox" [(ngModel)]="consents.functional"
                   (ngModelChange)="onConsentChange()" />
            <span>Fonctionnels</span>
            <small>Personnalisation de l'interface</small>
          </label>
        </div>

        <div class="cookie-banner__actions">
          <button class="btn btn-outline" (click)="showDetails = !showDetails">
            {{ showDetails ? 'Masquer' : 'Personnaliser' }}
          </button>
          <button class="btn btn-secondary" (click)="rejectAll()">
            Refuser tout
          </button>
          <button class="btn btn-primary" (click)="acceptAll()">
            Accepter tout
          </button>
          <button *ngIf="showDetails" class="btn btn-primary" (click)="savePreferences()">
            Enregistrer mes choix
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .cookie-banner {
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      background: #fff;
      box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.15);
      z-index: 10000;
      padding: 24px;
      border-top: 3px solid #1976d2;
    }
    .cookie-banner__content { max-width: 1200px; margin: 0 auto; }
    .cookie-banner__content h3 { margin: 0 0 8px; }
    .cookie-banner__content p { margin: 0 0 16px; color: #555; }
    .cookie-banner__details { margin: 16px 0; }
    .cookie-option {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 0; border-bottom: 1px solid #eee;
    }
    .cookie-option small { color: #888; margin-left: auto; }
    .cookie-banner__actions { display: flex; gap: 12px; margin-top: 16px; flex-wrap: wrap; }
    .btn { padding: 10px 20px; border-radius: 4px; cursor: pointer; border: none; font-size: 14px; }
    .btn-primary { background: #1976d2; color: #fff; }
    .btn-secondary { background: #757575; color: #fff; }
    .btn-outline { background: transparent; border: 1px solid #ccc; }
  `]
})
export class CookieBannerComponent implements OnInit {
  showBanner = false;
  showDetails = false;

  consents: ConsentState = {
    necessary: true,
    analytics: false,
    marketing: false,
    functional: false
  };

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    const stored = localStorage.getItem('rheosim_cookie_consent');
    if (!stored) {
      this.showBanner = true;
    } else {
      this.consents = JSON.parse(stored);
    }
  }

  acceptAll(): void {
    this.consents = { necessary: true, analytics: true, marketing: true, functional: true };
    this.saveAndClose();
  }

  rejectAll(): void {
    this.consents = { necessary: true, analytics: false, marketing: false, functional: false };
    this.saveAndClose();
  }

  savePreferences(): void {
    this.saveAndClose();
  }

  onConsentChange(): void {}

  private saveAndClose(): void {
    localStorage.setItem('rheosim_cookie_consent', JSON.stringify(this.consents));
    this.showBanner = false;
    this.syncWithBackend();
  }

  private syncWithBackend(): void {
    const consentTypes = ['ANALYTICS', 'MARKETING', 'FUNCTIONAL'] as const;
    const mapping: Record<string, boolean> = {
      ANALYTICS: this.consents.analytics,
      MARKETING: this.consents.marketing,
      FUNCTIONAL: this.consents.functional
    };

    for (const type of consentTypes) {
      this.http.put('/api/v1/privacy/consents', {
        type,
        granted: mapping[type]
      }).subscribe();
    }
  }
}
