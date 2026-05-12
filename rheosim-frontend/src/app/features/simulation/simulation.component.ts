import { Component, inject, OnInit, signal, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SimulationService } from '../../core/services/simulation.service';
import { MaterialService } from '../../core/services/material.service';
import { DatasetService } from '../../core/services/dataset.service';
import { SimulationJob, SimulationResult, FitTarget, SimulationType } from '../../core/models/simulation.model';
import { Material } from '../../core/models/material.model';
import { Dataset } from '../../core/models/dataset.model';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-simulation',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe],
  template: `
    <div class="simulation-page">
      <h2>Model Calibration</h2>

      <div class="sim-layout">
        <div class="sim-form-panel">
          <form [formGroup]="form" (ngSubmit)="submitJob()">
            <div class="form-group">
              <label>Material</label>
              <select formControlName="materialId" class="input">
                <option value="">Select material...</option>
                @for (m of materials(); track m.id) {
                  <option [value]="m.id">{{ m.name }} ({{ m.family }})</option>
                }
              </select>
            </div>
            <div class="form-group">
              <label>Dataset</label>
              <select formControlName="datasetId" class="input">
                <option value="">Select dataset...</option>
                @for (ds of datasets(); track ds.id) {
                  <option [value]="ds.id">{{ ds.fileName }} ({{ ds.experimentType }})</option>
                }
              </select>
            </div>
            <div class="form-group">
              <label>Simulation Type</label>
              <select formControlName="simulationType" class="input">
                <option value="PARAMETER_IDENTIFICATION">Parameter Identification</option>
                <option value="FORWARD_SIMULATION">Forward Simulation</option>
              </select>
            </div>
            <div class="form-group">
              <label>Fit Target</label>
              <select formControlName="fitTarget" class="input">
                <option value="RELAXATION_MODULUS">Relaxation Modulus G(t)</option>
                <option value="CREEP_COMPLIANCE">Creep Compliance J(t)</option>
                <option value="STORAGE_MODULUS">Storage Modulus G'(w)</option>
                <option value="LOSS_MODULUS">Loss Modulus G''(w)</option>
                <option value="COMPLEX_VISCOSITY">Complex Viscosity |eta*|(w)</option>
              </select>
            </div>
            <button type="submit" class="btn btn-primary" [disabled]="form.invalid || running()">
              {{ running() ? 'Running...' : 'Submit Job' }}
            </button>
          </form>

          @if (currentJob()) {
            <div class="job-status">
              <h4>Job Status</h4>
              <div class="status-row">
                <span class="badge" [class]="'badge-' + currentJob()!.status.toLowerCase()">{{ currentJob()!.status }}</span>
                @if (currentJob()!.elapsedMs) {
                  <span class="elapsed">{{ currentJob()!.elapsedMs }}ms</span>
                }
              </div>
            </div>
          }

          @if (result()) {
            <div class="results-panel">
              <h4>Results</h4>
              <div class="metrics">
                <div class="metric"><span class="metric-label">R²</span><span class="metric-value">{{ result()!.rSquared | number:'1.6-6' }}</span></div>
                <div class="metric"><span class="metric-label">Iterations</span><span class="metric-value">{{ result()!.iterations }}</span></div>
                <div class="metric"><span class="metric-label">Converged</span><span class="metric-value">{{ result()!.converged ? 'Yes' : 'No' }}</span></div>
              </div>
              <h5>Identified Parameters</h5>
              <table class="params-table">
                @for (name of result()!.parameterNames; track name; let i = $index) {
                  <tr>
                    <td>{{ name }}</td>
                    <td>{{ result()!.identifiedParameters[i] | number:'1.4-4' }}</td>
                  </tr>
                }
              </table>
            </div>
          }
        </div>

        <div class="chart-panel">
          <canvas #chartCanvas></canvas>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .simulation-page { padding: 1.5rem; }
    h2 { color: #fff; margin-bottom: 1.5rem; }
    .sim-layout { display: grid; grid-template-columns: 380px 1fr; gap: 1.5rem; }
    .sim-form-panel { background: #16213e; padding: 1.25rem; border-radius: 8px; }
    .chart-panel { background: #16213e; padding: 1.25rem; border-radius: 8px; min-height: 400px; display: flex; align-items: center; justify-content: center; }
    .chart-panel canvas { width: 100% !important; height: 100% !important; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; color: #a8a8b3; margin-bottom: 0.375rem; font-size: 0.8rem; }
    .input {
      width: 100%;
      padding: 0.5rem 0.75rem;
      background: #1a1a2e;
      border: 1px solid #2a2a4a;
      border-radius: 6px;
      color: #fff;
      font-size: 0.85rem;
      box-sizing: border-box;
    }
    .input:focus { outline: none; border-color: #e94560; }
    .btn-primary { background: #e94560; color: #fff; border: none; padding: 0.625rem 1.25rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; width: 100%; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .job-status { margin-top: 1.25rem; padding-top: 1rem; border-top: 1px solid #2a2a4a; }
    .job-status h4 { color: #fff; margin-bottom: 0.5rem; font-size: 0.9rem; }
    .status-row { display: flex; align-items: center; gap: 0.75rem; }
    .badge { padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 600; text-transform: uppercase; }
    .badge-completed { background: rgba(76, 175, 80, 0.2); color: #4caf50; }
    .badge-running { background: rgba(33, 150, 243, 0.2); color: #2196f3; }
    .badge-queued { background: rgba(255, 193, 7, 0.2); color: #ffc107; }
    .badge-failed { background: rgba(233, 69, 96, 0.2); color: #e94560; }
    .elapsed { color: #a8a8b3; font-size: 0.8rem; }
    .results-panel { margin-top: 1.25rem; padding-top: 1rem; border-top: 1px solid #2a2a4a; }
    .results-panel h4, .results-panel h5 { color: #fff; margin-bottom: 0.5rem; font-size: 0.9rem; }
    .metrics { display: flex; gap: 1rem; margin-bottom: 1rem; }
    .metric { display: flex; flex-direction: column; }
    .metric-label { color: #a8a8b3; font-size: 0.7rem; text-transform: uppercase; }
    .metric-value { color: #e94560; font-weight: 600; font-size: 0.9rem; }
    .params-table { width: 100%; }
    .params-table td { padding: 0.25rem 0.5rem; color: #e0e0e0; font-size: 0.8rem; border-bottom: 1px solid #2a2a4a; }
    .params-table td:first-child { color: #a8a8b3; }
  `]
})
export class SimulationComponent implements OnInit, OnDestroy {
  @ViewChild('chartCanvas', { static: true }) chartCanvas!: ElementRef<HTMLCanvasElement>;

  private simulationService = inject(SimulationService);
  private materialService = inject(MaterialService);
  private datasetService = inject(DatasetService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);

  private chart: Chart | null = null;
  private pollInterval: ReturnType<typeof setInterval> | null = null;

  materials = signal<Material[]>([]);
  datasets = signal<Dataset[]>([]);
  currentJob = signal<SimulationJob | null>(null);
  result = signal<SimulationResult | null>(null);
  running = signal(false);

  form = this.fb.nonNullable.group({
    materialId: ['', Validators.required],
    datasetId: ['', Validators.required],
    simulationType: ['PARAMETER_IDENTIFICATION' as SimulationType, Validators.required],
    fitTarget: ['RELAXATION_MODULUS' as FitTarget, Validators.required]
  });

  ngOnInit(): void {
    const projectId = this.route.snapshot.queryParamMap.get('projectId') || '';
    if (projectId) {
      this.materialService.getByProject(projectId).subscribe(m => this.materials.set(m));
      this.datasetService.getByProject(projectId).subscribe(ds => this.datasets.set(ds.filter(d => d.status === 'VALID')));
    }
  }

  ngOnDestroy(): void {
    if (this.pollInterval) clearInterval(this.pollInterval);
    this.chart?.destroy();
  }

  submitJob(): void {
    if (this.form.invalid) return;
    this.running.set(true);
    this.result.set(null);

    const projectId = this.route.snapshot.queryParamMap.get('projectId') || '';
    const formValue = this.form.getRawValue();

    this.simulationService.submit({ ...formValue, projectId }).subscribe({
      next: (job) => {
        this.currentJob.set(job);
        this.startPolling(job.id);
      },
      error: () => this.running.set(false)
    });
  }

  private startPolling(jobId: string): void {
    this.pollInterval = setInterval(() => {
      this.simulationService.getStatus(jobId).subscribe(job => {
        this.currentJob.set(job);
        if (job.status === 'COMPLETED' || job.status === 'FAILED' || job.status === 'CANCELLED') {
          this.running.set(false);
          if (this.pollInterval) clearInterval(this.pollInterval);
          if (job.status === 'COMPLETED') this.loadResult(jobId);
        }
      });
    }, 2000);
  }

  private loadResult(jobId: string): void {
    this.simulationService.getResult(jobId).subscribe(res => {
      this.result.set(res);
      this.renderChart(res);
    });
  }

  private renderChart(res: SimulationResult): void {
    this.chart?.destroy();
    const labels = res.fittedCurve.map((_, i) => i.toString());
    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Fitted Curve',
          data: res.fittedCurve,
          borderColor: '#e94560',
          backgroundColor: 'rgba(233, 69, 96, 0.1)',
          fill: true,
          tension: 0.3
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { labels: { color: '#a8a8b3' } } },
        scales: {
          x: { ticks: { color: '#6c6c80' }, grid: { color: '#2a2a4a' } },
          y: { ticks: { color: '#6c6c80' }, grid: { color: '#2a2a4a' } }
        }
      }
    });
  }
}
