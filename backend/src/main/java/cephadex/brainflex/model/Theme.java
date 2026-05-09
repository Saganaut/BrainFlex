package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "themes")
public class Theme {

    @Id
    private String id;

    private String name;
    private String ownerId;

    /** Null = private. Set to an org id to share with all org members. */
    private String organizationId;

    /** Hue angle 0–360 for --hue-primary. */
    private int huePrimary = 260;

    /** Hue angle 0–360 for --hue-accent. */
    private int hueAccent = 25;

    /** "light" | "dark" | "system" */
    private String mode = "system";

    private String backgroundImageUrl;
    private String logoImageUrl;

    private LocalDateTime createdAt = LocalDateTime.now();
}
