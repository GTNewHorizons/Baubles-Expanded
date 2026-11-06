package baubles.api.expanded;

import com.github.bsideup.jabel.Desugar;

import java.util.Objects;

/**
 * Immutable slot policy. Empty icons and equal orders preserve the legacy presentation.
 * Icons name standalone textures, for example "example:textures/gui/ring.png".
 */
@Desugar
public record BaubleSlotType(String id, int order, String icon, boolean cosmetic, boolean renderToggle,
        boolean nativeGui, boolean enabled, int stackLimit, boolean fitsUniversal) {

    public BaubleSlotType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(icon, "icon");
        if (id.isEmpty() || id.length() > 256) {
            throw new IllegalArgumentException("Invalid slot type: " + id);
        }
        if (icon.length() > 512 || !icon.isEmpty() && !icon.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid slot icon: " + icon);
        }
        if (stackLimit < 1) throw new IllegalArgumentException("Slot stack limit must be positive");
    }

    public static BaubleSlotType defaults(String id) {
        return new BaubleSlotType(id, 0, "", true, true, true, true, 1, false);
    }
}
