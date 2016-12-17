package ai;

import game.*;

import java.util.*;
import java.util.stream.Collectors;

import static util.Logger.out;


public class Context {
    public final GameMap gameMap;
    public final ArrayList<Location> friendlyBoundaries;
    public final int myID;
    public GameMap projectionMap;
    public DiffusionMap expansionMap;
    public DiffusionMap enemyFrontierMap;

    public Context(GameMap gameMap, ArrayList<Location> friendlyBoundaries, int myID, GameMap projectionMap, DiffusionMap expansionMap, DiffusionMap enemyFrontierMap) {
        this.gameMap = gameMap;
        this.friendlyBoundaries = friendlyBoundaries;
        this.myID = myID;
        this.projectionMap = projectionMap;
        this.expansionMap = expansionMap;
        this.enemyFrontierMap = enemyFrontierMap;
    }

    public Context(GameMap gameMap, ArrayList<Location> friendlyBoundaries, int myID, GameMap projectionMap, DiffusionMap expansionMap) {
        this(gameMap, friendlyBoundaries, myID, projectionMap, expansionMap, null);
    }

    public boolean isFriendly(Site site) {
        return site.owner == myID;
    }

    public Location findClosestBoundary(Location loc) {
        double minDistance = Double.MAX_VALUE;
        Location closestBoundary = null;
        double distance;
        for (Location boundaryLoc : friendlyBoundaries) {
            distance = gameMap.getDistance(loc, boundaryLoc);
            if (!loc.equals(boundaryLoc)) {
                if (distance < minDistance) {
                    minDistance = distance;
                    closestBoundary = boundaryLoc;
                }
            }
            if (minDistance < 2.0) break;
        }
        return closestBoundary;
    }

    public Location climbExpansionMap(Location friendlyLoc) {
        List<Location> targets = gameMap
                .getNeighbors(friendlyLoc)
                .stream()
                .filter(neighbor -> expansionMap.getValue(neighbor) > expansionMap.getValue(friendlyLoc))
                .sorted(Comparator.comparingDouble(expansionMap::getValue))
                .collect(Collectors.toList());
        if (targets.size() > 0) return targets.get(targets.size() - 1);
        else return friendlyLoc;
    }
}
