import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
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

const DAY_MS = 24 * 60 * 60 * 1000;

@Component({
  selector: 'app-travel-detail-page',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './travel-detail.page.html',
  styleUrl: './travel-detail.page.scss',
})
export class TravelDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly travelService = inject(TravelService);
  private readonly authService = inject(AuthService);
  private readonly purchaseService = inject(PurchaseService);
  private readonly toastService = inject(ToastService);

  readonly travel = signal<TravelResponse | null>(null);
  readonly isLoading = signal(true);
  readonly notFound = signal(false);
  readonly hasError = signal(false);

  readonly myPurchase = signal<PurchaseResponse | null>(null);
  readonly showPurchaseModal = signal(false);
  readonly showCancelModal = signal(false);
  readonly isSubmitting = signal(false);

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

    if (this.authService.isAuthenticated()) {
      this.loadMyPurchase(id);
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
      error: () => {
        // Non-blocking: viewing the package shouldn't fail if purchases can't load.
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

  // Treats a date as a calendar date pinned to UTC midnight so day-count math is
  // timezone/DST-independent and consistent across nights() and daysUntilStart().
  private dateOnlyUtcMs(dateStr: string): number {
    const [y, m, d] = dateStr.split('T')[0].split('-').map(Number);
    return Date.UTC(y, m - 1, d);
  }

  private todayUtcMs(): number {
    const now = new Date();
    return Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
  }
}
