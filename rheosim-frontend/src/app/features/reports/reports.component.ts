import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ReportService } from '../../core/services/report.service';
import { Report, ReportFormat } from '../../core/models/report.model';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  template: `
    <div class="reports-page">
      <div class="header">
        <h2>Reports</h2>
        <button class="btn btn-primary" (click)="showForm.set(!showForm())">
          {{ showForm() ? 'Cancel' : '+ Generate Report' }}
        </button>
      </div>

      @if (showForm()) {
        <div class="generate-form">
          <form [formGroup]="form" (ngSubmit)="generate()">
            <div class="form-row">
              <div class="form-group">
                <label>Title</label>
                <input formControlName="title" class="input" placeholder="Report title" />
              </div>
              <div class="form-group">
                <label>Format</label>
                <select formControlName="format" class="input">
                  <option value="PDF">PDF</option>
                  <option value="CSV">CSV</option>
                  <option value="JSON">JSON</option>
                </select>
              </div>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || generating()">
                {{ generating() ? 'Generating...' : 'Generate' }}
              </button>
            </div>
          </form>
        </div>
      }

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Format</th>
              <th>Status</th>
              <th>Size</th>
              <th>Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (report of reports(); track report.id) {
              <tr>
                <td>{{ report.title }}</td>
                <td>{{ report.format }}</td>
                <td><span class="badge" [class]="'badge-' + report.status.toLowerCase()">{{ report.status }}</span></td>
                <td>{{ formatSize(report.fileSizeBytes) }}</td>
                <td>{{ report.createdAt | date:'short' }}</td>
                <td>
                  @if (report.status === 'READY') {
                    <button class="btn btn-sm" (click)="download(report)">Download</button>
                  }
                  <button class="btn btn-sm btn-danger" (click)="deleteReport(report.id)">Delete</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="empty">No reports generated yet.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .reports-page { padding: 1.5rem; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .header h2 { color: #fff; margin: 0; }
    .generate-form { background: #16213e; padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; }
    .form-row { display: flex; gap: 0.75rem; align-items: flex-end; }
    .form-group { margin-bottom: 0; flex: 1; }
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
    .table-container { overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; background: #16213e; border-radius: 8px; overflow: hidden; }
    .data-table th { background: #1a1a2e; color: #a8a8b3; padding: 0.75rem 1rem; text-align: left; font-size: 0.8rem; text-transform: uppercase; }
    .data-table td { padding: 0.75rem 1rem; color: #e0e0e0; font-size: 0.875rem; border-top: 1px solid #2a2a4a; }
    .badge { padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 600; text-transform: uppercase; }
    .badge-ready { background: rgba(76, 175, 80, 0.2); color: #4caf50; }
    .badge-generating { background: rgba(33, 150, 243, 0.2); color: #2196f3; }
    .badge-failed { background: rgba(233, 69, 96, 0.2); color: #e94560; }
    .badge-expired { background: rgba(158, 158, 158, 0.2); color: #9e9e9e; }
    .btn { padding: 0.375rem 0.75rem; border-radius: 4px; cursor: pointer; font-size: 0.8rem; border: 1px solid #e94560; color: #e94560; background: transparent; }
    .btn:hover { background: #e94560; color: #fff; }
    .btn-primary { background: #e94560; color: #fff; border: none; padding: 0.5rem 1rem; }
    .btn-sm { padding: 0.25rem 0.5rem; font-size: 0.75rem; }
    .btn-danger { border-color: #f44336; color: #f44336; }
    .btn-danger:hover { background: #f44336; color: #fff; }
    .empty { text-align: center; color: #a8a8b3; padding: 2rem !important; }
  `]
})
export class ReportsComponent implements OnInit {
  private reportService = inject(ReportService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);

  reports = signal<Report[]>([]);
  showForm = signal(false);
  generating = signal(false);
  projectId = '';

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    format: ['PDF' as ReportFormat, Validators.required]
  });

  ngOnInit(): void {
    this.projectId = this.route.snapshot.queryParamMap.get('projectId') || '';
    this.loadReports();
  }

  generate(): void {
    if (this.form.invalid) return;
    this.generating.set(true);
    const formValue = this.form.getRawValue();
    this.reportService.generate({ ...formValue, projectId: this.projectId }).subscribe({
      next: () => {
        this.generating.set(false);
        this.showForm.set(false);
        this.form.reset({ title: '', format: 'PDF' });
        this.loadReports();
      },
      error: () => this.generating.set(false)
    });
  }

  download(report: Report): void {
    this.reportService.download(report.id).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${report.title}.${report.format.toLowerCase()}`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  deleteReport(id: string): void {
    this.reportService.delete(id).subscribe({ next: () => this.loadReports() });
  }

  formatSize(bytes: number): string {
    if (!bytes) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  private loadReports(): void {
    if (this.projectId) {
      this.reportService.getByProject(this.projectId).subscribe(r => this.reports.set(r));
    }
  }
}
