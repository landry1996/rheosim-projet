import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { DatasetService } from '../../../core/services/dataset.service';
import { Dataset } from '../../../core/models/dataset.model';

@Component({
  selector: 'app-dataset-list',
  standalone: true,
  imports: [RouterLink, DatePipe],
  template: `
    <div class="dataset-list">
      <div class="header">
        <h2>Datasets</h2>
        <a [routerLink]="['/datasets/upload']" [queryParams]="{projectId: projectId}" class="btn btn-primary">+ Upload</a>
      </div>

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>File Name</th>
              <th>Type</th>
              <th>Status</th>
              <th>Rows</th>
              <th>Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (ds of datasets(); track ds.id) {
              <tr>
                <td>{{ ds.fileName }}</td>
                <td>{{ ds.experimentType }}</td>
                <td><span class="badge" [class]="'badge-' + ds.status.toLowerCase()">{{ ds.status }}</span></td>
                <td>{{ ds.rowCount }}</td>
                <td>{{ ds.createdAt | date:'short' }}</td>
                <td>
                  <button class="btn btn-sm btn-danger" (click)="deleteDataset(ds.id)">Delete</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="empty">No datasets uploaded yet.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .dataset-list { padding: 1.5rem; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .header h2 { color: #fff; margin: 0; }
    .table-container { overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; background: #16213e; border-radius: 8px; overflow: hidden; }
    .data-table th { background: #1a1a2e; color: #a8a8b3; padding: 0.75rem 1rem; text-align: left; font-size: 0.8rem; text-transform: uppercase; }
    .data-table td { padding: 0.75rem 1rem; color: #e0e0e0; font-size: 0.875rem; border-top: 1px solid #2a2a4a; }
    .badge { padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 600; text-transform: uppercase; }
    .badge-valid { background: rgba(76, 175, 80, 0.2); color: #4caf50; }
    .badge-invalid { background: rgba(233, 69, 96, 0.2); color: #e94560; }
    .badge-uploaded { background: rgba(255, 193, 7, 0.2); color: #ffc107; }
    .badge-validating { background: rgba(33, 150, 243, 0.2); color: #2196f3; }
    .badge-processing { background: rgba(156, 39, 176, 0.2); color: #9c27b0; }
    .btn { padding: 0.375rem 0.75rem; border-radius: 4px; cursor: pointer; font-size: 0.8rem; border: 1px solid #e94560; color: #e94560; background: transparent; text-decoration: none; }
    .btn:hover { background: #e94560; color: #fff; }
    .btn-primary { background: #e94560; color: #fff; border: none; }
    .btn-sm { padding: 0.25rem 0.5rem; font-size: 0.75rem; }
    .btn-danger { border-color: #f44336; color: #f44336; }
    .btn-danger:hover { background: #f44336; color: #fff; }
    .empty { text-align: center; color: #a8a8b3; padding: 2rem !important; }
  `]
})
export class DatasetListComponent implements OnInit {
  private datasetService = inject(DatasetService);
  private route = inject(ActivatedRoute);

  datasets = signal<Dataset[]>([]);
  projectId = '';

  ngOnInit(): void {
    this.projectId = this.route.snapshot.queryParamMap.get('projectId') || '';
    this.loadDatasets();
  }

  deleteDataset(id: string): void {
    this.datasetService.delete(id).subscribe({
      next: () => this.loadDatasets()
    });
  }

  private loadDatasets(): void {
    if (this.projectId) {
      this.datasetService.getByProject(this.projectId).subscribe({
        next: (ds) => this.datasets.set(ds)
      });
    }
  }
}
