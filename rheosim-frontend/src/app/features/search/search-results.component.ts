import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

interface SearchHit {
  id: string;
  index: string;
  score: number;
  source: Record<string, any>;
  highlights: Record<string, string[]>;
}

interface SearchResponse {
  hits: SearchHit[];
  totalHits: number;
  page: number;
  size: number;
  facets: Record<string, number>;
  hasNext: boolean;
}

@Component({
  selector: 'app-search-results',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="search-results-page">
      <div class="search-header">
        <h2>Résultats de recherche</h2>
        <p *ngIf="results">{{ results.totalHits }} résultat(s) pour "{{ query }}"</p>
      </div>

      <div class="search-layout">
        <!-- Facets sidebar -->
        <aside class="facets" *ngIf="results && results.facets">
          <h3>Type</h3>
          <div class="facet-list">
            <label class="facet-item" (click)="filterByType(null)"
                   [class.active]="!selectedType">
              <span>Tous</span>
              <span class="count">{{ results.totalHits }}</span>
            </label>
            <label *ngFor="let facet of facetEntries" class="facet-item"
                   (click)="filterByType(facet[0])"
                   [class.active]="selectedType === facet[0]">
              <span>{{ getTypeLabel(facet[0]) }}</span>
              <span class="count">{{ facet[1] }}</span>
            </label>
          </div>
        </aside>

        <!-- Results list -->
        <main class="results-list">
          <div *ngIf="loading" class="loading">Recherche en cours...</div>

          <div *ngIf="!loading && results && results.hits.length === 0" class="no-results">
            <p>Aucun résultat trouvé pour "{{ query }}"</p>
            <p>Essayez avec des termes différents ou moins spécifiques.</p>
          </div>

          <div *ngFor="let hit of results?.hits" class="result-item">
            <div class="result-type-badge" [attr.data-type]="getType(hit)">
              {{ getTypeLabel(hit.index) }}
            </div>
            <h3 class="result-title" [innerHTML]="getTitle(hit)"></h3>
            <p class="result-description" [innerHTML]="getDescription(hit)"></p>
            <div class="result-meta">
              <span *ngIf="hit.source['family']">Famille: {{ hit.source['family'] }}</span>
              <span *ngIf="hit.source['author']">Par: {{ hit.source['author'] }}</span>
              <span *ngIf="hit.source['status']">Statut: {{ hit.source['status'] }}</span>
              <span class="score">Score: {{ hit.score | number:'1.2-2' }}</span>
            </div>
          </div>

          <!-- Pagination -->
          <div *ngIf="results && results.totalHits > results.size" class="pagination">
            <button [disabled]="results.page === 0" (click)="changePage(results.page - 1)">
              Précédent
            </button>
            <span>Page {{ results.page + 1 }} / {{ totalPages }}</span>
            <button [disabled]="!results.hasNext" (click)="changePage(results.page + 1)">
              Suivant
            </button>
          </div>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .search-results-page { max-width: 1200px; margin: 0 auto; padding: 24px; }
    .search-header { margin-bottom: 24px; }
    .search-header h2 { margin: 0 0 4px; }
    .search-header p { color: #666; margin: 0; }
    .search-layout { display: grid; grid-template-columns: 220px 1fr; gap: 32px; }
    .facets h3 { font-size: 14px; text-transform: uppercase; color: #888; margin-bottom: 12px; }
    .facet-item {
      display: flex; justify-content: space-between; padding: 8px 12px;
      cursor: pointer; border-radius: 4px; margin-bottom: 4px;
    }
    .facet-item:hover, .facet-item.active { background: #e3f2fd; }
    .facet-item .count { color: #888; font-size: 12px; }
    .result-item {
      padding: 16px; border: 1px solid #eee; border-radius: 8px;
      margin-bottom: 12px; transition: box-shadow 0.2s;
    }
    .result-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    .result-type-badge {
      display: inline-block; padding: 2px 8px; border-radius: 12px;
      font-size: 11px; font-weight: 600; text-transform: uppercase;
      background: #e0e0e0; margin-bottom: 8px;
    }
    .result-type-badge[data-type="plugin"] { background: #e8f5e9; color: #2e7d32; }
    .result-type-badge[data-type="project"] { background: #e3f2fd; color: #1565c0; }
    .result-type-badge[data-type="material"] { background: #fff3e0; color: #e65100; }
    .result-title { margin: 0 0 8px; font-size: 18px; }
    .result-title :deep(mark) { background: #fff59d; padding: 0 2px; }
    .result-description { color: #555; margin: 0 0 8px; font-size: 14px; }
    .result-meta { display: flex; gap: 16px; font-size: 12px; color: #888; }
    .score { margin-left: auto; }
    .pagination { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 24px; }
    .pagination button { padding: 8px 16px; border: 1px solid #ccc; border-radius: 4px; cursor: pointer; }
    .pagination button:disabled { opacity: 0.5; cursor: not-allowed; }
    .loading, .no-results { text-align: center; padding: 48px; color: #888; }
  `]
})
export class SearchResultsComponent implements OnInit {
  query = '';
  selectedType: string | null = null;
  results: SearchResponse | null = null;
  loading = false;

  get totalPages(): number {
    if (!this.results) return 0;
    return Math.ceil(this.results.totalHits / this.results.size);
  }

  get facetEntries(): [string, number][] {
    if (!this.results?.facets) return [];
    return Object.entries(this.results.facets);
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.query = params['q'] || '';
      this.selectedType = params['type'] || null;
      if (this.query) this.search(0);
    });
  }

  search(page: number): void {
    this.loading = true;
    let url = `/api/v1/search?q=${encodeURIComponent(this.query)}&page=${page}&size=20`;
    if (this.selectedType) url += `&type=${this.selectedType}`;

    this.http.get<SearchResponse>(url).subscribe({
      next: (response) => {
        this.results = response;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  filterByType(type: string | null): void {
    this.selectedType = type;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { q: this.query, type },
      queryParamsHandling: 'merge'
    });
  }

  changePage(page: number): void {
    this.search(page);
  }

  getType(hit: SearchHit): string {
    if (hit.index?.includes('plugin')) return 'plugin';
    if (hit.index?.includes('project')) return 'project';
    if (hit.index?.includes('material')) return 'material';
    return 'unknown';
  }

  getTypeLabel(index: string): string {
    if (index?.includes('plugin')) return 'Plugin';
    if (index?.includes('project')) return 'Projet';
    if (index?.includes('material')) return 'Matériau';
    return index;
  }

  getTitle(hit: SearchHit): string {
    if (hit.highlights && hit.highlights['name']?.length) return hit.highlights['name'][0];
    return hit.source['name'] || 'Sans titre';
  }

  getDescription(hit: SearchHit): string {
    if (hit.highlights && hit.highlights['description']?.length) return hit.highlights['description'][0];
    return hit.source['description'] || '';
  }
}
