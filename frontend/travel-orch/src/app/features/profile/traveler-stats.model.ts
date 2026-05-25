export interface TravelerStatsResponse {
  totalTripsCompleted: number;
  totalCancellations: number;
  totalSpent: number;
  averageRatingGiven: number;
  totalReviewsGiven: number;
  totalReportsFiled: number;
  preferredPaymentMethods: Record<string, number>;
}
