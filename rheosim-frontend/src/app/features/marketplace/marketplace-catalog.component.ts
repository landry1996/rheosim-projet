import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { MarketplaceService, PluginResponse } from '../../core/services/marketplace.service';

@Component({
  selector: 'app-marketplace-catalog',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  template: `
    <div class="marketplace-page">
      <div class="marketplace-header">
        <h2>Model Marketplace</h2>
        <p class="subtitle">Discover and share constitutive models for viscoelastic simulation</p>
      </div>

      <div class="search-bar">
        <input
          type="text"
          class="search-input"
          placeholder="Search plugins..."
          [(ngModel)]="searchQuery"
          (input)="onSearch()"
        />
        <div class="tag-filters">
          @for (tag of popularTags; track tag) {
            <button
              class="tag-chip"
              [class.active]="selectedTags().includes(tag)"
              (click)="toggleTag(tag)">
              {{ tag }}
            </button>
          }
        </div>
      </div>

      <div class="plugins-grid">
        @for (plugin of plugins(); track plugin.id) {
          <div class="plugin-card" (click)="openPlugin(plugin.slug)">
            <div class="card-header">
              <h3>{{ plugin.name }}</h3>
              <span class="license-badge">{{ plugin.license || 'MIT' }}</span>
            </div>
            <p class="card-description">{{ plugin.description }}</p>
            <div class="card-tags">
              @for (tag of plugin.tags; track tag) {
                <span class="tag">{{ tag }}</span>
              }
            </div>
            <div class="card-footer">
              <div class="rating">
                <span class="stars">{{ getStars(plugin.ratingAverage) }}</span>
                <span class="count">({{ plugin.ratingCount }})</span>
              </div>
              <div class="downloads">
                <span class="download-icon">&#8595;</span>
                {{ plugin.downloadsCount }}
              </div>
            </div>
          </div>
        }

        @if (plugins().length === 0 && !loading()) {
          <div class="empty-state">
            <p>No plugins found. Be the first to publish one!</p>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .marketplace-page { padding: 1.5rem; max-width: 1200px; margin: 0 auto; }
    .marketplace-header { margin-bottom: 1.5rem; }
    .marketplace-header h2 { color: #fff; margin-bottom: 0.25rem; }
    .subtitle { color: #a8a8b3; font-size: 0.9rem; }
    .search-bar { margin-bottom: 1.5rem; }
    .search-input {
      width: 100%;
      padding: 0.75rem 1rem;
      background: #1a1a2e;
      border: 1px solid #2a2a4a;
      border-radius: 8px;
      color: #fff;
      font-size: 0.9rem;
      margin-bottom: 0.75rem;
      box-sizing: border-box;
    }
    .search-input:focus { outline: none; border-color: #6c63ff; }
    .tag-filters { display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .tag-chip {
      background: #2a2a4a;
      color: #a8a8b3;
      border: none;
      padding: 0.3rem 0.75rem;
      border-radius: 16px;
      font-size: 0.75rem;
      cursor: pointer;
      transition: all 0.2s;
    }
    .tag-chip.active { background: #6c63ff; color: #fff; }
    .tag-chip:hover { background: #3a3a5a; }
    .plugins-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 1rem; }
    .plugin-card {
      background: #16213e;
      border: 1px solid #2a2a4a;
      border-radius: 8px;
      padding: 1.25rem;
      cursor: pointer;
      transition: border-color 0.2s, transform 0.2s;
    }
    .plugin-card:hover { border-color: #6c63ff; transform: translateY(-2px); }
    .card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.5rem; }
    .card-header h3 { color: #fff; font-size: 1rem; margin: 0; }
    .license-badge { background: #2a2a4a; color: #a8a8b3; padding: 0.15rem 0.5rem; border-radius: 4px; font-size: 0.7rem; }
    .card-description { color: #a8a8b3; font-size: 0.8rem; margin-bottom: 0.75rem; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .card-tags { display: flex; gap: 0.375rem; flex-wrap: wrap; margin-bottom: 0.75rem; }
    .tag { background: rgba(108, 99, 255, 0.15); color: #6c63ff; padding: 0.15rem 0.5rem; border-radius: 4px; font-size: 0.7rem; }
    .card-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 0.75rem; border-top: 1px solid #2a2a4a; }
    .rating { display: flex; align-items: center; gap: 0.25rem; }
    .stars { color: #ffc107; font-size: 0.8rem; }
    .count { color: #6c6c80; font-size: 0.75rem; }
    .downloads { color: #a8a8b3; font-size: 0.8rem; }
    .download-icon { margin-right: 0.25rem; }
    .empty-state { grid-column: 1 / -1; text-align: center; color: #6c6c80; padding: 3rem; }
  `]
})
export class MarketplaceCatalogComponent implements OnInit {
  private marketplaceService = inject(MarketplaceService);
  private router = inject(Router);

  plugins = signal<PluginResponse[]>([]);
  loading = signal(false);
  searchQuery = '';
  selectedTags = signal<string[]>([]);

  popularTags = ['viscoelastic', 'polymer', 'rubber', 'composite', 'metal', 'bio-material'];

  ngOnInit(): void {
    this.loadPlugins();
  }

  loadPlugins(): void {
    this.loading.set(true);
    this.marketplaceService.listPlugins().subscribe({
      next: (plugins) => {
        this.plugins.set(plugins);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onSearch(): void {
    if (this.searchQuery.length > 2 || this.selectedTags().length > 0) {
      this.marketplaceService.searchPlugins(this.searchQuery, this.selectedTags()).subscribe(
        plugins => this.plugins.set(plugins)
      );
    } else if (this.searchQuery.length === 0) {
      this.loadPlugins();
    }
  }

  toggleTag(tag: string): void {
    const current = this.selectedTags();
    if (current.includes(tag)) {
      this.selectedTags.set(current.filter(t => t !== tag));
    } else {
      this.selectedTags.set([...current, tag]);
    }
    this.onSearch();
  }

  openPlugin(slug: string): void {
    this.router.navigate(['/marketplace', slug]);
  }

  getStars(rating: number): string {
    const full = Math.floor(rating);
    const half = rating - full >= 0.5 ? 1 : 0;
    return '★'.repeat(full) + (half ? '½' : '') + '☆'.repeat(5 - full - half);
  }
}
