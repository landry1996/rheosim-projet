import { Component, OnInit, signal, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MeshViewerComponent, MeshData } from './mesh-viewer.component';
import { WebSocketService } from '../../core/services/websocket.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-results-viewer',
  standalone: true,
  imports: [CommonModule, MeshViewerComponent],
  template: `
    <div class="results-container">
      <div class="results-header">
        <h2>3D Simulation Results</h2>
        @if (progress() > 0 && progress() < 100) {
          <div class="progress-bar">
            <div class="progress-fill" [style.width.%]="progress()"></div>
            <span>{{ progressPhase() }} — {{ progress() }}%</span>
          </div>
        }
      </div>

      @if (meshData()) {
        <app-mesh-viewer [meshData]="meshData()" />
      } @else {
        <div class="placeholder">
          <p>No 3D results available yet.</p>
          <p>Run a 3D FEM simulation to visualize deformation and stress fields.</p>
        </div>
      }

      @if (meshData()) {
        <div class="results-stats">
          <div class="stat-card">
            <span class="stat-label">Max Displacement</span>
            <span class="stat-value">{{ maxDisplacement() | number:'1.4-4' }} mm</span>
          </div>
          <div class="stat-card">
            <span class="stat-label">Max Von Mises</span>
            <span class="stat-value">{{ maxStress() | number:'1.2-2' }} MPa</span>
          </div>
          <div class="stat-card">
            <span class="stat-label">Nodes</span>
            <span class="stat-value">{{ meshData()!.nodes.length }}</span>
          </div>
          <div class="stat-card">
            <span class="stat-label">Elements</span>
            <span class="stat-value">{{ meshData()!.elements.length }}</span>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .results-container { padding: 24px; }
    .results-header { margin-bottom: 16px; }
    .progress-bar {
      position: relative;
      height: 24px;
      background: #e0e0e0;
      border-radius: 12px;
      overflow: hidden;
      margin-top: 8px;
    }
    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #1976d2, #42a5f5);
      transition: width 0.3s;
    }
    .progress-bar span {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      font-size: 11px;
      font-weight: 500;
    }
    .placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 300px;
      background: #f5f5f5;
      border-radius: 8px;
      color: #666;
    }
    .results-stats {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      margin-top: 16px;
    }
    .stat-card {
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;
      text-align: center;
    }
    .stat-label { display: block; color: #666; font-size: 12px; margin-bottom: 4px; }
    .stat-value { display: block; font-size: 18px; font-weight: 600; color: #1976d2; }
  `]
})
export class ResultsViewerComponent implements OnInit {
  simulationId = input<string>('');

  meshData = signal<MeshData | null>(null);
  progress = signal(0);
  progressPhase = signal('');
  maxDisplacement = signal(0);
  maxStress = signal(0);

  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private wsService: WebSocketService) {}

  ngOnInit(): void {
    const simId = this.simulationId();
    if (simId) {
      this.loadResults(simId);
      this.wsService.subscribeToSimulationProgress(simId, (data) => {
        this.progress.set(parseInt(data['progress'] || '0'));
        this.progressPhase.set(data['phase'] || '');
      });
    }
  }

  private loadResults(simulationId: string): void {
    this.http.get<MeshData>(`${this.apiUrl}/simulations/${simulationId}/mesh-results`).subscribe({
      next: (data) => {
        this.meshData.set(data);
        this.computeStats(data);
      },
      error: () => this.meshData.set(null)
    });
  }

  private computeStats(data: MeshData): void {
    if (data.displacements) {
      const maxDisp = Math.max(...data.displacements.map(d => Math.sqrt(d[0]**2 + d[1]**2 + d[2]**2)));
      this.maxDisplacement.set(maxDisp * 1000);
    }
    if (data.stresses) {
      this.maxStress.set(Math.max(...data.stresses));
    }
  }
}
