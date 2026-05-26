export interface TravelDestinationResponse {
  id: number;
  destinationId: number;
  visitOrder: number;
  arrivalDate: string | null;
  departureDate: string | null;
  notes: string | null;
  destination: {
    id: number;
    name: string;
    country: string;
    city: string;
  } | null;
  createdAt: string;
}

export interface TravelResponse {
  id: number;
  title: string;
  description: string | null;
  startDate: string;
  endDate: string;
  totalPrice: number | null;
  status: string;
  managerId: number;
  managerName: string | null;
  destinations: TravelDestinationResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface TravelCreateRequest {
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  totalPrice?: number;
  managerId?: number;
  destinations?: TravelDestinationCreateRequest[];
}

export interface TravelDestinationCreateRequest {
  destinationId: number;
  visitOrder: number;
  arrivalDate?: string;
  departureDate?: string;
  notes?: string;
}

export interface TravelUpdateRequest {
  title?: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  totalPrice?: number;
  status?: string;
}

export interface ManagerStatsResponse {
  totalPackages: number;
  averageRating: number;
  totalReviews: number;
  totalReports: number;
}

export interface ManagerDashboardResponse {
  totalIncome: number;
  totalTravels: number;
  totalTravelers: number;
}

export interface MonthlyIncomeResponse {
  year: number;
  month: number;
  label: string;
  totalIncome: number;
  transactionCount: number;
}
