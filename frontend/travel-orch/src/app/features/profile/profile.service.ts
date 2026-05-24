import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TravelerStatsResponse } from './traveler-stats.model';
import { UserResponse } from '../admin/admin.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getStats(userId: number): Observable<TravelerStatsResponse> {
    return this.http.get<TravelerStatsResponse>(
      `${this.apiUrl}/api/user/users/${userId}/stats`
    );
  }

  getUserByUsername(username: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(
      `${this.apiUrl}/api/user/users/username/${username}`
    );
  }

  getMyStats(username: string): Observable<TravelerStatsResponse> {
    return this.getUserByUsername(username).pipe(
      switchMap((user) => this.getStats(user.id))
    );
  }
}
