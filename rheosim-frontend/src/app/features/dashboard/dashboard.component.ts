import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ProjectService } from '../../core/services/project.service';
import { AuthService } from '../../core/services/auth.service';
import { Project } from '../../core/models/project.model';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, DatePipe],
  template: `
    <div class="dashboard">
      <div class="dashboard-header">
        <h1>Dashboard</h1>
        <button class="btn btn-primary" (click)="showCreateForm.set(!showCreateForm())">
          {{ showCreateForm() ? 'Cancel' : '+ New Project' }}
        </button>
      </div>

      @if (showCreateForm()) {
        <div class="create-form">
          <form [formGroup]="createForm" (ngSubmit)="createProject()">
            <div class="form-row">
              <input formControlName="name" placeholder="Project name" class="input" />
              <input formControlName="description" placeholder="Description" class="input flex-1" />
              <button type="submit" class="btn btn-primary" [disabled]="createForm.invalid">Create</button>
            </div>
          </form>
        </div>
      }

      <div class="stats-grid">
        <div class="stat-card">
          <span class="stat-value">{{ projects().length }}</span>
          <span class="stat-label">Projects</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ activeProjects() }}</span>
          <span class="stat-label">Active</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ draftProjects() }}</span>
          <span class="stat-label">Drafts</span>
        </div>
      </div>

      <div class="projects-grid">
        @for (project of projects(); track project.id) {
          <div class="project-card">
            <div class="project-header">
              <h3>{{ project.name }}</h3>
              <span class="badge" [class]="'badge-' + project.status.toLowerCase()">{{ project.status }}</span>
            </div>
            <p class="project-desc">{{ project.description }}</p>
            <div class="project-meta">
              <span>{{ project.materialIds.length }} materials</span>
              <span>{{ project.createdAt | date }}</span>
            </div>
            <div class="project-actions">
              <a [routerLink]="['/datasets']" [queryParams]="{projectId: project.id}" class="btn btn-sm">Datasets</a>
              <a [routerLink]="['/simulation']" [queryParams]="{projectId: project.id}" class="btn btn-sm">Simulate</a>
            </div>
          </div>
        } @empty {
          <div class="empty-state">
            <p>No projects yet. Create your first project to get started.</p>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .dashboard { padding: 1.5rem; }
    .dashboard-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .dashboard-header h1 { color: #fff; margin: 0; }
    .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-bottom: 2rem; }
    .stat-card {
      background: #16213e;
      padding: 1.25rem;
      border-radius: 8px;
      text-align: center;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .stat-value { font-size: 2rem; font-weight: 700; color: #e94560; }
    .stat-label { font-size: 0.8rem; color: #a8a8b3; text-transform: uppercase; }
    .projects-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 1rem; }
    .project-card {
      background: #16213e;
      padding: 1.25rem;
      border-radius: 8px;
      border: 1px solid #2a2a4a;
      transition: border-color 0.2s;
    }
    .project-card:hover { border-color: #e94560; }
    .project-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .project-header h3 { color: #fff; margin: 0; font-size: 1.1rem; }
    .badge {
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-size: 0.7rem;
      text-transform: uppercase;
      font-weight: 600;
    }
    .badge-active { background: rgba(76, 175, 80, 0.2); color: #4caf50; }
    .badge-draft { background: rgba(255, 193, 7, 0.2); color: #ffc107; }
    .badge-archived { background: rgba(158, 158, 158, 0.2); color: #9e9e9e; }
    .project-desc { color: #a8a8b3; font-size: 0.85rem; margin-bottom: 0.75rem; }
    .project-meta { display: flex; justify-content: space-between; color: #6c6c80; font-size: 0.8rem; margin-bottom: 0.75rem; }
    .project-actions { display: flex; gap: 0.5rem; }
    .btn { padding: 0.375rem 0.75rem; border-radius: 4px; cursor: pointer; font-size: 0.8rem; border: 1px solid #e94560; color: #e94560; background: transparent; text-decoration: none; transition: all 0.2s; }
    .btn:hover { background: #e94560; color: #fff; }
    .btn-primary { background: #e94560; color: #fff; border: none; padding: 0.5rem 1rem; }
    .btn-primary:hover { background: #d63851; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-sm { padding: 0.25rem 0.5rem; font-size: 0.75rem; }
    .create-form { background: #16213e; padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; }
    .form-row { display: flex; gap: 0.75rem; align-items: center; }
    .input {
      padding: 0.5rem 0.75rem;
      background: #1a1a2e;
      border: 1px solid #2a2a4a;
      border-radius: 6px;
      color: #fff;
      font-size: 0.9rem;
    }
    .input:focus { outline: none; border-color: #e94560; }
    .flex-1 { flex: 1; }
    .empty-state { text-align: center; color: #a8a8b3; padding: 3rem; grid-column: 1 / -1; }
  `]
})
export class DashboardComponent implements OnInit {
  private projectService = inject(ProjectService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  projects = signal<Project[]>([]);
  showCreateForm = signal(false);

  createForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: ['']
  });

  activeProjects = () => this.projects().filter(p => p.status === 'ACTIVE').length;
  draftProjects = () => this.projects().filter(p => p.status === 'DRAFT').length;

  ngOnInit(): void {
    this.loadProjects();
  }

  createProject(): void {
    if (this.createForm.invalid) return;
    this.projectService.create(this.createForm.getRawValue()).subscribe({
      next: () => {
        this.showCreateForm.set(false);
        this.createForm.reset();
        this.loadProjects();
      }
    });
  }

  private loadProjects(): void {
    this.projectService.getAll().subscribe({
      next: (projects) => this.projects.set(projects)
    });
  }
}
