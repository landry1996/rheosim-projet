import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface ExportStatus {
  userId: string;
  downloadUrl: string;
  expiresAt: string;
  sizeBytes: number;
}

@Component({
  selector: 'app-data-export',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="data-export">
      <h2>Export de données personnelles</h2>

      <div class="export-info">
        <p>
          Conformément au Règlement Général sur la Protection des Données (RGPD),
          vous avez le droit d'obtenir une copie de toutes les données personnelles
          que nous détenons à votre sujet.
        </p>

        <h3>Données incluses dans l'export :</h3>
        <ul>
          <li>Profil utilisateur (nom, email, date d'inscription)</li>
          <li>Projets et configurations</li>
          <li>Matériaux et modèles constitutifs</li>
          <li>Historique des simulations</li>
          <li>Rapports générés</li>
          <li>Préférences de consentement</li>
        </ul>

        <h3>Format de l'export :</h3>
        <p>
          Les données sont fournies au format JSON dans une archive ZIP,
          conformément à l'Article 20 (droit à la portabilité).
        </p>

        <div class="export-limits">
          <p><strong>Limitations :</strong></p>
          <ul>
            <li>1 export par période de 24 heures</li>
            <li>Le lien de téléchargement expire après 48 heures</li>
            <li>L'export peut prendre jusqu'à 5 minutes pour les comptes volumineux</li>
          </ul>
        </div>
      </div>

      <div class="export-actions">
        <button class="btn btn-primary" (click)="requestExport()" [disabled]="loading">
          {{ loading ? 'Génération en cours...' : 'Générer mon export' }}
        </button>
      </div>

      <div *ngIf="exportStatus" class="export-result">
        <div class="success-card">
          <h4>Export prêt !</h4>
          <p>Taille : {{ formatSize(exportStatus.sizeBytes) }}</p>
          <p>Expire le : {{ exportStatus.expiresAt | date:'medium' }}</p>
          <a [href]="exportStatus.downloadUrl" class="btn btn-primary" download>
            Télécharger l'archive
          </a>
        </div>
      </div>

      <div *ngIf="error" class="error-message">
        <p>{{ error }}</p>
      </div>
    </div>
  `,
  styles: [`
    .data-export { max-width: 700px; margin: 0 auto; padding: 24px; }
    .export-info { background: #f5f5f5; padding: 24px; border-radius: 8px; margin-bottom: 24px; }
    .export-info h3 { margin: 16px 0 8px; }
    .export-info ul { padding-left: 20px; }
    .export-info li { margin-bottom: 4px; color: #555; }
    .export-limits { background: #fff3e0; padding: 12px 16px; border-radius: 4px; margin-top: 16px; }
    .export-actions { text-align: center; margin: 24px 0; }
    .export-result { margin-top: 24px; }
    .success-card {
      background: #e8f5e9; padding: 24px; border-radius: 8px;
      text-align: center; border: 1px solid #a5d6a7;
    }
    .success-card h4 { color: #2e7d32; margin-bottom: 12px; }
    .error-message { background: #ffebee; padding: 16px; border-radius: 4px; color: #c62828; }
    .btn { padding: 12px 24px; border-radius: 4px; cursor: pointer; border: none; font-size: 14px; text-decoration: none; display: inline-block; }
    .btn-primary { background: #1976d2; color: #fff; }
    .btn:disabled { opacity: 0.6; cursor: not-allowed; }
  `]
})
export class DataExportComponent {
  loading = false;
  exportStatus: ExportStatus | null = null;
  error: string | null = null;

  constructor(private http: HttpClient) {}

  requestExport(): void {
    this.loading = true;
    this.error = null;
    this.exportStatus = null;

    this.http.get<ExportStatus>('/api/v1/privacy/export').subscribe({
      next: (status) => {
        this.exportStatus = status;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 429) {
          this.error = 'Vous avez déjà effectué un export dans les dernières 24 heures.';
        } else {
          this.error = 'Une erreur est survenue. Veuillez réessayer plus tard.';
        }
      }
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
}
