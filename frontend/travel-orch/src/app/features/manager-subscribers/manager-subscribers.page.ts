import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { TravelService } from '../admin/travel/travel.service';
import { TravelResponse } from '../admin/travel/travel.model';
import { PurchaseService } from '../travel-detail/purchase.service';
import { PurchaseResponse } from '../travel-detail/purchase.model';
import { AdminService } from '../admin/admin.service';
import { UserResponse } from '../admin/admin.model';

@Component({
  selector: 'app-manager-subscribers-page',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './manager-subscribers.page.html',
  styleUrl: './manager-subscribers.page.scss',
})
export class ManagerSubscribersPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly travelService = inject(TravelService);
  private readonly purchaseService = inject(PurchaseService);
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);

  private readonly travelId = Number(this.route.snapshot.paramMap.get('id'));

  readonly travel = signal<TravelResponse | null>(null);
  readonly subscribers = signal<PurchaseResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly notFound = signal(false);

  // Profile modal
  readonly selectedProfile = signal<UserResponse | null>(null);
  readonly isProfileLoading = signal(false);
  readonly showProfileModal = signal(false);

  // Remove & refund modal
  readonly removeTarget = signal<PurchaseResponse | null>(null);
  readonly isSubmitting = signal(false);

  readonly totalSubscribers = computed(() => this.subscribers().length);
  readonly activeCount = computed(
    () => this.subscribers().filter((s) => s.status !== 'refunded' && s.status !== 'failed').length
  );
  readonly refundedCount = computed(
    () => this.subscribers().filter((s) => s.status === 'refunded').length
  );

  readonly profileFullName = computed(() => {
    const p = this.selectedProfile();
    if (!p) return '';
    const full = `${p.firstName ?? ''} ${p.lastName ?? ''}`.trim();
    return full || p.username;
  });

  constructor() {
    if (!this.travelId || Number.isNaN(this.travelId)) {
      this.isLoading.set(false);
      this.notFound.set(true);
      return;
    }
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.travelService.getById(this.travelId).subscribe({
      next: (t) => this.travel.set(t),
      error: (err) => {
        if (err.status === 404) this.notFound.set(true);
      },
    });

    this.purchaseService.getByTravel(this.travelId).subscribe({
      next: (list) => {
        this.subscribers.set(list);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 404) {
          this.notFound.set(true);
        } else if (err.status === 403) {
          this.errorMessage.set('You do not have access to this package’s subscribers.');
        } else {
          this.errorMessage.set('Failed to load subscribers');
          this.toastService.error('Failed to load subscribers');
        }
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/manager']);
  }

  // --- Traveler profile ---
  openProfile(sub: PurchaseResponse): void {
    if (sub.buyerId == null) return;
    this.selectedProfile.set(null);
    this.showProfileModal.set(true);
    this.isProfileLoading.set(true);
    this.adminService.getUser(sub.buyerId).subscribe({
      next: (user) => {
        this.selectedProfile.set(user);
        this.isProfileLoading.set(false);
      },
      error: () => {
        this.isProfileLoading.set(false);
        this.toastService.error('Could not load traveler profile');
        this.showProfileModal.set(false);
      },
    });
  }

  closeProfileModal(): void {
    this.showProfileModal.set(false);
    this.selectedProfile.set(null);
  }

  // --- Remove & refund ---
  canRemove(sub: PurchaseResponse): boolean {
    return sub.status !== 'refunded' && sub.status !== 'failed';
  }

  openRemoveModal(sub: PurchaseResponse): void {
    this.removeTarget.set(sub);
  }

  closeRemoveModal(): void {
    if (this.isSubmitting()) return;
    this.removeTarget.set(null);
  }

  confirmRemove(): void {
    const target = this.removeTarget();
    if (!target) return;
    this.isSubmitting.set(true);
    this.purchaseService.cancelRefund(target.id).subscribe({
      next: (updated) => {
        this.isSubmitting.set(false);
        this.subscribers.update((list) =>
          list.map((s) => (s.id === updated.id ? { ...s, ...updated } : s))
        );
        this.removeTarget.set(null);
        this.toastService.success('Traveler removed and refund issued');
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.removeTarget.set(null);
        if (err.status === 409) {
          this.toastService.error('This purchase has already been refunded');
        } else if (err.status === 403) {
          this.toastService.error('You are not allowed to remove this traveler');
        } else {
          this.toastService.error('Could not remove the traveler. Please try again.');
        }
      },
    });
  }

  // --- Display helpers ---
  buyerLabel(sub: PurchaseResponse): string {
    return sub.buyerName || sub.buyerUsername || `User #${sub.buyerId ?? '—'}`;
  }

  initials(name: string): string {
    const parts = name.trim().split(/\s+/);
    const first = parts[0]?.[0] ?? '';
    const second = parts.length > 1 ? parts[parts.length - 1][0] : '';
    return (first + second).toUpperCase() || '?';
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('T')[0].split('-').map(Number);
    return `${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}`;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      pending: 'badge-pending',
      processing: 'badge-pending',
      completed: 'badge-completed',
      failed: 'badge-failed',
      refunded: 'badge-refunded',
    };
    return map[status] || 'badge-pending';
  }

  getStatusLabel(status: string): string {
    return status.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }
}
