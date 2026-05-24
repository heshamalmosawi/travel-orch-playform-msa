import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { AuthService } from '../auth/auth.service';
import { ProfileService } from './profile.service';
import { AdminService } from '../admin/admin.service';
import { TravelerStatsResponse } from './traveler-stats.model';
import { UserResponse } from '../admin/admin.model';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.scss',
})
export class ProfilePage {
  private readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly adminService = inject(AdminService);
  private readonly router = inject(Router);

  readonly user = signal<UserResponse | null>(null);
  readonly stats = signal<TravelerStatsResponse | null>(null);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);

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

  get paymentMethodsArray(): { name: string; count: number }[] {
    const methods = this.stats()?.preferredPaymentMethods;
    if (!methods) return [];
    return Object.entries(methods)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
  }
}
