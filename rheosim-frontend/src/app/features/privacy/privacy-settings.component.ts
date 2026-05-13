import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

interface Consent {
  type: string;
  granted: boolean;
  grantedAt: string | null;
  revokedAt: string | null;
}

interface DeletionRequest {
  id: string;
  status: string;
  requestedAt: string;
  scheduledDeletionAt: string;
}

@Component({
  selector: 'app-privacy-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="privacy-settings">
      <h2>Paramètres de confidentialité</h2>

      <!-- Data Export Section -->
      <section class="privacy-section">
        <h3>Exporter mes données</h3>
        <p>
          Conformément à l'Article 15 & 20 du RGPD, vous pouvez télécharger une copie
          complète de vos données personnelles au format JSON.
        </p>
        <button class="btn btn-primary" (click)="requestExport()" [disabled]="exportLoading">
          {{ exportLoading ? 'Export en cours...' : 'Demander un export' }}
        </button>
        <div *ngIf="exportUrl" class="success-message">
          <p>Export prêt ! <a [href]="exportUrl" download>Télécharger (expire dans 48h)</a></p>
        </div>
      </section>

      <!-- Consent Management Section -->
      <section class="privacy-section">
        <h3>Gestion du consentement</h3>
        <p>Gérez vos préférences de collecte de données.</p>
        <div *ngFor="let consent of consents" class="consent-row">
          <div class="consent-info">
            <strong>{{ getConsentLabel(consent.type) }}</strong>
            <small>{{ getConsentDescription(consent.type) }}</small>
          </div>
          <label class="toggle">
            <input type="checkbox"
                   [checked]="consent.granted"
                   [disabled]="consent.type === 'NECESSARY'"
                   (change)="toggleConsent(consent)" />
            <span class="toggle-slider"></span>
          </label>
        </div>
      </section>

      <!-- Account Deletion Section -->
      <section class="privacy-section danger">
        <h3>Supprimer mon compte</h3>
        <p>
          Conformément à l'Article 17 du RGPD, vous pouvez demander la suppression
          complète de votre compte et de toutes vos données associées.
          Cette action est irréversible après le délai de grâce de 72 heures.
        </p>

        <div *ngIf="activeDeletion" class="deletion-status">
          <p><strong>Demande en cours</strong></p>
          <p>Statut : {{ activeDeletion.status }}</p>
          <p>Suppression prévue : {{ activeDeletion.scheduledDeletionAt | date:'medium' }}</p>
          <button class="btn btn-secondary" (click)="cancelDeletion()">Annuler la demande</button>
        </div>

        <div *ngIf="!activeDeletion">
          <textarea [(ngModel)]="deletionReason"
                    placeholder="Raison (optionnel)"
                    rows="3"></textarea>
          <button class="btn btn-danger" (click)="requestDeletion()">
            Supprimer définitivement mon compte
          </button>
        </div>
      </section>

      <!-- DPO Contact -->
      <section class="privacy-section">
        <h3>Contacter le DPO</h3>
        <p>Pour toute question relative à la protection de vos données :</p>
        <a href="/privacy/contact" class="btn btn-outline">Contacter le Délégué à la Protection des Données</a>
      </section>
    </div>
  `,
  styles: [`
    .privacy-settings { max-width: 800px; margin: 0 auto; padding: 24px; }
    .privacy-section {
      background: #fff; border-radius: 8px; padding: 24px;
      margin-bottom: 24px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .privacy-section.danger { border-left: 4px solid #d32f2f; }
    .privacy-section h3 { margin: 0 0 12px; }
    .privacy-section p { color: #555; margin-bottom: 16px; }
    .consent-row {
      display: flex; justify-content: space-between; align-items: center;
      padding: 12px 0; border-bottom: 1px solid #eee;
    }
    .consent-info { display: flex; flex-direction: column; gap: 4px; }
    .consent-info small { color: #888; }
    .toggle { position: relative; display: inline-block; width: 48px; height: 24px; }
    .toggle input { opacity: 0; width: 0; height: 0; }
    .toggle-slider {
      position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0;
      background: #ccc; border-radius: 24px; transition: 0.3s;
    }
    .toggle input:checked + .toggle-slider { background: #1976d2; }
    .success-message { background: #e8f5e9; padding: 12px; border-radius: 4px; margin-top: 12px; }
    .deletion-status { background: #fff3e0; padding: 16px; border-radius: 4px; }
    textarea { width: 100%; padding: 12px; border: 1px solid #ccc; border-radius: 4px; margin-bottom: 12px; }
    .btn { padding: 10px 20px; border-radius: 4px; cursor: pointer; border: none; font-size: 14px; text-decoration: none; display: inline-block; }
    .btn-primary { background: #1976d2; color: #fff; }
    .btn-secondary { background: #757575; color: #fff; }
    .btn-danger { background: #d32f2f; color: #fff; }
    .btn-outline { background: transparent; border: 1px solid #1976d2; color: #1976d2; }
  `]
})
export class PrivacySettingsComponent implements OnInit {
  consents: Consent[] = [];
  exportLoading = false;
  exportUrl: string | null = null;
  activeDeletion: DeletionRequest | null = null;
  deletionReason = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadConsents();
  }

  loadConsents(): void {
    this.http.get<Consent[]>('/api/v1/privacy/consents').subscribe(consents => {
      this.consents = consents;
    });
  }

  requestExport(): void {
    this.exportLoading = true;
    this.http.get<{ downloadUrl: string }>('/api/v1/privacy/export').subscribe({
      next: (response) => {
        this.exportUrl = response.downloadUrl;
        this.exportLoading = false;
      },
      error: () => { this.exportLoading = false; }
    });
  }

  toggleConsent(consent: Consent): void {
    consent.granted = !consent.granted;
    this.http.put('/api/v1/privacy/consents', {
      type: consent.type,
      granted: consent.granted
    }).subscribe();
  }

  requestDeletion(): void {
    if (!confirm('Êtes-vous sûr ? Cette action supprimera définitivement votre compte après 72h.')) {
      return;
    }
    this.http.post<DeletionRequest>('/api/v1/privacy/delete-account', {
      reason: this.deletionReason
    }).subscribe(deletion => {
      this.activeDeletion = deletion;
    });
  }

  cancelDeletion(): void {
    if (!this.activeDeletion) return;
    this.http.post<DeletionRequest>(
      `/api/v1/privacy/delete-account/${this.activeDeletion.id}/cancel`, {}
    ).subscribe(() => {
      this.activeDeletion = null;
    });
  }

  getConsentLabel(type: string): string {
    const labels: Record<string, string> = {
      NECESSARY: 'Cookies nécessaires',
      ANALYTICS: 'Cookies analytiques',
      MARKETING: 'Cookies marketing',
      FUNCTIONAL: 'Cookies fonctionnels'
    };
    return labels[type] || type;
  }

  getConsentDescription(type: string): string {
    const descriptions: Record<string, string> = {
      NECESSARY: 'Requis pour le fonctionnement du site (non désactivable)',
      ANALYTICS: 'Mesure d\'audience anonyme pour améliorer le service',
      MARKETING: 'Publicités et communications personnalisées',
      FUNCTIONAL: 'Personnalisation de l\'interface et préférences'
    };
    return descriptions[type] || '';
  }
}
