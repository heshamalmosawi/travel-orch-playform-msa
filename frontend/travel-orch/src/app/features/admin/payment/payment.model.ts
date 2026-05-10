export interface PaymentTransactionResponse {
  id: number;
  amount: number;
  currency: string;
  status: string;
  providerTransactionId: string | null;
  paymentIntentId: string | null;
  travelId: number;
  createdAt: string;
}
