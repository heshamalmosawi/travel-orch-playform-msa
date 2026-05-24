package com.sayedhesham.travelorch.common.entity.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("TravelNode")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("travelId")
    private Long travelId;

    @Property("title")
    private String title;

    @Property("managerId")
    private Long managerId;

    @Property("totalPrice")
    private Double totalPrice;

    @Property("startDateEpochDay")
    private Long startDateEpochDay;

    @Property("status")
    private String status;

    @Relationship(type = "VISITS", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<CountryNode> countries = new ArrayList<>();
}
