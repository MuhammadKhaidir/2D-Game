package Tamed;

import Entitas.Entity;
import Entitas.player;
import Main.GamePanel;

public final class MinionTargeting {

    private MinionTargeting() {}

    public static Entity findNearestTarget(GamePanel gp, Entity hunter) {
        if (gp == null || hunter == null || hunter.solidArea == null) return null;

        Entity nearest = null;
        double bestDistance = Double.MAX_VALUE;

        if (gp.player != null && !gp.player.isDead && gp.player.solidArea != null) {
            nearest = gp.player;
            bestDistance = distance(hunter, nearest);
        }

        if (gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
            for (Entity pawn : gp.deathTotemManager.getMinions()) {
                if (!(pawn instanceof GolemMinion) || pawn == hunter || pawn.solidArea == null) continue;
                GolemMinion golem = (GolemMinion) pawn;
                if (!golem.isAlive() || golem.life <= 0) continue;

                double d = distance(hunter, golem);
                if (d < bestDistance) {
                    bestDistance = d;
                    nearest = golem;
                }
            }
        }

        return nearest;
    }

    public static int centerX(Entity e) {
        return e.x + e.solidArea.x + e.solidArea.width / 2;
    }

    public static int centerY(Entity e) {
        return e.y + e.solidArea.y + e.solidArea.height / 2;
    }

    public static double distance(Entity a, Entity b) {
        return Math.hypot(centerX(a) - centerX(b), centerY(a) - centerY(b));
    }

    public static void applyDamage(GamePanel gp, Entity target, int damage, Entity attacker) {
        if (target instanceof player) {
            ((player) target).takeDamageFrom(damage, attacker);
        } else if (target instanceof GolemMinion) {
            ((GolemMinion) target).takeDamage(damage);
        }
    }
}
