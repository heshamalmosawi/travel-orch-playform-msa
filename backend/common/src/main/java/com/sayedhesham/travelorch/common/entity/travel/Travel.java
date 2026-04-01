package com.sayedhesham.travelorch.common.entity.travel;

import com.sayedhesham.travelorch.common.entity.base.BaseEntity;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.TravelStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travels", indexes = {
    @Index(name = "idx_travels_user", columnList = "user_id"),
    @Index(name = "idx_travels_status", columnList = "status"),
    @Index(name = "idx_travels_dates", columnList = "start_date, end_date")
})
@Getter
@Setter
@NoArgsConstructor
public class Travel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TravelStatus status = TravelStatus.DRAFT;

    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("visitOrder ASC")
    private List<TravelDestination> destinations = new ArrayList<>();

    public void addDestination(TravelDestination travelDestination) {
        destinations.add(travelDestination);
        travelDestination.setTravel(this);
    }

    public void removeDestination(TravelDestination travelDestination) {
        destinations.remove(travelDestination);
        travelDestination.setTravel(null);
    }
}
