export interface PurchaseResponse {
  id: number;
  amount: number;
  currency: string;
  status: string;
  providerTransactionId: string | null;
  paymentIntentId: string | null;
  travelId: number;
  buyerId: number | null;
  createdAt: string;
}
