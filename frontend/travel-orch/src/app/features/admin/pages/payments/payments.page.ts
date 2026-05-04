import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { AuthService } from '../../../auth/auth.service';
import { PaymentService } from '../../payment/payment.service';
import { PaymentTransactionResponse } from '../../payment/payment.model';

@Component({
  selector: 'app-payments-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payments.page.html',
  styleUrl: './payments.page.scss',
})
export class PaymentsPage {
  private readonly paymentService = inject(PaymentService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly transactions = signal<PaymentTransactionResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly statusFilter = signal('');

  readonly viewTransaction = signal<PaymentTransactionResponse | null>(null);

  readonly filteredTransactions = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const status = this.statusFilter();
    return this.transactions().filter((t) => {
      const matchesSearch =
        !term ||
        t.id.toString().includes(term) ||
        (t.paymentIntentId && t.paymentIntentId.toLowerCase().includes(term)) ||
        t.currency.toLowerCase().includes(term);
      const matchesStatus = !status || t.status === status;
      return matchesSearch && matchesStatus;
    });
  });

  readonly totalCount = computed(() => this.transactions().length);
  readonly pendingCount = computed(() =>
    this.transactions().filter((t) => t.status === 'pending').length
  );
  readonly completedCount = computed(() =>
    this.transactions().filter((t) => t.status === 'completed').length
  );
  readonly volumeByCurrency = computed(() => {
    const map = new Map<string, number>();
    for (const t of this.transactions()) {
      map.set(t.currency, (map.get(t.currency) ?? 0) + t.amount);
    }
    return map;
  });

  readonly statusOptions = ['pending', 'processing', 'completed', 'failed'];

  constructor() {
    if (!this.authService.isAuthenticated() || !this.authService.isAdmin()) {
      this.router.navigate(['/auth']);
      return;
    }
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.paymentService.getAll().subscribe({
      next: (transactions) => {
        this.transactions.set(transactions);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load transactions');
        this.toastService.error('Failed to load transactions');
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

  readonly isLoadingDetails = signal(false);

  onViewDetails(transaction: PaymentTransactionResponse): void {
    this.viewTransaction.set(transaction);
    this.isLoadingDetails.set(true);
    this.paymentService.getById(transaction.id).subscribe({
      next: (full) => {
        this.viewTransaction.set(full);
        this.isLoadingDetails.set(false);
      },
      error: () => {
        this.isLoadingDetails.set(false);
      },
    });
  }

  closeViewModal(): void {
    this.viewTransaction.set(null);
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

  formatAmount(amount: number, currency: string): string {
    const symbol = this.getCurrencySymbol(currency);
    return `${symbol}${amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency.toUpperCase()}`;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      pending: 'badge-pending',
      processing: 'badge-processing',
      completed: 'badge-completed',
      failed: 'badge-failed',
    };
    return map[status] || 'badge-pending';
  }

  getStatusLabel(status: string): string {
    return status.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  truncate(text: string | null, max: number): string {
    if (!text) return '';
    return text.length > max ? text.substring(0, max) + '...' : text;
  }

  getCurrencyVolumes(): { currency: string; formatted: string }[] {
    const entries: { currency: string; formatted: string }[] = [];
    this.volumeByCurrency().forEach((amount, currency) => {
      const symbol = this.getCurrencySymbol(currency);
      entries.push({
        currency,
        formatted: `${symbol}${amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency.toUpperCase()}`,
      });
    });
    return entries;
  }

  private getCurrencySymbol(currency: string): string {
    const symbols: Record<string, string> = {
      USD: '$', EUR: '\u20AC', GBP: '\u00A3', JPY: '\u00A5',
    };
    return symbols[currency.toUpperCase()] || '';
  }
}
