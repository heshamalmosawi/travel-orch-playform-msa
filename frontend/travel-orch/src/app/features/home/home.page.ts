import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { AutocompleteComponent } from '../../shared/components/autocomplete/autocomplete.component';
import { TravelService } from '../admin/travel/travel.service';
import { TravelResponse } from '../admin/travel/travel.model';
import { DestinationResponse } from '../admin/travel/destination.model';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, NavbarComponent, AutocompleteComponent],
  templateUrl: './home.page.html',
  styleUrl: './home.page.scss',
})
export class HomePage {
  private readonly travelService = inject(TravelService);
  private readonly router = inject(Router);

  readonly packages = signal<TravelResponse[]>([]);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly searchTerm = signal('');

  readonly filteredPackages = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.packages();
    return this.packages().filter((pkg) => {
      const inTitle = pkg.title.toLowerCase().includes(term);
      const inDesc = !!pkg.description && pkg.description.toLowerCase().includes(term);
      const inDestinations = (pkg.destinations ?? []).some((d) => {
        const dest = d.destination;
        return (
          !!dest &&
          (dest.name.toLowerCase().includes(term) ||
            dest.city.toLowerCase().includes(term) ||
            dest.country.toLowerCase().includes(term))
        );
      });
      return inTitle || inDesc || inDestinations;
    });
  });

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

  onSearchTermChange(term: string): void {
    this.searchTerm.set(term);
  }

  onDestinationSelected(dest: DestinationResponse): void {
    this.searchTerm.set(dest.name);
  }

  openDetail(id: number): void {
    this.router.navigate(['/travels', id]);
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
