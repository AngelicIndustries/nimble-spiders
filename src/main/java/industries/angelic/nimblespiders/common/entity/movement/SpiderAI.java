package industries.angelic.nimblespiders.common.entity.movement;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;

import industries.angelic.nimblespiders.common.SpiderDebug;
import industries.angelic.nimblespiders.common.entity.mob.SpiderClimber;

/** Vanilla spider behavior expressed through the task AI used by surface navigation. */
public final class SpiderAI {

    private final EntityCreature entity;

    public SpiderAI(EntityCreature entity) {
        this.entity = entity;
        entity.tasks.addTask(1, new EntityAISwimming(entity));
        entity.tasks.addTask(3, new EntityAILeapAtTarget(entity, 0.4F) {

            @Override
            public boolean shouldExecute() {
                return SpiderClimber.get(entity).orientationNormal.y > 0.9 && super.shouldExecute();
            }
        });
        entity.tasks.addTask(4, new SpiderAttack());
        entity.tasks.addTask(5, new EntityAIWander(entity, 0.8));
        entity.tasks.addTask(6, new EntityAIWatchClosest(entity, EntityPlayer.class, 8));
        entity.tasks.addTask(6, new EntityAILookIdle(entity));
        entity.targetTasks.addTask(1, new EntityAIHurtByTarget(entity, false));
        entity.targetTasks.addTask(2, new SpiderTarget(EntityPlayer.class));
        entity.targetTasks.addTask(3, new SpiderTarget(EntityIronGolem.class));
    }

    /**
     * Vanilla's nearest-target task searches only 4 blocks up and down, and drops a target it hasn't seen for 3
     * seconds. A climber goes after targets above it and rarely sees them while climbing, so it searches as high as it
     * searches wide and keeps an unseen target while it still has a path to follow.
     */
    private class SpiderTarget extends EntityAITarget {

        private final Class<? extends EntityLivingBase> targetClass;
        private EntityLivingBase candidate;
        private int unseenTicks;

        SpiderTarget(Class<? extends EntityLivingBase> targetClass) {
            super(entity, true);
            this.targetClass = targetClass;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (entity.getBrightness(1) >= 0.5F) return false;
            double range = getTargetDistance(), nearest = range * range;
            candidate = null;
            for (Object found : entity.worldObj
                .getEntitiesWithinAABB(targetClass, entity.boundingBox.expand(range, range, range))) {
                EntityLivingBase target = (EntityLivingBase) found;
                double distance = entity.getDistanceSqToEntity(target);
                if (distance < nearest && isSuitableTarget(target, false)) {
                    nearest = distance;
                    candidate = target;
                }
            }
            return candidate != null;
        }

        @Override
        public void startExecuting() {
            entity.setAttackTarget(candidate);
            unseenTicks = 0;
            super.startExecuting();
        }

        @Override
        public boolean continueExecuting() {
            EntityLivingBase target = entity.getAttackTarget();
            if (target == null || !target.isEntityAlive()) return false;
            double range = getTargetDistance();
            if (entity.getDistanceSqToEntity(target) > range * range) return false;
            if (target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.disableDamage) return false;
            if (entity.getEntitySenses()
                .canSee(target)) unseenTicks = 0;
            else if (++unseenTicks > 60 && entity.getNavigator()
                .noPath()) {
                    SpiderClimber.get(entity).debug.event("TARGET_LOST reason=unseen_without_path");
                    return false;
                }
            return true;
        }
    }

    private class SpiderAttack extends EntityAIBase {

        private int attackCooldown, pathCooldown;

        SpiderAttack() {
            setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = entity.getAttackTarget();
            return target != null && target.isEntityAlive();
        }

        @Override
        public boolean continueExecuting() {
            if (entity.getBrightness(1) >= 0.5F && entity.getRNG()
                .nextInt(100) == 0) {
                SpiderClimber.get(entity).debug.event("TARGET_LOST reason=daylight");
                entity.setAttackTarget(null);
                return false;
            }
            return shouldExecute();
        }

        @Override
        public void resetTask() {
            SpiderClimber.get(entity).debug.event("ATTACK_TASK_STOP");
            entity.getNavigator()
                .clearPathEntity();
        }

        private String dropReason(SpiderClimber climber, EntityLivingBase target, double horizontalDistanceSq) {
            if (climber.isDropping()) return "already_dropping";
            if (!(climber.orientationNormal.y < -0.7)) return "not_on_ceiling";
            if (!(entity.posY > target.boundingBox.maxY)) return "not_above_target";
            if (!(horizontalDistanceSq < 16)) return "too_far_from_target";
            AdvancedWalkNodeProcessor processor = new AdvancedWalkNodeProcessor(entity);
            if (!processor.canDropTo(entity.posX, entity.posY, entity.posZ, target.boundingBox.minY))
                return "descent_blocked_or_over_drop_limit";
            if (!processor.canLandAt(entity.posX, target.boundingBox.minY, entity.posZ)) return "no_landing_surface";
            if (!processor.canReachAfterLanding(entity.posX, target.boundingBox.minY, entity.posZ, target))
                return "target_unreachable_after_landing";
            return "ready";
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = entity.getAttackTarget();
            entity.getLookHelper()
                .setLookPositionWithEntity(target, 30, 30);
            SpiderClimber climber = SpiderClimber.get(entity);
            double dx = target.posX - entity.posX, dz = target.posZ - entity.posZ;
            String dropReason = dropReason(climber, target, dx * dx + dz * dz);
            if ("ready".equals(dropReason)) {
                climber.debug.event("DROP_START target=" + SpiderDebug.describe(target));
                climber.dropFromSurface();
            }
            if (entity.ticksExisted % 20 == 0 && climber.debug.enabled()) climber.debug.event(
                "DROP_CHECK reason=" + dropReason
                    + " horizontalDistanceSq="
                    + (dx * dx + dz * dz)
                    + " feetGap="
                    + (entity.posY - target.boundingBox.minY)
                    + " targetTop="
                    + target.boundingBox.maxY);
            if (!climber.isDropping() && --pathCooldown <= 0) {
                entity.getNavigator()
                    .tryMoveToEntityLiving(target, 1);
                pathCooldown = 10 + entity.getRNG()
                    .nextInt(5);
            }
            if (attackCooldown > 0) attackCooldown--;
            if (entity.getDistanceSq(target.posX, target.boundingBox.minY, target.posZ) <= 4 + target.width
                && attackCooldown == 0
                && entity.getEntitySenses()
                    .canSee(target)) {
                attackCooldown = 20;
                boolean hit = entity.attackEntityAsMob(target);
                if (climber.debug.enabled())
                    climber.debug.event("ATTACK hit=" + hit + " target=" + SpiderDebug.describe(target));
            }
        }
    }
}
