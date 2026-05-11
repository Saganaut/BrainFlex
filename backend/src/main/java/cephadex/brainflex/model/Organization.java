package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "organizations")
public class Organization {

    @Id
    private String id;

    private String name;
    private String ownerId;

    /** Subscription plan held by this organization (seat plan). */
    private OrganizationPlan plan = new OrganizationPlan();

    private LocalDateTime createdAt = LocalDateTime.now();
}
