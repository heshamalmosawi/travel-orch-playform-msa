package com.sayedhesham.travelorch.travel_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sayedhesham.travelorch.common.enums.TravelStatus;
import com.sayedhesham.travelorch.common.repository.neo4j.TravelGraphRepository;
import com.sayedhesham.travelorch.travel_service.dto.DestinationResponse;
import com.sayedhesham.travelorch.travel_service.dto.TravelDestinationResponse;
import com.sayedhesham.travelorch.travel_service.dto.TravelResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GraphSyncServiceTest {

    @Mock
    private TravelGraphRepository travelGraphRepository;

    @InjectMocks
    private GraphSyncService graphSyncService;

    private TravelResponse travelWithCountries() {
        DestinationResponse italy = DestinationResponse.builder().id(1L).name("Rome").country("Italy").build();
        DestinationResponse italyAgain = DestinationResponse.builder().id(2L).name("Milan").country("Italy").build();
        DestinationResponse france = DestinationResponse.builder().id(3L).name("Paris").country("France").build();

        return TravelResponse.builder()
                .id(100L)
                .title("Euro Trip")
                .managerId(20L)
                .totalPrice(new BigDecimal("4000.00"))
                .startDate(LocalDate.of(2026, 6, 1))
                .status(TravelStatus.confirmed)
                .destinations(List.of(
                        TravelDestinationResponse.builder().destination(italy).build(),
                        TravelDestinationResponse.builder().destination(italyAgain).build(),
                        TravelDestinationResponse.builder().destination(france).build()))
                .build();
    }

    @Test
    void syncTravel_UpsertsAndLinksDistinctCountries() {
        graphSyncService.syncTravel(travelWithCountries());

        verify(travelGraphRepository).upsertTravel(100L, "Euro Trip", 20L, 4000.0,
                LocalDate.of(2026, 6, 1).toEpochDay(), "confirmed");
        verify(travelGraphRepository).clearVisits(100L);
        verify(travelGraphRepository).linkCountries(eq(100L), eq(List.of("Italy", "France")));
    }

    @Test
    void syncTravel_NoDestinations_DoesNotLinkCountries() {
        TravelResponse travel = TravelResponse.builder()
                .id(101L)
                .title("Empty")
                .managerId(20L)
                .totalPrice(new BigDecimal("100.00"))
                .startDate(LocalDate.of(2026, 7, 1))
                .status(TravelStatus.draft)
                .destinations(List.of())
                .build();

        graphSyncService.syncTravel(travel);

        verify(travelGraphRepository).upsertTravel(eq(101L), any(), any(), any(), any(), any());
        verify(travelGraphRepository).clearVisits(101L);
        verify(travelGraphRepository, never()).linkCountries(anyLong(), any());
    }

    @Test
    void syncTravel_RepositoryThrows_DoesNotPropagate() {
        doThrow(new RuntimeException("neo4j down"))
                .when(travelGraphRepository).upsertTravel(any(), any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> graphSyncService.syncTravel(travelWithCountries()));
    }

    @Test
    void recordReview_RepositoryThrows_DoesNotPropagate() {
        doThrow(new RuntimeException("neo4j down"))
                .when(travelGraphRepository).recordReview(anyLong(), anyLong(), any());

        assertDoesNotThrow(() -> graphSyncService.recordReview(10L, 100L, 5));
    }

    @Test
    void recordReview_NullUser_Skips() {
        graphSyncService.recordReview(null, 100L, 5);
        verify(travelGraphRepository, never()).recordReview(any(), any(), any());
    }

    @Test
    void removeReview_NullTravel_Skips() {
        graphSyncService.removeReview(10L, null);
        verify(travelGraphRepository, never()).deleteReview(any(), any());
    }

    @Test
    void removeTravel_RepositoryThrows_DoesNotPropagate() {
        doThrow(new RuntimeException("neo4j down")).when(travelGraphRepository).deleteTravel(anyLong());

        assertDoesNotThrow(() -> graphSyncService.removeTravel(100L));
    }
}
