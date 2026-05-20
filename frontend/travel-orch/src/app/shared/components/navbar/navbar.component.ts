import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/auth/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  isAdmin = this.authService.isAdmin();
  isTravelManager = this.authService.isTravelManager();
  isAuthenticated = this.authService.isAuthenticated();

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth']);
  }

  goHome(): void {
    this.router.navigate(['/']);
  }

  goToAdmin(): void {
    this.router.navigate(['/admin']);
  }

  goToManager(): void {
    this.router.navigate(['/manager']);
  }

  goToLogin(): void {
    this.router.navigate(['/auth']);
  }
}
