import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MarketplaceService, PluginResponse, PluginReview } from '../../core/services/marketplace.service';

@Component({
  selector: 'app-marketplace-detail',
  standalone: true,
  imports: [DecimalPipe, DatePipe, FormsModule],
  template: `
    <div class="detail-page">
      @if (plugin()) {
        <div class="detail-header">
          <div class="header-info">
            <h2>{{ plugin()!.name }}</h2>
            <div class="meta">
              <span class="license">{{ plugin()!.license || 'MIT' }}</span>
              <span class="version">{{ plugin()!.latestVersion || '1.0.0' }}</span>
              <span class="downloads">{{ plugin()!.downloadsCount }} downloads</span>
            </div>
          </div>
          <button class="btn-install" (click)="install()">Install Plugin</button>
        </div>

        <div class="detail-body">
          <div class="main-content">
            <section class="description">
              <h3>Description</h3>
              <p>{{ plugin()!.description }}</p>
            </section>

            <section class="reviews-section">
              <h3>Reviews ({{ plugin()!.ratingCount }})</h3>
              <div class="rating-summary">
                <span class="big-rating">{{ plugin()!.ratingAverage | number:'1.1-1' }}</span>
                <span class="stars">{{ getStars(plugin()!.ratingAverage) }}</span>
              </div>

              <div class="add-review">
                <h4>Leave a review</h4>
                <div class="star-input">
                  @for (i of [1,2,3,4,5]; track i) {
                    <button class="star-btn" [class.active]="newRating >= i" (click)="newRating = i">★</button>
                  }
                </div>
                <textarea class="review-input" [(ngModel)]="newComment" placeholder="Your review..."></textarea>
                <button class="btn-submit-review" (click)="submitReview()">Submit Review</button>
              </div>

              @for (review of reviews(); track review.id) {
                <div class="review-card">
                  <div class="review-header">
                    <span class="review-stars">{{ getStars(review.rating) }}</span>
                    <span class="review-date">{{ review.createdAt | date:'mediumDate' }}</span>
                  </div>
                  <p class="review-comment">{{ review.comment }}</p>
                </div>
              }
            </section>
          </div>

          <aside class="sidebar">
            <div class="sidebar-section">
              <h4>Tags</h4>
              <div class="tags">
                @for (tag of plugin()!.tags; track tag) {
                  <span class="tag">{{ tag }}</span>
                }
              </div>
            </div>
            <div class="sidebar-section">
              <h4>Author</h4>
              <p class="author">{{ plugin()!.authorName || 'Community' }}</p>
            </div>
          </aside>
        </div>
      }
    </div>
  `,
  styles: [`
    .detail-page { padding: 1.5rem; max-width: 1000px; margin: 0 auto; }
    .detail-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 1px solid #2a2a4a; }
    .header-info h2 { color: #fff; margin-bottom: 0.5rem; }
    .meta { display: flex; gap: 1rem; }
    .meta span { color: #a8a8b3; font-size: 0.8rem; background: #2a2a4a; padding: 0.2rem 0.6rem; border-radius: 4px; }
    .btn-install { background: #6c63ff; color: #fff; border: none; padding: 0.75rem 1.5rem; border-radius: 6px; font-size: 0.9rem; cursor: pointer; font-weight: 600; }
    .btn-install:hover { background: #5a52e0; }
    .detail-body { display: grid; grid-template-columns: 1fr 280px; gap: 2rem; }
    .main-content section { margin-bottom: 2rem; }
    .main-content h3 { color: #fff; font-size: 1rem; margin-bottom: 0.75rem; }
    .description p { color: #a8a8b3; line-height: 1.6; }
    .rating-summary { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1rem; }
    .big-rating { color: #fff; font-size: 2rem; font-weight: 700; }
    .stars { color: #ffc107; font-size: 1.2rem; }
    .add-review { background: #1a1a2e; padding: 1rem; border-radius: 8px; margin-bottom: 1rem; }
    .add-review h4 { color: #fff; font-size: 0.85rem; margin-bottom: 0.5rem; }
    .star-input { margin-bottom: 0.5rem; }
    .star-btn { background: none; border: none; font-size: 1.5rem; color: #2a2a4a; cursor: pointer; }
    .star-btn.active { color: #ffc107; }
    .review-input { width: 100%; background: #16213e; border: 1px solid #2a2a4a; border-radius: 4px; color: #fff; padding: 0.5rem; min-height: 60px; resize: vertical; margin-bottom: 0.5rem; box-sizing: border-box; }
    .btn-submit-review { background: #4caf50; color: #fff; border: none; padding: 0.4rem 1rem; border-radius: 4px; cursor: pointer; font-size: 0.8rem; }
    .review-card { border-bottom: 1px solid #2a2a4a; padding: 0.75rem 0; }
    .review-header { display: flex; justify-content: space-between; margin-bottom: 0.375rem; }
    .review-stars { color: #ffc107; font-size: 0.85rem; }
    .review-date { color: #6c6c80; font-size: 0.75rem; }
    .review-comment { color: #a8a8b3; font-size: 0.8rem; }
    .sidebar { }
    .sidebar-section { background: #16213e; padding: 1rem; border-radius: 8px; margin-bottom: 1rem; }
    .sidebar-section h4 { color: #fff; font-size: 0.85rem; margin-bottom: 0.5rem; }
    .tags { display: flex; gap: 0.375rem; flex-wrap: wrap; }
    .tag { background: rgba(108, 99, 255, 0.15); color: #6c63ff; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.7rem; }
    .author { color: #a8a8b3; font-size: 0.85rem; }
  `]
})
export class MarketplaceDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private marketplaceService = inject(MarketplaceService);

  plugin = signal<PluginResponse | null>(null);
  reviews = signal<PluginReview[]>([]);
  newRating = 0;
  newComment = '';

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug') || '';
    this.marketplaceService.getPlugin(slug).subscribe(p => {
      this.plugin.set(p);
      this.marketplaceService.getReviews(p.id).subscribe(r => this.reviews.set(r));
    });
  }

  install(): void {
    const p = this.plugin();
    if (p) {
      this.marketplaceService.downloadPlugin(p.id).subscribe();
    }
  }

  submitReview(): void {
    const p = this.plugin();
    if (p && this.newRating > 0) {
      this.marketplaceService.addReview(p.id, { rating: this.newRating, comment: this.newComment })
        .subscribe(review => {
          this.reviews.set([review, ...this.reviews()]);
          this.newRating = 0;
          this.newComment = '';
        });
    }
  }

  getStars(rating: number): string {
    const full = Math.floor(rating);
    const half = rating - full >= 0.5 ? 1 : 0;
    return '★'.repeat(full) + (half ? '½' : '') + '☆'.repeat(5 - full - half);
  }
}
