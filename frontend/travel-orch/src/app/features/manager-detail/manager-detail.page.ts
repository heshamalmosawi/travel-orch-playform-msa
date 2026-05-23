import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { TravelService } from '../admin/travel/travel.service';
import { TravelResponse, ManagerStatsResponse } from '../admin/travel/travel.model';
import { FeedbackService } from '../travel-detail/feedback.service';
import { FeedbackResponse } from '../travel-detail/feedback.model';
import { AdminService } from '../admin/admin.service';
import { UserResponse } from '../admin/admin.model';

@Component({
  selector: 'app-manager-detail-page',
  standalone: true,
  imports: [CommonModule, DecimalPipe, NavbarComponent, RouterLink],
  templateUrl: './manager-detail.page.html',
  styleUrl: './manager-detail.page.scss',
})
export class ManagerDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly adminService = inject(AdminService);
  private readonly travelService = inject(TravelService);
  private readonly feedbackService = inject(FeedbackService);

  readonly manager = signal<UserResponse | null>(null);
  readonly stats = signal<ManagerStatsResponse | null>(null);
  readonly upcomingPackages = signal<TravelResponse[]>([]);
  readonly feedbacks = signal<FeedbackResponse[]>([]);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly notFound = signal(false);

  readonly stars = [1, 2, 3, 4, 5];

  readonly managerFullName = computed(() => {
    const m = this.manager();
    if (!m) return '';
    const full = `${m.firstName ?? ''} ${m.lastName ?? ''}`.trim();
    return full || m.username;
  });

  readonly memberSince = computed(() => {
    const m = this.manager();
    return m ? this.formatDate(m.createdAt) : '';
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id || Number.isNaN(id)) {
      this.isLoading.set(false);
      this.notFound.set(true);
      return;
    }

    let pending = 4;
    const done = () => { if (--pending === 0) this.isLoading.set(false); };

    this.adminService.getUser(id).subscribe({
      next: (user) => { this.manager.set(user); done(); },
      error: (err) => {
        done();
        if (err.status === 404) this.notFound.set(true);
        else this.hasError.set(true);
      },
    });

    this.travelService.getManagerStats(id).subscribe({
      next: (s) => { this.stats.set(s); done(); },
      error: () => done(),
    });

    this.travelService.getUpcomingByManager(id).subscribe({
      next: (list) => { this.upcomingPackages.set(list); done(); },
      error: () => done(),
    });

    this.feedbackService.getForManager(id).subscribe({
      next: (list) => { this.feedbacks.set(list); done(); },
      error: () => done(),
    });
  }

  goBack(): void {
    this.router.navigate(['/']);
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

  truncate(text: string | null, max: number): string {
    if (!text) return '';
    return text.length > max ? text.substring(0, max) + '...' : text;
  }
}
