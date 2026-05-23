import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { TravelService } from '../admin/travel/travel.service';
import {
  TravelResponse,
  TravelDestinationResponse,
} from '../admin/travel/travel.model';

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

  readonly travel = signal<TravelResponse | null>(null);
  readonly isLoading = signal(true);
  readonly notFound = signal(false);
  readonly hasError = signal(false);

  readonly orderedDestinations = computed<TravelDestinationResponse[]>(() => {
    const t = this.travel();
    if (!t) return [];
    return [...t.destinations].sort((a, b) => a.visitOrder - b.visitOrder);
  });

  readonly nights = computed<number>(() => {
    const t = this.travel();
    if (!t || !t.startDate || !t.endDate) return 0;
    const s = new Date(t.startDate.split('T')[0]).getTime();
    const e = new Date(t.endDate.split('T')[0]).getTime();
    const diff = Math.round((e - s) / (1000 * 60 * 60 * 24));
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
}
