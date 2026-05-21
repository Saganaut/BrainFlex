package cephadex.brainflex.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "themes")
public class Theme extends Auditable {

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

    /** Body font-family CSS string. Null = inherit the platform default. Applied by
     *  {@code useTheme.ts} on the root element after the design-system tokens. */
    private String fontFamily;

    /** Heading font-family CSS string. Null = inherit {@link #fontFamily} (or the
     *  platform default if that's null too). */
    private String headingFontFamily;

    /** Optional pointer to a {@code MediaAsset} (kind = AUDIO) bundling the answer-correct /
     *  answer-wrong sound effects. Resolves to null until chunk 19 ships the MediaAsset
     *  model; the runtime falls back to the platform-default SFX when this is null. */
    private String soundThemeId;

    /** Power-user CSS custom-property overrides applied verbatim on {@code :root}. Keys
     *  are the variable name (e.g. {@code "--radius-md"}), values the CSS value
     *  ({@code "0.5rem"}). Never null; empty by default. Applied after the design-system
     *  defaults so user overrides win. */
    private Map<String, String> tokenOverrides = new LinkedHashMap<>();
}
