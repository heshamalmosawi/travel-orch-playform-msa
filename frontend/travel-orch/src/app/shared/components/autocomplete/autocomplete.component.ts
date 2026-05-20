import { Component, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, distinctUntilChanged, debounceTime, switchMap, tap } from 'rxjs';
import { DestinationService } from '../../../features/admin/travel/destination.service';
import type { DestinationResponse } from '../../../features/admin/travel/destination.model';

@Component({
  selector: 'app-autocomplete',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './autocomplete.component.html',
  styleUrl: './autocomplete.component.scss',
})
export class AutocompleteComponent {
  private readonly destinationService = inject(DestinationService);

  readonly placeholder = input('Search...');
  readonly selected = output<DestinationResponse>();
  readonly queryChange = output<string>();

  readonly query = signal('');
  readonly suggestions = signal<DestinationResponse[]>([]);
  readonly isOpen = signal(false);
  readonly isLoading = signal(false);
  readonly highlightedIndex = signal(-1);

  private readonly searchSubject = new Subject<string>();

  constructor() {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => this.isLoading.set(true)),
        switchMap((term) => {
          if (!term || term.length < 2) {
            this.suggestions.set([]);
            this.isOpen.set(false);
            this.isLoading.set(false);
            return [];
          }
          return this.destinationService.autocomplete(term);
        })
      )
      .subscribe({
        next: (results) => {
          this.suggestions.set(results);
          this.isOpen.set(results.length > 0);
          this.isLoading.set(false);
          this.highlightedIndex.set(-1);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });
  }

  onInput(value: string): void {
    this.query.set(value);
    this.queryChange.emit(value);
    this.searchSubject.next(value);
  }

  selectSuggestion(dest: DestinationResponse): void {
    this.query.set(dest.name);
    this.isOpen.set(false);
    this.selected.emit(dest);
  }

  onKeydown(event: KeyboardEvent): void {
    if (!this.isOpen()) return;

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.highlightedIndex.update((i) =>
          Math.min(i + 1, this.suggestions().length - 1)
        );
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.highlightedIndex.update((i) => Math.max(i - 1, 0));
        break;
      case 'Enter':
        event.preventDefault();
        const idx = this.highlightedIndex();
        if (idx >= 0 && idx < this.suggestions().length) {
          this.selectSuggestion(this.suggestions()[idx]);
        }
        break;
      case 'Escape':
        this.isOpen.set(false);
        break;
    }
  }

  onBlur(): void {
    setTimeout(() => this.isOpen.set(false), 200);
  }

  onFocus(): void {
    if (this.suggestions().length > 0) {
      this.isOpen.set(true);
    }
  }
}
