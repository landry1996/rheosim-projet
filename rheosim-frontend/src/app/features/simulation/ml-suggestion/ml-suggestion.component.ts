import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { DecimalPipe, PercentPipe } from '@angular/common';
import { MLService, MLPredictionResponse, ModelAlternative } from '../../../core/services/ml.service';

@Component({
  selector: 'app-ml-suggestion',
  standalone: true,
  imports: [DecimalPipe, PercentPipe],
  template: `
    <div class="ml-suggestion-panel">
      <div class="panel-header">
        <h4>ML Auto-Calibration</h4>
        <button class="btn-analyze" (click)="analyze()" [disabled]="loading()">
          {{ loading() ? 'Analyzing...' : 'Analyze Data' }}
        </button>
      </div>

      @if (prediction()) {
        <div class="prediction-result">
          @if (!prediction()!.mlAvailable) {
            <div class="warning">ML Service is not available. Predictions are disabled.</div>
          } @else {
            <div class="recommended">
              <div class="model-name">{{ formatModelName(prediction()!.recommendedModel) }}</div>
              <div class="confidence-bar">
                <div class="confidence-fill" [style.width.%]="prediction()!.confidence * 100"
                     [class.high]="prediction()!.confidence > 0.8"
                     [class.medium]="prediction()!.confidence > 0.5 && prediction()!.confidence <= 0.8"
                     [class.low]="prediction()!.confidence <= 0.5">
                </div>
                <span class="confidence-label">{{ prediction()!.confidence | percent:'1.0-0' }} confidence</span>
              </div>

              <div class="params-preview">
                <span class="params-title">Suggested Initial Parameters:</span>
                @for (param of prediction()!.initialParameters; track $index) {
                  <span class="param-chip">{{ param | number:'1.3-3' }}</span>
                }
              </div>

              <button class="btn-apply" (click)="applyPrediction()">
                Apply Suggestion
              </button>
            </div>

            @if (prediction()!.alternatives.length > 0) {
              <div class="alternatives">
                <span class="alt-title">Alternatives:</span>
                @for (alt of prediction()!.alternatives; track alt.modelType) {
                  <div class="alt-item" (click)="applyAlternative(alt)">
                    <span class="alt-name">{{ formatModelName(alt.modelType) }}</span>
                    <span class="alt-confidence">{{ alt.confidence | percent:'1.0-0' }}</span>
                  </div>
                }
              </div>
            }
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .ml-suggestion-panel {
      background: #1a1a2e;
      border: 1px solid #2a2a4a;
      border-radius: 8px;
      padding: 1rem;
      margin-bottom: 1rem;
    }
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.75rem;
    }
    .panel-header h4 {
      color: #fff;
      font-size: 0.9rem;
      margin: 0;
    }
    .btn-analyze {
      background: #6c63ff;
      color: #fff;
      border: none;
      padding: 0.375rem 0.75rem;
      border-radius: 4px;
      font-size: 0.75rem;
      cursor: pointer;
    }
    .btn-analyze:disabled { opacity: 0.5; cursor: not-allowed; }
    .warning {
      color: #ffc107;
      font-size: 0.8rem;
      padding: 0.5rem;
      background: rgba(255, 193, 7, 0.1);
      border-radius: 4px;
    }
    .recommended { margin-top: 0.5rem; }
    .model-name {
      color: #e94560;
      font-size: 1rem;
      font-weight: 600;
      margin-bottom: 0.5rem;
    }
    .confidence-bar {
      position: relative;
      height: 20px;
      background: #2a2a4a;
      border-radius: 10px;
      overflow: hidden;
      margin-bottom: 0.75rem;
    }
    .confidence-fill {
      height: 100%;
      border-radius: 10px;
      transition: width 0.5s ease;
    }
    .confidence-fill.high { background: #4caf50; }
    .confidence-fill.medium { background: #ff9800; }
    .confidence-fill.low { background: #e94560; }
    .confidence-label {
      position: absolute;
      right: 8px;
      top: 50%;
      transform: translateY(-50%);
      color: #fff;
      font-size: 0.7rem;
      font-weight: 600;
    }
    .params-preview { margin-bottom: 0.75rem; }
    .params-title { color: #a8a8b3; font-size: 0.75rem; display: block; margin-bottom: 0.375rem; }
    .param-chip {
      display: inline-block;
      background: #2a2a4a;
      color: #e0e0e0;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-size: 0.7rem;
      margin: 0.125rem;
      font-family: monospace;
    }
    .btn-apply {
      width: 100%;
      background: #4caf50;
      color: #fff;
      border: none;
      padding: 0.5rem;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.8rem;
      font-weight: 600;
    }
    .btn-apply:hover { background: #45a049; }
    .alternatives {
      margin-top: 0.75rem;
      padding-top: 0.75rem;
      border-top: 1px solid #2a2a4a;
    }
    .alt-title { color: #a8a8b3; font-size: 0.75rem; display: block; margin-bottom: 0.375rem; }
    .alt-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.375rem 0.5rem;
      border-radius: 4px;
      cursor: pointer;
      transition: background 0.2s;
    }
    .alt-item:hover { background: #2a2a4a; }
    .alt-name { color: #e0e0e0; font-size: 0.8rem; }
    .alt-confidence { color: #a8a8b3; font-size: 0.75rem; }
  `]
})
export class MLSuggestionComponent {
  @Input() datasetId = '';
  @Input() timePoints: number[] = [];
  @Input() values: number[] = [];
  @Input() experimentType = 'relaxation';

  @Output() modelSelected = new EventEmitter<{ model: string; parameters: number[] }>();

  private mlService = inject(MLService);

  loading = signal(false);
  prediction = signal<MLPredictionResponse | null>(null);

  analyze(): void {
    if (this.timePoints.length === 0 || this.values.length === 0) return;

    this.loading.set(true);
    this.mlService.predict({
      datasetId: this.datasetId,
      timePoints: this.timePoints,
      values: this.values,
      experimentType: this.experimentType,
    }).subscribe({
      next: (response) => {
        this.prediction.set(response);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  applyPrediction(): void {
    const pred = this.prediction();
    if (pred) {
      this.modelSelected.emit({
        model: pred.recommendedModel,
        parameters: pred.initialParameters,
      });
    }
  }

  applyAlternative(alt: ModelAlternative): void {
    this.modelSelected.emit({
      model: alt.modelType,
      parameters: alt.initialParameters,
    });
  }

  formatModelName(type: string): string {
    const names: Record<string, string> = {
      maxwell: 'Maxwell',
      kelvin_voigt: 'Kelvin-Voigt',
      prony_2: 'Prony Series (2 branches)',
      prony_3: 'Prony Series (3 branches)',
      prony_4: 'Prony Series (4 branches)',
    };
    return names[type] || type;
  }
}
