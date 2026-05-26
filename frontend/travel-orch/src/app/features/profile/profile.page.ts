import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { AuthService } from '../auth/auth.service';
import { ProfileService } from './profile.service';
import { AdminService } from '../admin/admin.service';
import { TravelerStatsResponse } from './traveler-stats.model';
import { UserResponse } from '../admin/admin.model';
import { FeedbackService } from '../travel-detail/feedback.service';
import { FeedbackResponse } from '../travel-detail/feedback.model';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, DecimalPipe, RouterLink, NavbarComponent],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.scss',
})
export class ProfilePage {
  private readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly adminService = inject(AdminService);
  private readonly feedbackService = inject(FeedbackService);
  private readonly router = inject(Router);

  readonly user = signal<UserResponse | null>(null);
  readonly stats = signal<TravelerStatsResponse | null>(null);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);

  readonly feedbacks = signal<FeedbackResponse[]>([]);
  readonly stars = [1, 2, 3, 4, 5];

  readonly reviewCount = computed(() => this.feedbacks().length);
  readonly averageRating = computed(() => {
    const list = this.feedbacks();
    if (list.length === 0) return 0;
    return list.reduce((sum, f) => sum + f.rating, 0) / list.length;
  });

  constructor() {
    this.loadProfile();
  }

  private loadProfile(): void {
    const username = this.authService.getUsername();
    if (!username) {
      this.isLoading.set(false);
      this.hasError.set(true);
      return;
    }

    this.profileService.getUserByUsername(username).subscribe({
      next: (user) => {
        this.user.set(user);
        if (user.role === 'travel_manager') {
          this.feedbackService.getForManager(user.id).subscribe({
            next: (list) => this.feedbacks.set(list),
            error: () => this.feedbacks.set([]),
          });
        }
        this.profileService.getStats(user.id).subscribe({
          next: (stats) => {
            this.stats.set(stats);
            this.isLoading.set(false);
          },
          error: () => {
            this.isLoading.set(false);
            this.hasError.set(true);
          },
        });
      },
      error: () => {
        this.isLoading.set(false);
        this.hasError.set(true);
      },
    });
  }

  goHome(): void {
    this.router.navigate(['/']);
  }

  formatCurrency(amount: number): string {
    return '$' + amount.toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('T')[0].split('-').map(Number);
    return `${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}`;
  }

  get paymentMethodsArray(): { name: string; count: number }[] {
    const methods = this.stats()?.preferredPaymentMethods;
    if (!methods) return [];
    return Object.entries(methods)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
  }
}
