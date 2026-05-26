import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { AdminService } from '../../admin.service';
import { UserResponse, UserUpdateRequest, RoleUpdateRequest } from '../../admin.model';
import { PurchaseService } from '../../../travel-detail/purchase.service';
import { PurchaseResponse } from '../../../travel-detail/purchase.model';
import { FeedbackService } from '../../../travel-detail/feedback.service';
import { FeedbackResponse } from '../../../travel-detail/feedback.model';
import { TravelService } from '../../travel/travel.service';

@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './users.page.html',
  styleUrl: './users.page.scss',
})
export class UsersPage {
  private readonly adminService = inject(AdminService);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);
  private readonly purchaseService = inject(PurchaseService);
  private readonly feedbackService = inject(FeedbackService);
  private readonly travelService = inject(TravelService);

  readonly users = signal<UserResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');

  readonly editingUser = signal<UserResponse | null>(null);
  readonly deletingUser = signal<UserResponse | null>(null);
  readonly isSubmitting = signal(false);

  // User-details (travel history + feedbacks) modal
  readonly viewingUser = signal<UserResponse | null>(null);
  readonly userPurchases = signal<PurchaseResponse[]>([]);
  readonly userFeedbacks = signal<FeedbackResponse[]>([]);
  readonly isDetailLoading = signal(false);
  readonly detailError = signal<string | null>(null);
  readonly purchasesPage = signal(0);
  readonly feedbacksPage = signal(0);
  readonly pageSize = 5;
  readonly stars = [1, 2, 3, 4, 5];

  // Resolved travel titles, keyed by travelId (purchases/feedbacks only carry the id)
  readonly travelNames = signal<Record<number, string>>({});

  private detailVersion = 0;

  readonly pagedPurchases = computed(() => {
    const start = this.purchasesPage() * this.pageSize;
    return this.userPurchases().slice(start, start + this.pageSize);
  });
  readonly purchasesPageCount = computed(() =>
    Math.max(1, Math.ceil(this.userPurchases().length / this.pageSize))
  );
  readonly pagedFeedbacks = computed(() => {
    const start = this.feedbacksPage() * this.pageSize;
    return this.userFeedbacks().slice(start, start + this.pageSize);
  });
  readonly feedbacksPageCount = computed(() =>
    Math.max(1, Math.ceil(this.userFeedbacks().length / this.pageSize))
  );

  // Available roles: admins can't promote to ADMIN, only to TRAVEL_MANAGER or USER
  readonly availableRoles = ['user', 'travel_manager'];

  readonly editForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.maxLength(20)]],
    dateOfBirth: [''],
    role: ['', [Validators.required]],
  });

  readonly filteredUsers = computed(() => {
    const term = this.searchTerm().toLowerCase();
    if (!term) return this.users();
    return this.users().filter(
      (u) =>
        u.firstName.toLowerCase().includes(term) ||
        u.lastName.toLowerCase().includes(term) ||
        u.username.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term)
    );
  });

  readonly totalUsers = computed(() => this.users().length);
  readonly adminCount = computed(() =>
    this.users().filter((u) =>
      u.role?.toUpperCase() === 'ADMIN'
    ).length
  );
  readonly recentCount = computed(() => {
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    return this.users().filter((u) => new Date(u.createdAt) >= thirtyDaysAgo)
      .length;
  });

  constructor() {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.adminService.getUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err.error?.message || 'Failed to load users'
        );
        this.toastService.error('Failed to load users');
      },
    });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
  }

  openEditModal(user: UserResponse): void {
    this.editingUser.set(user);
    const userRole = user.role ? user.role.toLowerCase() : 'user';
    this.editForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phone: user.phone || '',
      dateOfBirth: user.dateOfBirth ? user.dateOfBirth.split('T')[0] : '',
      role: userRole,
    });
  }

  closeEditModal(): void {
    this.editingUser.set(null);
    this.editForm.reset();
  }

  onSaveEdit(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const user = this.editingUser();
    if (!user) return;

    this.isSubmitting.set(true);
    const data: UserUpdateRequest = {
      firstName: this.editForm.value.firstName,
      lastName: this.editForm.value.lastName,
      email: this.editForm.value.email,
      phone: this.editForm.value.phone || undefined,
      dateOfBirth: this.editForm.value.dateOfBirth || undefined,
    };

    const newRole = this.editForm.value.role?.toLowerCase();
    const currentRole = user.role ? user.role.toLowerCase() : 'user';
    const roleChanged = newRole && newRole !== currentRole;

    // First update user details
    this.adminService.updateUser(user.id, data).subscribe({
      next: (updated) => {
        // If role changed, update role separately
        if (roleChanged) {
          const roleUpdateData: RoleUpdateRequest = {
            role: newRole,
          };
          this.adminService.updateUserRole(user.id, roleUpdateData).subscribe({
            next: (updatedWithRole) => {
              this.isSubmitting.set(false);
              this.users.update((users) =>
                users.map((u) => (u.id === updatedWithRole.id ? updatedWithRole : u))
              );
              this.closeEditModal();
              this.toastService.success(`User "${updatedWithRole.username}" updated successfully`);
            },
            error: () => {
              this.isSubmitting.set(false);
              this.users.update((users) =>
                users.map((u) => (u.id === updated.id ? updated : u))
              );
              this.closeEditModal();
              this.toastService.warning(
                `User details saved, but role update failed. Please try updating the role separately.`
              );
            },
          });
        } else {
          this.isSubmitting.set(false);
          this.users.update((users) =>
            users.map((u) => (u.id === updated.id ? updated : u))
          );
          this.closeEditModal();
          this.toastService.success(`User "${updated.username}" updated successfully`);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(
          err.error?.message || 'Failed to update user'
        );
      },
    });
  }

  confirmDelete(user: UserResponse): void {
    this.deletingUser.set(user);
  }

  cancelDelete(): void {
    this.deletingUser.set(null);
  }

  onDeleteUser(): void {
    const user = this.deletingUser();
    if (!user) return;

    this.isSubmitting.set(true);
    this.adminService.deleteUser(user.id).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.users.update((users) => users.filter((u) => u.id !== user.id));
        this.deletingUser.set(null);
        this.toastService.success(`User "${user.username}" deleted successfully`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(
          err.error?.message || 'Failed to delete user'
        );
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

  getRoleBadgeClass(role: string): string {
    return role?.toUpperCase() === 'ADMIN' ? 'badge-admin' : 'badge-user';
  }

  openDetailModal(user: UserResponse): void {
    const version = ++this.detailVersion;

    this.viewingUser.set(user);
    this.userPurchases.set([]);
    this.userFeedbacks.set([]);
    this.purchasesPage.set(0);
    this.feedbacksPage.set(0);
    this.detailError.set(null);
    this.travelNames.set({});
    this.isDetailLoading.set(true);

    let pending = 2;
    const done = () => {
      if (--pending === 0) this.isDetailLoading.set(false);
    };

    this.purchaseService.getByUser(user.id).subscribe({
      next: (list) => {
        if (this.detailVersion !== version) return;
        this.userPurchases.set(list);
        this.loadTravelNames(list.map((p) => p.travelId), version);
        done();
      },
      error: (err) => {
        if (this.detailVersion !== version) return;
        done();
        if (err.status === 403) {
          this.detailError.set("You do not have permission to view this user's data.");
        } else if (err.status !== 404) {
          this.toastService.error('Failed to load travel history');
        }
      },
    });

    this.feedbackService.getByReviewer(user.id).subscribe({
      next: (list) => {
        if (this.detailVersion !== version) return;
        this.userFeedbacks.set(list);
        this.loadTravelNames(list.map((f) => f.travelId), version);
        done();
      },
      error: (err) => {
        if (this.detailVersion !== version) return;
        done();
        if (err.status === 403) {
          this.detailError.set("You do not have permission to view this user's data.");
        } else if (err.status !== 404) {
          this.toastService.error('Failed to load feedbacks');
        }
      },
    });
  }

  closeDetailModal(): void {
    this.detailVersion++;
    this.viewingUser.set(null);
    this.userPurchases.set([]);
    this.userFeedbacks.set([]);
    this.detailError.set(null);
  }

  nextPurchasesPage(): void {
    if (this.purchasesPage() < this.purchasesPageCount() - 1) {
      this.purchasesPage.update((p) => p + 1);
    }
  }

  prevPurchasesPage(): void {
    if (this.purchasesPage() > 0) {
      this.purchasesPage.update((p) => p - 1);
    }
  }

  nextFeedbacksPage(): void {
    if (this.feedbacksPage() < this.feedbacksPageCount() - 1) {
      this.feedbacksPage.update((p) => p + 1);
    }
  }

  prevFeedbacksPage(): void {
    if (this.feedbacksPage() > 0) {
      this.feedbacksPage.update((p) => p - 1);
    }
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      pending: 'badge-pending',
      processing: 'badge-pending',
      completed: 'badge-completed',
      failed: 'badge-failed',
      refunded: 'badge-refunded',
    };
    return map[status] || 'badge-pending';
  }

  getStatusLabel(status: string): string {
    return status.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  private loadTravelNames(travelIds: Array<number | null | undefined>, version?: number): void {
    const ids = Array.from(
      new Set(travelIds.filter((id): id is number => id != null))
    );
    for (const id of ids) {
      if (this.travelNames()[id] !== undefined) continue;
      this.travelService.getById(id).subscribe({
        next: (t) => {
          if (version != null && this.detailVersion !== version) return;
          this.travelNames.update((m) => ({ ...m, [id]: t.title }));
        },
        error: () => {},
      });
    }
  }

  travelLabel(travelId: number): string {
    return this.travelNames()[travelId] ?? `Travel #${travelId}`;
  }
}
