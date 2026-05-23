import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { ReportService } from '../../../manager-detail/report.service';
import { ReportResponse } from '../../../manager-detail/report.model';

@Component({
  selector: 'app-reports-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports.page.html',
  styleUrl: './reports.page.scss',
})
export class ReportsPage {
  private readonly reportService = inject(ReportService);
  private readonly toastService = inject(ToastService);

  readonly reports = signal<ReportResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly totalReports = computed(() => this.reports().length);

  constructor() {
    this.loadReports();
  }

  loadReports(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.reportService.getAll().subscribe({
      next: (reports) => {
        this.reports.set(reports);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load reports');
        this.toastService.error('Failed to load reports');
      },
    });
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('T')[0].split('-').map(Number);
    const date = new Date(y, m - 1, d);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
}
