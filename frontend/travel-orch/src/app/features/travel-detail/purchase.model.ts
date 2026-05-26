export interface PurchaseResponse {
  id: number;
  amount: number;
  currency: string;
  status: string;
  providerTransactionId: string | null;
  paymentIntentId: string | null;
  travelId: number;
  buyerId: number | null;
  buyerUsername?: string | null;
  buyerName?: string | null;
  buyerEmail?: string | null;
  createdAt: string;
}
