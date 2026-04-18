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
import { TravelService } from '../../travel/travel.service';
import { DestinationService } from '../../travel/destination.service';
import {
  TravelResponse,
  TravelCreateRequest,
  TravelDestinationCreateRequest,
  TravelUpdateRequest,
} from '../../travel/travel.model';
import { DestinationResponse } from '../../travel/destination.model';

@Component({
  selector: 'app-travels-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './travels.page.html',
  styleUrl: './travels.page.scss',
})
export class TravelsPage {
  private readonly travelService = inject(TravelService);
  private readonly destinationService = inject(DestinationService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  readonly travels = signal<TravelResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly statusFilter = signal<string>('');

  readonly editingTravel = signal<TravelResponse | null>(null);
  readonly deletingTravel = signal<TravelResponse | null>(null);
  readonly viewTravel = signal<TravelResponse | null>(null);
  readonly creatingTravel = signal(false);
  readonly availableDestinations = signal<DestinationResponse[]>([]);
  readonly pendingDestinations = signal<TravelDestinationCreateRequest[]>([]);
  readonly isSubmitting = signal(false);

  readonly editForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(10000)]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    durationDays: [null, [Validators.required, Validators.min(1)]],
    totalPrice: [null, [Validators.min(0)]],
    status: [''],
  });

  readonly createForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(10000)]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    durationDays: [null, [Validators.required, Validators.min(1)]],
    totalPrice: [null, [Validators.min(0)]],
    userId: [null, [Validators.required, Validators.min(1)]],
  });

  readonly filteredTravels = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const status = this.statusFilter();
    return this.travels().filter((t) => {
      const matchesSearch =
        !term ||
        t.title.toLowerCase().includes(term) ||
        (t.description && t.description.toLowerCase().includes(term)) ||
        t.id.toString().includes(term);
      const matchesStatus = !status || t.status === status;
      return matchesSearch && matchesStatus;
    });
  });

  readonly totalTravels = computed(() => this.travels().length);
  readonly draftCount = computed(() => this.travels().filter((t) => t.status === 'draft').length);
  readonly confirmedCount = computed(() => this.travels().filter((t) => t.status === 'confirmed').length);
  readonly completedCount = computed(() => this.travels().filter((t) => t.status === 'completed').length);

  readonly statusOptions = ['draft', 'planned', 'confirmed', 'in_progress', 'completed', 'cancelled'];

  constructor() {
    if (!this.authService.isAuthenticated() || !this.authService.isAdmin()) {
      this.router.navigate(['/auth']);
      return;
    }
    this.loadTravels();
  }

  loadTravels(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.travelService.getAll().subscribe({
      next: (travels) => {
        this.travels.set(travels);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load travels');
        this.toastService.error('Failed to load travels');
      },
    });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
  }

  onStatusFilter(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.statusFilter.set(select.value);
  }

  openEditModal(travel: TravelResponse): void {
    this.editingTravel.set(travel);
    this.editForm.patchValue({
      title: travel.title,
      description: travel.description || '',
      startDate: travel.startDate,
      endDate: travel.endDate,
      durationDays: travel.durationDays,
      totalPrice: travel.totalPrice,
      status: travel.status,
    });
  }

  closeEditModal(): void {
    this.editingTravel.set(null);
    this.editForm.reset();
  }

  onViewDetails(travel: TravelResponse): void {
    this.viewTravel.set(travel);
  }

  closeViewModal(): void {
    this.viewTravel.set(null);
  }

  onSaveEdit(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const travel = this.editingTravel();
    if (!travel) return;

    this.isSubmitting.set(true);
    const data: TravelUpdateRequest = {
      title: this.editForm.value.title,
      description: this.editForm.value.description || undefined,
      startDate: this.editForm.value.startDate,
      endDate: this.editForm.value.endDate,
      durationDays: this.editForm.value.durationDays,
      totalPrice: this.editForm.value.totalPrice,
      status: this.editForm.value.status || undefined,
    };

    this.travelService.update(travel.id, data).subscribe({
      next: (updated) => {
        this.isSubmitting.set(false);
        this.travels.update((list) =>
          list.map((t) => (t.id === updated.id ? updated : t))
        );
        this.closeEditModal();
        this.toastService.success(`Travel "${updated.title}" updated successfully`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Failed to update travel');
      },
    });
  }

  confirmDelete(travel: TravelResponse): void {
    this.deletingTravel.set(travel);
  }

  cancelDelete(): void {
    this.deletingTravel.set(null);
  }

  onDeleteTravel(): void {
    const travel = this.deletingTravel();
    if (!travel) return;

    this.isSubmitting.set(true);
    this.travelService.delete(travel.id).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.travels.update((list) => list.filter((t) => t.id !== travel.id));
        this.deletingTravel.set(null);
        this.toastService.success(`Travel "${travel.title}" deleted successfully`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Failed to delete travel');
      },
    });
  }

  openCreateModal(): void {
    this.creatingTravel.set(true);
    this.pendingDestinations.set([]);
    this.createForm.reset();
    this.destinationService.getAll().subscribe({
      next: (dests) => this.availableDestinations.set(dests),
      error: () => this.availableDestinations.set([]),
    });
  }

  closeCreateModal(): void {
    this.creatingTravel.set(false);
    this.createForm.reset();
    this.pendingDestinations.set([]);
  }

  addDestination(): void {
    this.pendingDestinations.update((list) => [
      ...list,
      {
        destinationId: 0,
        visitOrder: list.length + 1,
      },
    ]);
  }

  removeDestination(index: number): void {
    this.pendingDestinations.update((list) =>
      list.filter((_, i) => i !== index).map((d, i) => ({ ...d, visitOrder: i + 1 }))
    );
  }

  updatePendingDestination(index: number, field: string, value: any): void {
    this.pendingDestinations.update((list) =>
      list.map((d, i) => (i === index ? { ...d, [field]: value } : d))
    );
  }

  onCreateTravel(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const destinations = this.pendingDestinations().filter(
      (d) => d.destinationId > 0
    );
    const data: TravelCreateRequest = {
      title: this.createForm.value.title,
      description: this.createForm.value.description || undefined,
      startDate: this.createForm.value.startDate,
      endDate: this.createForm.value.endDate,
      durationDays: this.createForm.value.durationDays,
      totalPrice: this.createForm.value.totalPrice || undefined,
      userId: this.createForm.value.userId,
      destinations: destinations.length > 0 ? destinations : undefined,
    };

    this.travelService.create(data).subscribe({
      next: (created) => {
        this.isSubmitting.set(false);
        this.travels.update((list) => [created, ...list]);
        this.closeCreateModal();
        this.toastService.success(`Travel "${created.title}" created successfully`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Failed to create travel');
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
