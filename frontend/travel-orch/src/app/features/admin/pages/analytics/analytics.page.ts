import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService } from './analytics.service';
import {
  AnalyticsOverviewResponse,
  MonthlyIncomeResponse,
  ManagerRankingResponse,
  TravelRankingResponse,
} from './analytics.model';

@Component({
  selector: 'app-analytics-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './analytics.page.html',
  styleUrl: './analytics.page.scss',
})
export class AnalyticsPage implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);

  readonly overview = signal<AnalyticsOverviewResponse | null>(null);
  readonly income = signal<MonthlyIncomeResponse[]>([]);
  readonly managers = signal<PagedResponse<ManagerRankingResponse> | null>(null);
  readonly travels = signal<PagedResponse<TravelRankingResponse> | null>(null);

  readonly isLoadingOverview = signal(true);
  readonly isLoadingIncome = signal(true);
  readonly isLoadingManagers = signal(true);
  readonly isLoadingTravels = signal(true);

  readonly overviewError = signal<string | null>(null);
  readonly incomeError = signal<string | null>(null);
  readonly managersError = signal<string | null>(null);
  readonly travelsError = signal<string | null>(null);

  readonly managersPage = signal(0);
  readonly travelsPage = signal(0);
  private readonly pageSize = 5;
  readonly stars = [1, 2, 3, 4, 5];

  readonly maxIncome = computed(() => {
    const data = this.income();
    if (!data.length) return 0;
    return Math.max(...data.map((m) => m.totalIncome));
  });

  readonly managersTotalPages = computed(() => this.managers()?.totalPages ?? 0);
  readonly travelsTotalPages = computed(() => this.travels()?.totalPages ?? 0);

  ngOnInit(): void {
    this.loadOverview();
    this.loadIncome();
    this.loadManagers();
    this.loadTravels();
  }

  loadOverview(): void {
    this.isLoadingOverview.set(true);
    this.overviewError.set(null);
    this.analyticsService.getOverview().subscribe({
      next: (data) => {
        this.overview.set(data);
        this.isLoadingOverview.set(false);
      },
      error: (err) => {
        this.overviewError.set(err.message || 'Failed to load overview');
        this.isLoadingOverview.set(false);
      },
    });
  }

  private loadIncome(): void {
    this.isLoadingIncome.set(true);
    this.incomeError.set(null);
    this.analyticsService.getIncome(6).subscribe({
      next: (data) => {
        this.income.set(data);
        this.isLoadingIncome.set(false);
      },
      error: (err) => {
        this.incomeError.set(err.message || 'Failed to load income data');
        this.isLoadingIncome.set(false);
      },
    });
  }

  loadManagers(page?: number): void {
    const p = page ?? this.managersPage();
    this.managersPage.set(p);
    this.isLoadingManagers.set(true);
    this.managersError.set(null);
    this.analyticsService.getTopManagers(p, this.pageSize).subscribe({
      next: (data) => {
        this.managers.set(data);
        this.isLoadingManagers.set(false);
      },
      error: (err) => {
        this.managersError.set(err.message || 'Failed to load managers');
        this.isLoadingManagers.set(false);
      },
    });
  }

  loadTravels(page?: number): void {
    const p = page ?? this.travelsPage();
    this.travelsPage.set(p);
    this.isLoadingTravels.set(true);
    this.travelsError.set(null);
    this.analyticsService.getTopTravels(p, this.pageSize).subscribe({
      next: (data) => {
        this.travels.set(data);
        this.isLoadingTravels.set(false);
      },
      error: (err) => {
        this.travelsError.set(err.message || 'Failed to load travels');
        this.isLoadingTravels.set(false);
      },
    });
  }

  prevManagers(): void {
    if (this.managersPage() > 0) this.loadManagers(this.managersPage() - 1);
  }

  nextManagers(): void {
    if (this.managersPage() < this.managersTotalPages() - 1) this.loadManagers(this.managersPage() + 1);
  }

  prevTravels(): void {
    if (this.travelsPage() > 0) this.loadTravels(this.travelsPage() - 1);
  }

  nextTravels(): void {
    if (this.travelsPage() < this.travelsTotalPages() - 1) this.loadTravels(this.travelsPage() + 1);
  }

  barWidth(value: number): string {
    const max = this.maxIncome();
    if (max === 0) return '0%';
    return `${(value / max) * 100}%`;
  }

  scoreBadgeClass(score: number): string {
    if (score >= 70) return 'badge--green';
    if (score >= 40) return 'badge--orange';
    return 'badge--red';
  }

  statusClass(status: string): string {
    switch (status) {
      case 'draft': return 'badge--blue';
      case 'cancelled': return 'badge--red';
      default: return 'badge--green';
    }
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value);
  }
}

interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
