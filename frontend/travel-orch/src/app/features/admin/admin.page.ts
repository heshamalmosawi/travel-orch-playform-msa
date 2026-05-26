import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, NavbarComponent],
  templateUrl: './admin.page.html',
  styleUrl: './admin.page.scss',
})
export class AdminPage {
  readonly navItems = [
    { label: 'Analytics', path: '/admin/analytics', icon: 'analytics' },
    { label: 'Users', path: '/admin/users', icon: 'users' },
    { label: 'Destinations', path: '/admin/travel', icon: 'travel' },
    { label: 'Travels', path: '/admin/travels', icon: 'travels' },
    { label: 'Payments', path: '/admin/payments', icon: 'payments' },
    { label: 'Bookings', path: '/admin/bookings', icon: 'bookings' },
    { label: 'Reports', path: '/admin/reports', icon: 'reports' },
    { label: 'Settings', path: '/admin/settings', icon: 'settings' },
  ];
}
