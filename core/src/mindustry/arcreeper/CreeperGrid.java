package mindustry.arcreeper;

import arc.func.Floatf;
import arc.math.Mathf;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.world.Tile;

import java.util.Objects;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;
import static mindustry.arcreeper.CreeperCombat.*;

public class CreeperGrid {
    private static final int GRID_SIZE = 4;

    public CreeperNode root = null;

    public CreeperGrid() {
    }

    public void update() {
        rebuildRoot();
    }

    private void rebuildRoot() {
        int size = Math.max(world.width(), world.height());
        int gridSize = Mathf.pow(GRID_SIZE, Mathf.ceil(Mathf.log(GRID_SIZE, size)));

        root = new CreeperNode(0, 0, gridSize);
        root.build();
    }

    public CreeperTarget findNearestTarget(Team team, float x, float y, float range) {
        return findTarget(team, x, y, range, node -> -node.dst2ToPoint(x, y));
    }

    public CreeperTarget findHighestTarget(Team team, float x, float y, float range) {
        return findTarget(team, x, y, range, node -> node.getCreeperByTeam(team) - node.dst2ToPoint(x, y) / 6400);
    }

    public CreeperTarget findTarget(Team team, float x, float y, float range, Floatf<CreeperNode> evaluator) {
        if (root == null) return null;

        CreeperNode.Result result = root.findBest(team, x, y, range, evaluator, Float.NEGATIVE_INFINITY);

        return result == null ? null : result.target;
    }

    public static class CreeperNode {
        public final int x, y;
        public final int size;

        public float maxC;
        public float maxAC;

        public CreeperNode[] children;

        CreeperNode(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }

        public boolean isInWorld() {
            return (x + size >= 0 || x < world.width()) && (y + size >= 0 || y < world.height());
        }

        public float dst2ToPoint(float targetX, float targetY) {
            if (targetX >= x && targetX <= x + size && targetY >= y && targetY <= y + size) {
                return 0f;
            }
            float nearestX = Mathf.clamp(targetX, x, x + size);
            float nearestY = Mathf.clamp(targetY, y, y + size);
            return Mathf.dst2(nearestX, nearestY, targetX, targetY);
        }

        public float getCreeperByTeam(Team team) {
            if (team == CreeperCore.antiCreeperTeam) {
                return maxC;
            } else if (team == CreeperCore.creeperTeam) {
                return maxAC;
            }
            return Math.max(maxC, maxAC);
        }

        public void build() {
            maxC = 0f;
            maxAC = 0f;

            if (size <= 1) {
                Tile tile = world.tile(x, y);
                if (tile != null) {
                    maxC = Math.max(0f, tile.creeper);
                    maxAC = Math.max(0f, -tile.creeper);
                }
                return;
            }

            int childSize = size / GRID_SIZE;

            children = new CreeperNode[GRID_SIZE * GRID_SIZE];

            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    int childX = x + i * childSize;
                    int childY = y + j * childSize;

                    children[i * GRID_SIZE + j] = buildChild(childX, childY, childSize);
                }
            }
        }

        CreeperNode buildChild(int childX, int childY, int childSize) {

            CreeperNode child = new CreeperNode(childX, childY, childSize);
            if (!child.isInWorld()) {
                return null;
            }

            child.build();
            if (child.maxC == 0f && child.maxAC == 0f) {
                return null;
            }

            maxC = Math.max(maxC, child.maxC);
            maxAC = Math.max(maxAC, child.maxAC);

            return child;
        }

        public Result findBest(Team team, float targetX, float targetY, float range, Floatf<CreeperNode> evaluator, float best) {
            if (children == null) {

                CreeperTarget target = new CreeperTarget(world.tile(x, y));
                if (invalidateTarget(target, team, targetX * tilesize, targetY * tilesize, range * tilesize)) {
                    return null;
                }
                return new Result(evaluator.get(this), target);
            }

            Result result = null;
            Seq<CreeperNode> sortedChildren = Seq.with(children).removeAll(Objects::isNull).sort(node -> -evaluator.get(node));

            for (CreeperNode child : sortedChildren) {
                if (child.getCreeperByTeam(team) == 0f || child.dst2ToPoint(targetX, targetY) > range * range) {
                    continue;
                }

                float childBest = evaluator.get(child);
                if (childBest <= best) {
                    continue;
                }

                Result childResult = child.findBest(team, targetX, targetY, range, evaluator, best);

                if (childResult != null && childResult.best > best) {
                    best = childResult.best;
                    result = childResult;
                }
            }

            return result;
        }
        public static class Result {
            public float best;
            public CreeperTarget target;
            public Result(float best, CreeperTarget target) {
                this.best = best;
                this.target = target;
            }
        }
    }
}
