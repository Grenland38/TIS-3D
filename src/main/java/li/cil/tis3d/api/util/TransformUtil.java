package li.cil.tis3d.api.util;

import li.cil.tis3d.api.machine.Face;
import li.cil.tis3d.api.machine.Port;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Utility class for coordinate transformation related operations.
 */
public final class TransformUtil {
    /**
     * Project a hit position on the surface of a block to a UV coordinate on
     * the that side.
     *
     * @param face   the face to project the hit onto.
     * @param hitPos the hit position to project, in block local coordinates.
     * @return the projected UV coordinate, with the Z component being 0.
     */
    public static Vector3d hitToUV(final Face face, final Vector3d hitPos) {
        switch (face) {
            case Y_NEG:
                return new Vector3d(1 - hitPos.x, hitPos.z, 0);
            case Y_POS:
                return new Vector3d(1 - hitPos.x, 1 - hitPos.z, 0);
            case Z_NEG:
                return new Vector3d(1 - hitPos.x, 1 - hitPos.y, 0);
            case Z_POS:
                return new Vector3d(hitPos.x, 1 - hitPos.y, 0);
            case X_NEG:
                return new Vector3d(hitPos.z, 1 - hitPos.y, 0);
            case X_POS:
                return new Vector3d(1 - hitPos.z, 1 - hitPos.y, 0);
        }
        return Vector3d.ZERO;
    }

    /**
     * Project a hit position on the surface of a block to a UV coordinate on
     * the that side, taking into account potential rotation of the block
     * around the Y axis (up being south).
     *
     * @param face   the face to project the hit onto.
     * @param facing the rotation of the block.
     * @param hitPos the hit position to project, in block local coordinates.
     * @return the projected UV coordinate, with the Z component being 0.
     * @see Face#fromDirection(Direction)
     * @see Port#fromDirection(Direction)
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public static Vector3d hitToUV(final Face face, final Port facing, final Vector3d hitPos) {
        final Vector3d uv = hitToUV(face, hitPos);
        switch (face) {
            case Y_NEG:
                switch (facing) {
                    case LEFT:
                        return new Vector3d(uv.y, 1 - uv.x, 0);
                    case RIGHT:
                        return new Vector3d(1 - uv.y, uv.x, 0);
                    case UP:
                        return uv;
                    case DOWN:
                        return new Vector3d(1 - uv.x, 1 - uv.y, 0);
                }
                break;
            case Y_POS:
                switch (facing) {
                    case LEFT:
                        return new Vector3d(1 - uv.y, uv.x, 0);
                    case RIGHT:
                        return new Vector3d(uv.y, 1 - uv.x, 0);
                    case UP:
                        return uv;
                    case DOWN:
                        return new Vector3d(1 - uv.x, 1 - uv.y, 0);
                }
                break;
        }
        return uv;
    }

    // --------------------------------------------------------------------- //

    private TransformUtil() {
    }
}
