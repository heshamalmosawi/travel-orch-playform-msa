import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { TravelService } from '../admin/travel/travel.service';
import { TravelResponse } from '../admin/travel/travel.model';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './home.page.html',
  styleUrl: './home.page.scss',
})
export class HomePage {
  private readonly travelService = inject(TravelService);

  readonly packages = signal<TravelResponse[]>([]);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);

  constructor() {
    this.travelService.getUpcoming().subscribe({
      next: (data) => {
        this.packages.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.hasError.set(true);
      },
    });
  }

  formatDate(dateStr: string): string {
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
