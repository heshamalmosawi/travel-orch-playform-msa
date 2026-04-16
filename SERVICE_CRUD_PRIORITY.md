# Service CRUD Implementation Priority

## Simplicity Ranking (Simplest → Most Complex)

| Rank | Service | Entity | Why |
|------|---------|--------|-----|
| 1 | `travel-service` | **Destination** | Single entity, no FK dependencies, simple fields (name, country, city, lat/lng). Repository already exists. Neo4j graph syncing via existing `DestinationNode`. |
| 2 | `payment-service` | **PaymentMethod** | Simple entity (provider enum, name, isTestMode, config). No FK to other tables. Payment processing (Stripe/PayPal) adds real complexity later. |
| 3 | `travel-service` | **Travel** | Core entity but has FK to User, joins to Destinations (TravelDestination join table), and status lifecycle management. |
| 4 | `travel-service` | **Activity/Accommodation/Transportation** | Each depends on Destination existing first. More entities to wire up. |
| 5 | `payment-service` | **PaymentTransaction** | Depends on Travel + PaymentMethod, has provider integration complexity. |

## Decision: Start with Destination

Foundation entity that everything else (Travel, Activity, Accommodation, Transportation) references.
