import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { TravelService } from '../admin/travel/travel.service';
import {
  TravelResponse,
  TravelDestinationResponse,
} from '../admin/travel/travel.model';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../../shared/components/toast/toast.service';
import { PurchaseService } from './purchase.service';
import { PurchaseResponse } from './purchase.model';
import { FeedbackService } from './feedback.service';
import { FeedbackResponse } from './feedback.model';

const DAY_MS = 24 * 60 * 60 * 1000;

@Component({
  selector: 'app-travel-detail-page',
  standalone: true,
  imports: [CommonModule, NavbarComponent, RouterLink],
  templateUrl: './travel-detail.page.html',
  styleUrl: './travel-detail.page.scss',
})
export class TravelDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly travelService = inject(TravelService);
  private readonly authService = inject(AuthService);
  private readonly purchaseService = inject(PurchaseService);
  private readonly feedbackService = inject(FeedbackService);
  private readonly toastService = inject(ToastService);

  readonly travel = signal<TravelResponse | null>(null);
  readonly isLoading = signal(true);
  readonly notFound = signal(false);
  readonly hasError = signal(false);

  readonly myPurchase = signal<PurchaseResponse | null>(null);
  readonly showPurchaseModal = signal(false);
  readonly showCancelModal = signal(false);
  readonly isSubmitting = signal(false);

  readonly feedbacks = signal<FeedbackResponse[]>([]);
  readonly myFeedback = signal<FeedbackResponse | null>(null);
  readonly pendingRating = signal<number>(0);
  readonly editRating = signal<number>(0);
  readonly editMode = signal(false);
  readonly isSubmittingFeedback = signal(false);

  readonly stars = [1, 2, 3, 4, 5];

  readonly minDaysBeforeStart = 3;

  readonly daysUntilStart = computed<number>(() => {
    const t = this.travel();
    if (!t || !t.startDate) return Number.NEGATIVE_INFINITY;
    return Math.floor((this.dateOnlyUtcMs(t.startDate) - this.todayUtcMs()) / DAY_MS);
  });

  readonly hasActivePurchase = computed<boolean>(() => {
    const p = this.myPurchase();
    return !!p && p.status !== 'refunded';
  });

  readonly canPurchase = computed<boolean>(() => {
    const t = this.travel();
    return (
      this.authService.isTraveler() &&
      !this.hasActivePurchase() &&
      !!t &&
      t.totalPrice !== null &&
      t.totalPrice > 0 &&
      this.daysUntilStart() >= this.minDaysBeforeStart
    );
  });

  readonly purchaseClosed = computed<boolean>(() => {
    const t = this.travel();
    return (
      this.authService.isTraveler() &&
      !this.hasActivePurchase() &&
      !!t &&
      t.totalPrice !== null &&
      t.totalPrice > 0 &&
      this.daysUntilStart() < this.minDaysBeforeStart
    );
  });

  readonly canReview = computed<boolean>(() =>
    this.authService.isTraveler() &&
    this.myPurchase()?.status === 'completed' &&
    !this.myFeedback()
  );

  readonly orderedDestinations = computed<TravelDestinationResponse[]>(() => {
    const t = this.travel();
    if (!t) return [];
    return [...t.destinations].sort((a, b) => a.visitOrder - b.visitOrder);
  });

  readonly nights = computed<number>(() => {
    const t = this.travel();
    if (!t || !t.startDate || !t.endDate) return 0;
    const diff = Math.floor(
      (this.dateOnlyUtcMs(t.endDate) - this.dateOnlyUtcMs(t.startDate)) / DAY_MS
    );
    return diff > 0 ? diff : 0;
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id || Number.isNaN(id)) {
      this.isLoading.set(false);
      this.notFound.set(true);
      return;
    }

    this.travelService.getById(id).subscribe({
      next: (data) => {
        this.travel.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 404) {
          this.notFound.set(true);
        } else {
          this.hasError.set(true);
        }
      },
    });

    this.loadFeedbacks(id);

    if (this.authService.isAuthenticated()) {
      this.loadMyPurchase(id);
      this.loadMyFeedback(id);
    }
  }

  private loadMyPurchase(travelId: number): void {
    this.purchaseService.getMine().subscribe({
      next: (purchases) => {
        const active = purchases.find(
          (p) => p.travelId === travelId && p.status !== 'refunded'
        );
        this.myPurchase.set(active ?? null);
      },
      error: () => {},
    });
  }

  private loadFeedbacks(travelId: number): void {
    this.feedbackService.getForTravel(travelId).subscribe({
      next: (list) => this.feedbacks.set(list),
      error: () => {},
    });
  }

  private loadMyFeedback(travelId: number): void {
    this.feedbackService.getMine(travelId).pipe(
      catchError((err) => (err.status === 404 ? of(null) : of(null)))
    ).subscribe({
      next: (f) => {
        if (f) this.myFeedback.set(f);
      },
    });
  }

  setRating(value: number): void {
    this.pendingRating.set(value);
  }

  setEditRating(value: number): void {
    this.editRating.set(value);
  }

  submitReview(comment: string): void {
    const rating = this.pendingRating();
    if (!rating) {
      this.toastService.error('Please select a star rating before submitting.');
      return;
    }
    const t = this.travel();
    if (!t) return;

    this.isSubmittingFeedback.set(true);
    this.feedbackService.create({ travelId: t.id, rating, comment: comment.trim() || undefined }).subscribe({
      next: (feedback) => {
        this.isSubmittingFeedback.set(false);
        this.myFeedback.set(feedback);
        this.feedbacks.update((list) => [...list, feedback]);
        this.pendingRating.set(0);
        this.toastService.success('Your review was submitted.');
      },
      error: (err) => {
        this.isSubmittingFeedback.set(false);
        if (err.status === 409) {
          this.toastService.error('You have already reviewed this package.');
        } else if (err.status === 403) {
          this.toastService.error('You must have a completed purchase to leave a review.');
        } else {
          this.toastService.error('Could not submit your review. Please try again.');
        }
      },
    });
  }

  startEdit(): void {
    const f = this.myFeedback();
    if (!f) return;
    this.editRating.set(f.rating);
    this.editMode.set(true);
  }

  cancelEdit(): void {
    this.editMode.set(false);
  }

  saveEdit(comment: string): void {
    const f = this.myFeedback();
    if (!f) return;
    const rating = this.editRating();
    if (!rating) {
      this.toastService.error('Please select a rating.');
      return;
    }

    this.isSubmittingFeedback.set(true);
    this.feedbackService.update(f.id, { rating, comment: comment.trim() || undefined }).subscribe({
      next: (updated) => {
        this.isSubmittingFeedback.set(false);
        this.myFeedback.set(updated);
        this.feedbacks.update((list) =>
          list.map((item) => (item.id === updated.id ? updated : item))
        );
        this.editMode.set(false);
        this.toastService.success('Your review was updated.');
      },
      error: () => {
        this.isSubmittingFeedback.set(false);
        this.toastService.error('Could not update your review. Please try again.');
      },
    });
  }

  deleteReview(): void {
    const f = this.myFeedback();
    if (!f) return;

    this.isSubmittingFeedback.set(true);
    this.feedbackService.delete(f.id).subscribe({
      next: () => {
        this.isSubmittingFeedback.set(false);
        this.feedbacks.update((list) => list.filter((item) => item.id !== f.id));
        this.myFeedback.set(null);
        this.toastService.success('Your review was deleted.');
      },
      error: () => {
        this.isSubmittingFeedback.set(false);
        this.toastService.error('Could not delete your review. Please try again.');
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/']);
  }

  openPurchaseModal(): void {
    this.showPurchaseModal.set(true);
  }

  closePurchaseModal(): void {
    if (this.isSubmitting()) return;
    this.showPurchaseModal.set(false);
  }

  confirmPurchase(): void {
    const t = this.travel();
    if (!t) return;
    this.isSubmitting.set(true);
    this.purchaseService.purchase(t.id).subscribe({
      next: (purchase) => {
        this.isSubmitting.set(false);
        this.showPurchaseModal.set(false);
        this.myPurchase.set(purchase);
        this.toastService.success(`You purchased "${t.title}"`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.showPurchaseModal.set(false);
        if (err.status === 409) {
          this.toastService.error(
            'This package can no longer be purchased (it may be too close to departure or already purchased).'
          );
        } else {
          this.toastService.error('Could not complete the purchase. Please try again.');
        }
      },
    });
  }

  openCancelModal(): void {
    this.showCancelModal.set(true);
  }

  closeCancelModal(): void {
    if (this.isSubmitting()) return;
    this.showCancelModal.set(false);
  }

  confirmCancel(): void {
    const purchase = this.myPurchase();
    if (!purchase) return;
    this.isSubmitting.set(true);
    this.purchaseService.cancelRefund(purchase.id).subscribe({
      next: (updated) => {
        this.isSubmitting.set(false);
        this.showCancelModal.set(false);
        this.myPurchase.set(updated);
        this.toastService.success('Your purchase was cancelled and a refund was requested');
      },
      error: () => {
        this.isSubmitting.set(false);
        this.showCancelModal.set(false);
        this.toastService.error('Could not cancel the purchase. Please try again.');
      },
    });
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('T')[0].split('-').map(Number);
    return `${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}`;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      draft: 'badge-draft',
      planned: 'badge-planned',
      confirmed: 'badge-confirmed',
      in_progress: 'badge-progress',
      completed: 'badge-completed',
      cancelled: 'badge-cancelled',
    };
    return map[status] || 'badge-draft';
  }

  getStatusLabel(status: string): string {
    return status.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  private dateOnlyUtcMs(dateStr: string): number {
    const [y, m, d] = dateStr.split('T')[0].split('-').map(Number);
    return Date.UTC(y, m - 1, d);
  }

  private todayUtcMs(): number {
    const now = new Date();
    return Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
  }
}
