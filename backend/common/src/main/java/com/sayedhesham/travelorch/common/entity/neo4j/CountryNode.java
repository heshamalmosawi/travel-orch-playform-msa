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

@Node("Country")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;
}
