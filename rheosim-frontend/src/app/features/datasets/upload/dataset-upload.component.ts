import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatasetService } from '../../../core/services/dataset.service';
import { ExperimentType } from '../../../core/models/dataset.model';

@Component({
  selector: 'app-dataset-upload',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="upload-container">
      <h2>Upload Dataset</h2>

      @if (successMessage()) {
        <div class="alert alert-success">{{ successMessage() }}</div>
      }
      @if (errorMessage()) {
        <div class="alert alert-error">{{ errorMessage() }}</div>
      }

      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <div class="form-group">
          <label>Experiment Type</label>
          <select formControlName="experimentType" class="input">
            @for (type of experimentTypes; track type) {
              <option [value]="type">{{ type }}</option>
            }
          </select>
        </div>

        <div class="form-group">
          <label>Data File (CSV or Excel)</label>
          <div class="file-drop" [class.dragover]="isDragover()"
               (dragover)="onDragOver($event)" (dragleave)="isDragover.set(false)" (drop)="onDrop($event)">
            @if (selectedFile()) {
              <p class="file-name">{{ selectedFile()!.name }} ({{ formatSize(selectedFile()!.size) }})</p>
              <button type="button" class="btn btn-sm" (click)="selectedFile.set(null)">Remove</button>
            } @else {
              <p>Drag & drop file here or</p>
              <label class="btn btn-outline file-label">
                Browse
                <input type="file" accept=".csv,.xlsx,.xls" (change)="onFileSelect($event)" hidden />
              </label>
            }
          </div>
        </div>

        <button type="submit" class="btn btn-primary" [disabled]="!canSubmit() || uploading()">
          {{ uploading() ? 'Uploading...' : 'Upload & Validate' }}
        </button>
      </form>
    </div>
  `,
  styles: [`
    .upload-container { padding: 1.5rem; max-width: 600px; }
    h2 { color: #fff; margin-bottom: 1.5rem; }
    .form-group { margin-bottom: 1.25rem; }
    .form-group label { display: block; color: #a8a8b3; margin-bottom: 0.5rem; font-size: 0.875rem; }
    .input {
      width: 100%;
      padding: 0.625rem 0.875rem;
      background: #16213e;
      border: 1px solid #2a2a4a;
      border-radius: 6px;
      color: #fff;
      font-size: 0.9rem;
      box-sizing: border-box;
    }
    .input:focus { outline: none; border-color: #e94560; }
    select.input { cursor: pointer; }
    .file-drop {
      border: 2px dashed #2a2a4a;
      border-radius: 8px;
      padding: 2rem;
      text-align: center;
      color: #a8a8b3;
      transition: border-color 0.2s;
    }
    .file-drop.dragover { border-color: #e94560; background: rgba(233, 69, 96, 0.05); }
    .file-name { color: #fff; margin-bottom: 0.5rem; }
    .file-label { cursor: pointer; display: inline-block; margin-top: 0.5rem; }
    .btn { padding: 0.375rem 0.75rem; border-radius: 4px; cursor: pointer; font-size: 0.8rem; border: 1px solid #e94560; color: #e94560; background: transparent; }
    .btn:hover { background: #e94560; color: #fff; }
    .btn-primary { background: #e94560; color: #fff; border: none; padding: 0.75rem 1.5rem; font-size: 0.9rem; margin-top: 0.5rem; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-outline { border: 1px solid #e94560; }
    .btn-sm { padding: 0.25rem 0.5rem; font-size: 0.75rem; }
    .alert-success { background: rgba(76, 175, 80, 0.1); border: 1px solid #4caf50; color: #4caf50; padding: 0.75rem; border-radius: 6px; margin-bottom: 1rem; }
    .alert-error { background: rgba(233, 69, 96, 0.1); border: 1px solid #e94560; color: #e94560; padding: 0.75rem; border-radius: 6px; margin-bottom: 1rem; }
  `]
})
export class DatasetUploadComponent {
  private datasetService = inject(DatasetService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  experimentTypes: ExperimentType[] = [
    'CREEP', 'RELAXATION', 'DYNAMIC_OSCILLATORY', 'FLOW_CURVE',
    'TEMPERATURE_SWEEP', 'FREQUENCY_SWEEP', 'STRAIN_SWEEP', 'OTHER'
  ];

  form = this.fb.nonNullable.group({
    experimentType: ['CREEP' as string, Validators.required]
  });

  selectedFile = signal<File | null>(null);
  isDragover = signal(false);
  uploading = signal(false);
  successMessage = signal('');
  errorMessage = signal('');

  canSubmit = () => this.form.valid && this.selectedFile() !== null;

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedFile.set(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragover.set(true);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragover.set(false);
    if (event.dataTransfer?.files.length) {
      this.selectedFile.set(event.dataTransfer.files[0]);
    }
  }

  onSubmit(): void {
    if (!this.canSubmit()) return;
    this.uploading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const projectId = this.route.snapshot.queryParamMap.get('projectId') || '';

    this.datasetService.upload(projectId, this.form.getRawValue().experimentType, this.selectedFile()!).subscribe({
      next: (dataset) => {
        this.uploading.set(false);
        this.successMessage.set(`Dataset "${dataset.fileName}" uploaded successfully. Status: ${dataset.status}`);
        this.selectedFile.set(null);
      },
      error: (err) => {
        this.uploading.set(false);
        this.errorMessage.set(err.error?.message || 'Upload failed.');
      }
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }
}
