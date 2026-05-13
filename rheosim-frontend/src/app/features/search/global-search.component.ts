import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';

@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="global-search" [class.active]="isActive">
      <div class="search-input-wrapper">
        <svg class="search-icon" viewBox="0 0 24 24" width="20" height="20">
          <path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z" fill="currentColor"/>
        </svg>
        <input
          type="text"
          [(ngModel)]="query"
          (ngModelChange)="onQueryChange($event)"
          (focus)="isActive = true"
          (blur)="onBlur()"
          placeholder="Rechercher projets, matériaux, plugins..."
          class="search-input"
          autocomplete="off" />
        <kbd *ngIf="!isActive" class="shortcut">Ctrl+K</kbd>
      </div>

      <!-- Suggestions dropdown -->
      <div *ngIf="isActive && (suggestions.length > 0 || query.length > 2)" class="search-dropdown">
        <div *ngIf="suggestions.length > 0" class="suggestions">
          <div *ngFor="let suggestion of suggestions"
               class="suggestion-item"
               (mousedown)="selectSuggestion(suggestion)">
            {{ suggestion }}
          </div>
        </div>
        <div *ngIf="query.length > 2" class="search-action" (mousedown)="goToResults()">
          <span>Rechercher "{{ query }}" — Entrée</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .global-search { position: relative; width: 100%; max-width: 500px; }
    .search-input-wrapper {
      display: flex; align-items: center; gap: 8px;
      background: #f5f5f5; border-radius: 8px; padding: 8px 16px;
      border: 2px solid transparent; transition: all 0.2s;
    }
    .global-search.active .search-input-wrapper { background: #fff; border-color: #1976d2; box-shadow: 0 4px 12px rgba(25,118,210,0.15); }
    .search-icon { color: #888; flex-shrink: 0; }
    .search-input { flex: 1; border: none; background: transparent; outline: none; font-size: 14px; }
    .shortcut { background: #e0e0e0; padding: 2px 6px; border-radius: 3px; font-size: 11px; color: #666; }
    .search-dropdown {
      position: absolute; top: 100%; left: 0; right: 0; margin-top: 4px;
      background: #fff; border-radius: 8px; box-shadow: 0 8px 24px rgba(0,0,0,0.15);
      overflow: hidden; z-index: 1000;
    }
    .suggestion-item {
      padding: 10px 16px; cursor: pointer; font-size: 14px;
      transition: background 0.1s;
    }
    .suggestion-item:hover { background: #f0f7ff; }
    .search-action {
      padding: 12px 16px; border-top: 1px solid #eee;
      cursor: pointer; font-size: 13px; color: #1976d2;
    }
    .search-action:hover { background: #f0f7ff; }
  `]
})
export class GlobalSearchComponent implements OnInit, OnDestroy {
  query = '';
  suggestions: string[] = [];
  isActive = false;

  private searchSubject = new Subject<string>();

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(200),
      distinctUntilChanged(),
      switchMap(q => q.length >= 2
        ? this.http.get<string[]>(`/api/v1/search/suggest?q=${encodeURIComponent(q)}&limit=5`)
        : of([]))
    ).subscribe(suggestions => {
      this.suggestions = suggestions;
    });
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  onQueryChange(value: string): void {
    this.searchSubject.next(value);
  }

  selectSuggestion(suggestion: string): void {
    this.query = suggestion;
    this.goToResults();
  }

  goToResults(): void {
    this.isActive = false;
    this.router.navigate(['/search'], { queryParams: { q: this.query } });
  }

  onBlur(): void {
    setTimeout(() => { this.isActive = false; }, 200);
  }
}
