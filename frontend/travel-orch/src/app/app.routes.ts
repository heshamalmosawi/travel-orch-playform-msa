import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { managerGuard } from './core/guards/manager.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/home/home.page').then((m) => m.HomePage),
  },
  {
    path: 'auth',
    loadComponent: () =>
      import('./features/auth/auth.page').then((m) => m.AuthPage),
  },
  {
    path: 'travels/:id',
    loadComponent: () =>
      import('./features/travel-detail/travel-detail.page').then(
        (m) => m.TravelDetailPage
      ),
  },
  {
    path: 'managers/:id',
    loadComponent: () =>
      import('./features/manager-detail/manager-detail.page').then(
        (m) => m.ManagerDetailPage
      ),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/admin.page').then((m) => m.AdminPage),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'users',
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/admin/pages/users/users.page').then(
            (m) => m.UsersPage
          ),
      },
      {
        path: 'travel',
        loadComponent: () =>
          import('./features/admin/pages/travel/travel.page').then(
            (m) => m.TravelPage
          ),
      },
      {
        path: 'travels',
        loadComponent: () =>
          import('./features/admin/pages/travels/travels.page').then(
            (m) => m.TravelsPage
          ),
      },
      {
        path: 'bookings',
        loadComponent: () =>
          import('./features/admin/pages/bookings/bookings.page').then(
            (m) => m.BookingsPage
          ),
      },
      {
        path: 'payments',
        loadComponent: () =>
          import('./features/admin/pages/payments/payments.page').then(
            (m) => m.PaymentsPage
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./features/admin/pages/reports/reports.page').then(
            (m) => m.ReportsPage
          ),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/admin/pages/settings/settings.page').then(
            (m) => m.SettingsPage
          ),
      },
      {
        path: '**',
        redirectTo: 'users',
      },
    ],
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./features/profile/profile.page').then(
        (m) => m.ProfilePage
      ),
  },
  {
    path: 'manager',
    canActivate: [managerGuard],
    loadComponent: () =>
      import('./features/manager/manager.page').then(
        (m) => m.ManagerPage
      ),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
