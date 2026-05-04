import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, NavbarComponent],
  templateUrl: './admin.page.html',
  styleUrl: './admin.page.scss',
})
export class AdminPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly navItems = [
    { label: 'Users', path: '/admin/users', icon: 'users' },
    { label: 'Destinations', path: '/admin/travel', icon: 'travel' },
    { label: 'Travels', path: '/admin/travels', icon: 'travels' },
    { label: 'Payments', path: '/admin/payments', icon: 'payments' },
    { label: 'Bookings', path: '/admin/bookings', icon: 'bookings' },
    { label: 'Settings', path: '/admin/settings', icon: 'settings' },
  ];

  constructor() {
    if (!this.authService.isAuthenticated() || !this.authService.isAdmin()) {
      this.router.navigate(['/auth']);
    }
  }
}
