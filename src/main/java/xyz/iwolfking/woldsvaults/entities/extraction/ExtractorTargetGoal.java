package xyz.iwolfking.woldsvaults.entities.extraction;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Locks a mob's attack target onto a drilling extractor by UUID. Unlike the protect-villager goal
 * this never yields to nearby players: the extractor is the priority target while it drills.
 */
public class ExtractorTargetGoal extends Goal {
    private final Mob mob;
    private final UUID extractorId;

    public ExtractorTargetGoal(Mob mob, UUID extractorId) {
        this.mob = mob;
        this.extractorId = extractorId;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    private ExtractorEntity resolve() {
        if (!(this.mob.level instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.extractorId);
        return entity instanceof ExtractorEntity extractor && extractor.isAlive() && !extractor.isJobDone() ? extractor : null;
    }

    @Override
    public boolean canUse() {
        return this.resolve() != null;
    }

    @Override
    public boolean canContinueToUse() {
        ExtractorEntity extractor = this.resolve();
        return extractor != null && this.mob.getTarget() == extractor;
    }

    @Override
    public void start() {
        ExtractorEntity extractor = this.resolve();
        if (extractor != null) {
            this.mob.setTarget(extractor);
        }
    }

    @Override
    public void stop() {
        if (this.mob.getTarget() instanceof ExtractorEntity) {
            this.mob.setTarget(null);
        }
    }
}
