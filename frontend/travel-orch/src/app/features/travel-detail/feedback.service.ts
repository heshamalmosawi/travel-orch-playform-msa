import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FeedbackResponse,
  FeedbackCreateRequest,
  FeedbackUpdateRequest,
} from './feedback.model';

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getForTravel(travelId: number): Observable<FeedbackResponse[]> {
    return this.http.get<FeedbackResponse[]>(
      `${this.apiUrl}/api/travel/feedbacks?travelId=${travelId}`
    );
  }

  getForManager(managerId: number): Observable<FeedbackResponse[]> {
    return this.http.get<FeedbackResponse[]>(
      `${this.apiUrl}/api/travel/feedbacks?managerId=${managerId}`
    );
  }

  getByReviewer(userId: number): Observable<FeedbackResponse[]> {
    return this.http.get<FeedbackResponse[]>(
      `${this.apiUrl}/api/travel/feedbacks/user/${userId}`
    );
  }

  getMine(travelId: number): Observable<FeedbackResponse> {
    return this.http.get<FeedbackResponse>(
      `${this.apiUrl}/api/travel/feedbacks/me?travelId=${travelId}`
    );
  }

  create(req: FeedbackCreateRequest): Observable<FeedbackResponse> {
    return this.http.post<FeedbackResponse>(
      `${this.apiUrl}/api/travel/feedbacks`,
      req
    );
  }

  update(feedbackId: number, req: FeedbackUpdateRequest): Observable<FeedbackResponse> {
    return this.http.put<FeedbackResponse>(
      `${this.apiUrl}/api/travel/feedbacks/${feedbackId}`,
      req
    );
  }

  delete(feedbackId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/api/travel/feedbacks/${feedbackId}`
    );
  }
}
