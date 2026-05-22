import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { TravelService } from '../admin/travel/travel.service';
import { DestinationService } from '../admin/travel/destination.service';
import {
  TravelResponse,
  TravelCreateRequest,
  TravelDestinationCreateRequest,
} from '../admin/travel/travel.model';
import {
  DestinationResponse,
  DestinationCreateRequest,
} from '../admin/travel/destination.model';

@Component({
  selector: 'app-manager-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './manager.page.html',
  styleUrl: './manager.page.scss',
})
export class ManagerPage {
  private readonly travelService = inject(TravelService);
  private readonly destinationService = inject(DestinationService);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  readonly travels = signal<TravelResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly statusFilter = signal<string>('');

  readonly creatingTravel = signal(false);
  readonly availableDestinations = signal<DestinationResponse[]>([]);
  readonly pendingDestinations = signal<TravelDestinationCreateRequest[]>([]);
  readonly isSubmitting = signal(false);

  readonly showCreateDestinationModal = signal(false);
  readonly isSubmittingDestination = signal(false);

  readonly viewTravel = signal<TravelResponse | null>(null);

  readonly createForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(10000)]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    durationDays: [null, [Validators.required, Validators.min(1)]],
    totalPrice: [null, [Validators.min(0)]],
  });

  readonly destinationForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(10000)]],
    country: ['', [Validators.required, Validators.maxLength(100)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    region: ['', [Validators.maxLength(100)]],
    latitude: [null],
    longitude: [null],
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
        this.errorMessage.set(err.error?.message || 'Failed to load travel packages');
        this.toastService.error('Failed to load travel packages');
      },
    });
  }

  onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  onStatusFilter(event: Event): void {
    this.statusFilter.set((event.target as HTMLSelectElement).value);
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
    this.showCreateDestinationModal.set(false);
    this.destinationForm.reset();
  }

  addDestination(): void {
    this.pendingDestinations.update((list) => [
      ...list,
      { destinationId: 0, visitOrder: list.length + 1 },
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
    const destinations = this.pendingDestinations().filter((d) => d.destinationId > 0);
    const data: TravelCreateRequest = {
      title: this.createForm.value.title,
      description: this.createForm.value.description || undefined,
      startDate: this.createForm.value.startDate,
      endDate: this.createForm.value.endDate,
      durationDays: this.createForm.value.durationDays,
      totalPrice: this.createForm.value.totalPrice || undefined,
      destinations: destinations.length > 0 ? destinations : undefined,
    };

    this.travelService.create(data).subscribe({
      next: (created) => {
        this.isSubmitting.set(false);
        this.travels.update((list) => [created, ...list]);
        this.closeCreateModal();
        this.toastService.success(`Travel package "${created.title}" created successfully`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Failed to create travel package');
      },
    });
  }

  openCreateDestinationModal(): void {
    this.showCreateDestinationModal.set(true);
    this.destinationForm.reset();
  }

  closeCreateDestinationModal(): void {
    this.showCreateDestinationModal.set(false);
    this.destinationForm.reset();
  }

  onSubmitCreateDestination(): void {
    if (this.destinationForm.invalid) {
      this.destinationForm.markAllAsTouched();
      return;
    }
    this.isSubmittingDestination.set(true);
    const data: DestinationCreateRequest = {
      name: this.destinationForm.value.name,
      description: this.destinationForm.value.description || undefined,
      country: this.destinationForm.value.country,
      city: this.destinationForm.value.city,
      region: this.destinationForm.value.region || undefined,
      latitude: this.destinationForm.value.latitude,
      longitude: this.destinationForm.value.longitude,
    };

    this.destinationService.create(data).subscribe({
      next: (created) => {
        this.isSubmittingDestination.set(false);
        this.availableDestinations.update((list) => [...list, created]);
        this.closeCreateDestinationModal();
        this.toastService.success(`Destination "${created.name}" created`);
      },
      error: (err) => {
        this.isSubmittingDestination.set(false);
        this.toastService.error(err.error?.message || 'Failed to create destination');
      },
    });
  }

  onViewDetails(travel: TravelResponse): void {
    this.viewTravel.set(travel);
  }

  closeViewModal(): void {
    this.viewTravel.set(null);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('T')[0].split('-').map(Number);
    const date = new Date(y, m - 1, d);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
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
