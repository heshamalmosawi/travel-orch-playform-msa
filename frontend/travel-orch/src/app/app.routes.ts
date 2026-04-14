import { Routes } from '@angular/router';

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
    path: 'admin',
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
        path: 'bookings',
        loadComponent: () =>
          import('./features/admin/pages/bookings/bookings.page').then(
            (m) => m.BookingsPage
          ),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/admin/pages/settings/settings.page').then(
            (m) => m.SettingsPage
          ),
      },
    ],
  },
];
