export interface FeedbackResponse {
  id: number;
  travelId: number;
  managerId: number;
  reviewerId: number;
  reviewerUsername: string;
  rating: number;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FeedbackCreateRequest {
  travelId: number;
  rating: number;
  comment?: string;
}

export interface FeedbackUpdateRequest {
  rating?: number;
  comment?: string;
}
