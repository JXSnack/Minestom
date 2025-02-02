package net.minestom.server.collision;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class EntityCollision {
    /**
     * Represents the result of a collision with an entity
     * @param collisionPoint
     * @param entity
     * @param face null if the collision is not with a face
     */
    public record EntityCollisionResult(
            @NotNull Point collisionPoint,
            @NotNull Entity entity,
            @Nullable BlockFace face,
            double percentage
    ) implements Comparable<EntityCollisionResult> {
        @Override
        public int compareTo(@NotNull EntityCollision.EntityCollisionResult o) {
            return Double.compare(percentage, o.percentage);
        }
    }

    static @NotNull List<EntityCollisionResult> checkCollision(@NotNull Instance instance, @NotNull BoundingBox boundingBox, @NotNull Point point, @NotNull Vec entityVelocity, double extendRadius, @NotNull Function<Entity, Boolean> entityFilter, @Nullable PhysicsResult physicsResult) {
        double minimumRes = physicsResult != null ? physicsResult.res().res : Double.MAX_VALUE;

        List<EntityCollisionResult> result = new ArrayList<>();

        var maxDistance = Math.pow(boundingBox.height() * boundingBox.height() + boundingBox.depth()/2 * boundingBox.depth()/2 + boundingBox.width()/2 * boundingBox.width()/2, 1/3.0);
        double projectileDistance = entityVelocity.length();

        for (Entity e : instance.getNearbyEntities(point, extendRadius + maxDistance + projectileDistance)) {
            SweepResult sweepResult = new SweepResult(minimumRes, 0, 0, 0, null, 0, 0, 0);

            if (!entityFilter.apply(e)) continue;
            if (!e.hasCollision()) continue;

            // Overlapping with entity, math can't be done we return the entity
            if (e.getBoundingBox().intersectBox(e.getPosition().sub(point), boundingBox)) {
                var p = Pos.fromPoint(point);
                result.add(new EntityCollisionResult(p, e, null, 0));
            }

            // Check collisions with entity
            e.getBoundingBox().intersectBoxSwept(point, entityVelocity, e.getPosition(), boundingBox, sweepResult);

            if (sweepResult.res < 1) {
                var p = Pos.fromPoint(point).add(entityVelocity.mul(sweepResult.res));
                result.add(new EntityCollisionResult(p, e, null, sweepResult.res));
            }
        }

        return result;
    }
}
