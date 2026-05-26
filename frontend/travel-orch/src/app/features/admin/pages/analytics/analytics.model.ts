export interface AnalyticsOverviewResponse {
  totalIncome: number;
  totalBookings: number;
  totalManagers: number;
  totalOrganizedTravels: number;
  lastMonthIncome: number;
}

export interface MonthlyIncomeResponse {
  year: number;
  month: number;
  label: string;
  totalIncome: number;
  transactionCount: number;
}

export interface ManagerRankingResponse {
  managerId: number;
  name: string;
  email: string;
  organizedTravels: number;
  totalBookings: number;
  totalRevenue: number;
  averageRating: number;
  totalReviews: number;
  totalReports: number;
  performanceScore: number;
}

export interface TravelRankingResponse {
  travelId: number;
  title: string;
  managerName: string;
  bookings: number;
  revenue: number;
  averageRating: number;
  totalReviews: number;
  status: string;
  performanceScore: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
