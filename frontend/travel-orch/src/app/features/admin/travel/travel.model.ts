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
  durationDays: number;
  totalPrice: number | null;
  status: string;
  userId: number;
  destinations: TravelDestinationResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface TravelCreateRequest {
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  durationDays: number;
  totalPrice?: number;
  userId: number;
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
  durationDays?: number;
  totalPrice?: number;
  status?: string;
}
