import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-travel-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="placeholder-page">
      <div class="placeholder-content">
        <h2>Travel Management</h2>
        <p>Travel domain administration coming soon.</p>
      </div>
    </div>
  `,
  styles: [`
    .placeholder-page {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 400px;
    }
    .placeholder-content {
      text-align: center;
      h2 {
        font-family: 'Poppins', Arial, sans-serif;
        font-size: 1.5rem;
        font-weight: 600;
        color: #faf9f5;
        margin: 0 0 0.5rem;
      }
      p {
        font-family: 'Lora', Georgia, serif;
        font-size: 0.95rem;
        color: #b0aea5;
        margin: 0;
      }
    }
  `],
})
export class TravelPage {}
