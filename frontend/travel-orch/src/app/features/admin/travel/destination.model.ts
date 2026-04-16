export interface DestinationResponse {
  id: number;
  name: string;
  description: string | null;
  country: string;
  city: string;
  region: string | null;
  latitude: number | null;
  longitude: number | null;
  imageBase64: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DestinationCreateRequest {
  name: string;
  description?: string;
  country: string;
  city: string;
  region?: string;
  latitude?: number;
  longitude?: number;
  imageBase64?: string;
}

export interface DestinationUpdateRequest {
  name?: string;
  description?: string;
  country?: string;
  city?: string;
  region?: string;
  latitude?: number;
  longitude?: number;
  imageBase64?: string;
}
