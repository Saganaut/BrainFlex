package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    /** One entry per ImageSize tier (xs/sm/md/lg/xl) for the theme logo.
     *  Empty when no logo has been uploaded. Never null. */
    private List<StoredImageVariant> logoVariants = new ArrayList<>();

    /** One entry per ImageSize tier (xs/sm/md/lg/xl) for the theme background.
     *  Empty when no background has been uploaded. Never null. */
    private List<StoredImageVariant> backgroundVariants = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
}
