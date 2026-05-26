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
import { AuthService } from '../auth/auth.service';
import { ReportService } from './report.service';
import { ToastService } from '../../shared/components/toast/toast.service';

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
  private readonly authService = inject(AuthService);
  private readonly reportService = inject(ReportService);
  private readonly toastService = inject(ToastService);

  readonly manager = signal<UserResponse | null>(null);
  readonly stats = signal<ManagerStatsResponse | null>(null);
  readonly upcomingPackages = signal<TravelResponse[]>([]);
  readonly feedbacks = signal<FeedbackResponse[]>([]);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly notFound = signal(false);

  readonly canReport = computed(() => this.authService.isTraveler());
  readonly hasReported = signal(false);
  readonly showReportModal = signal(false);
  readonly reportReason = signal('');
  readonly isSubmittingReport = signal(false);

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

  openReportModal(): void {
    this.reportReason.set('');
    this.showReportModal.set(true);
  }

  closeReportModal(): void {
    this.showReportModal.set(false);
  }

  onReasonInput(event: Event): void {
    this.reportReason.set((event.target as HTMLTextAreaElement).value);
  }

  submitReport(): void {
    const mgr = this.manager();
    if (!mgr) return;

    this.isSubmittingReport.set(true);
    const reason = this.reportReason().trim();
    this.reportService.create({ managerId: mgr.id, reason: reason || undefined }).subscribe({
      next: () => {
        this.isSubmittingReport.set(false);
        this.hasReported.set(true);
        this.closeReportModal();
        this.stats.update((s) => (s ? { ...s, totalReports: s.totalReports + 1 } : s));
        this.toastService.success('Report submitted');
      },
      error: (err) => {
        this.isSubmittingReport.set(false);
        if (err.status === 409) {
          this.hasReported.set(true);
          this.closeReportModal();
          this.toastService.error('You have already reported this manager');
        } else {
          this.toastService.error(err.error?.message || 'Failed to submit report');
        }
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

  truncate(text: string | null, max: number): string {
    if (!text) return '';
    return text.length > max ? text.substring(0, max) + '...' : text;
  }
}
