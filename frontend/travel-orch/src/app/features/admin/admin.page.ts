import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { AuthService } from '../auth/auth.service';
import { AdminService } from './admin.service';
import { UserResponse, UserUpdateRequest } from './admin.model';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './admin.page.html',
  styleUrl: './admin.page.scss',
})
export class AdminPage {
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  readonly users = signal<UserResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');

  readonly editingUser = signal<UserResponse | null>(null);
  readonly deletingUser = signal<UserResponse | null>(null);
  readonly isSubmitting = signal(false);

  readonly editForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.maxLength(20)]],
    dateOfBirth: [''],
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
      u.roles.some((r) => r.toUpperCase() === 'ADMIN')
    ).length
  );
  readonly recentCount = computed(() => {
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    return this.users().filter((u) => new Date(u.createdAt) >= thirtyDaysAgo)
      .length;
  });

  constructor() {
    if (!this.authService.isAuthenticated() || !this.authService.isAdmin()) {
      this.router.navigate(['/auth']);
      return;
    }
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
    this.editForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phone: user.phone || '',
      dateOfBirth: user.dateOfBirth ? user.dateOfBirth.split('T')[0] : '',
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

    this.adminService.updateUser(user.id, data).subscribe({
      next: (updated) => {
        this.isSubmitting.set(false);
        this.users.update((users) =>
          users.map((u) => (u.id === updated.id ? updated : u))
        );
        this.closeEditModal();
        this.toastService.success(`User "${updated.username}" updated successfully`);
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
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  getRoleBadgeClass(roles: string[]): string {
    if (roles.some((r) => r.toUpperCase() === 'ADMIN')) return 'badge-admin';
    return 'badge-user';
  }
}
