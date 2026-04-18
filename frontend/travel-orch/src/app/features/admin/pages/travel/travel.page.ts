import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { AuthService } from '../../../auth/auth.service';
import { DestinationService } from '../../travel/destination.service';
import {
  DestinationResponse,
  DestinationCreateRequest,
  DestinationUpdateRequest,
} from '../../travel/destination.model';

@Component({
  selector: 'app-travel-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './travel.page.html',
  styleUrl: './travel.page.scss',
})
export class TravelPage {
  private readonly destinationService = inject(DestinationService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  readonly destinations = signal<DestinationResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');

  readonly showCreateModal = signal(false);
  readonly editingDestination = signal<DestinationResponse | null>(null);
  readonly deletingDestination = signal<DestinationResponse | null>(null);
  readonly isSubmitting = signal(false);

  readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(10000)]],
    country: ['', [Validators.required, Validators.maxLength(100)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    region: ['', [Validators.maxLength(100)]],
    latitude: [null],
    longitude: [null],
  });

  readonly filteredDestinations = computed(() => {
    const term = this.searchTerm().toLowerCase();
    if (!term) return this.destinations();
    return this.destinations().filter(
      (d) =>
        d.name.toLowerCase().includes(term) ||
        d.country.toLowerCase().includes(term) ||
        d.city.toLowerCase().includes(term) ||
        (d.region && d.region.toLowerCase().includes(term))
    );
  });

  readonly totalDestinations = computed(() => this.destinations().length);
  readonly countryCount = computed(
    () => new Set(this.destinations().map((d) => d.country)).size
  );
  readonly cityCount = computed(
    () => new Set(this.destinations().map((d) => d.city)).size
  );

  constructor() {
    if (!this.authService.isAuthenticated() || !this.authService.isAdmin()) {
      this.router.navigate(['/auth']);
      return;
    }
    this.loadDestinations();
  }

  loadDestinations(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.destinationService.getAll().subscribe({
      next: (destinations) => {
        this.destinations.set(destinations);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load destinations');
        this.toastService.error('Failed to load destinations');
      },
    });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
  }

  openCreateModal(): void {
    this.showCreateModal.set(true);
    this.form.reset();
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
    this.form.reset();
  }

  openEditModal(dest: DestinationResponse): void {
    this.editingDestination.set(dest);
    this.form.patchValue({
      name: dest.name,
      description: dest.description || '',
      country: dest.country,
      city: dest.city,
      region: dest.region || '',
      latitude: dest.latitude,
      longitude: dest.longitude,
    });
  }

  closeEditModal(): void {
    this.editingDestination.set(null);
    this.form.reset();
  }

  onSubmitCreate(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting.set(true);
    const data: DestinationCreateRequest = {
      name: this.form.value.name,
      description: this.form.value.description || undefined,
      country: this.form.value.country,
      city: this.form.value.city,
      region: this.form.value.region || undefined,
      latitude: this.form.value.latitude,
      longitude: this.form.value.longitude,
    };

    this.destinationService.create(data).subscribe({
      next: (created) => {
        this.isSubmitting.set(false);
        this.destinations.update((list) => [...list, created]);
        this.closeCreateModal();
        this.toastService.success(`Destination "${created.name}" created`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Failed to create destination');
      },
    });
  }

  onSubmitEdit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const dest = this.editingDestination();
    if (!dest) return;

    this.isSubmitting.set(true);
    const data: DestinationUpdateRequest = {
      name: this.form.value.name,
      description: this.form.value.description || undefined,
      country: this.form.value.country,
      city: this.form.value.city,
      region: this.form.value.region || undefined,
      latitude: this.form.value.latitude,
      longitude: this.form.value.longitude,
    };

    this.destinationService.update(dest.id, data).subscribe({
      next: (updated) => {
        this.isSubmitting.set(false);
        this.destinations.update((list) =>
          list.map((d) => (d.id === updated.id ? updated : d))
        );
        this.closeEditModal();
        this.toastService.success(`Destination "${updated.name}" updated`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Failed to update destination');
      },
    });
  }

  confirmDelete(dest: DestinationResponse): void {
    this.deletingDestination.set(dest);
  }

  cancelDelete(): void {
    this.deletingDestination.set(null);
  }

  onDeleteDestination(): void {
    const dest = this.deletingDestination();
    if (!dest) return;

    this.isSubmitting.set(true);
    this.destinationService.delete(dest.id).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.destinations.update((list) => list.filter((d) => d.id !== dest.id));
        this.deletingDestination.set(null);
        this.toastService.success(`Destination "${dest.name}" deleted`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Failed to delete destination');
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

  truncate(text: string | null, max: number): string {
    if (!text) return '—';
    return text.length > max ? text.substring(0, max) + '...' : text;
  }
}
