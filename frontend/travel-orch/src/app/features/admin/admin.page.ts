import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './admin.page.html',
  styleUrl: './admin.page.scss',
})
export class AdminPage {}
