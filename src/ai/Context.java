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

    public Location getBestBoundary(Location loc) {
        List<Location> locations = friendlyBoundaries.stream()
                .sorted(Comparator.comparingDouble(o -> expansionMap.getValue(o) / gameMap.getDistance(loc, o)))
                .collect(Collectors.toList());
        return locations.get(locations.size() - 1);
    }

    public Location getBestExpansionNeighbor(Location loc) {
        List<Location> targets = gameMap
                .getNeighbors(loc)
                .stream()
                .filter(neighbor -> !isFriendly(gameMap.getSite(neighbor)))
                .filter(neighbor -> gameMap.getSite(neighbor).strength < gameMap.getSite(loc).strength)
                .sorted(Comparator.comparingDouble(expansionMap::getValue))
                .collect(Collectors.toList());
        if (targets.size() > 0) return targets.get(targets.size() - 1);
        else return null;
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

    public ArrayList<Direction> moveToward(Location origin, Location destination) {
        ArrayList<Direction> possibleDirections = new ArrayList<>(0);

        int dx = origin.x - destination.x;
        int dy = origin.y - destination.y;

        double halfWidth = gameMap.width / 2.0;
        if(dx != 0) {
            if ((Math.abs(dx) < halfWidth && dx < 0) || dx > halfWidth)
                possibleDirections.add(Direction.EAST);
            if (dx < halfWidth * -1.0 || Math.abs(dx) < halfWidth)
                possibleDirections.add(Direction.WEST);
        }


        double halfHeight = gameMap.height / 2.0;
        if(dy != 0) {
            if ((Math.abs(dy) < halfHeight && dy < 0)|| dy > halfHeight)
                possibleDirections.add(Direction.SOUTH);
            if (dy < halfHeight * -1.0 || Math.abs(dy) < halfHeight)
                possibleDirections.add(Direction.NORTH);
        }

        out.printf("moveToward %s to %s (%s)\n", origin, destination, possibleDirections);
        return possibleDirections;
    }

    public Direction anyMoveToward(Location origin, Location destination) {
        List<Direction> directions = moveToward(origin, destination).stream()
                .collect(Collectors.toList());
        if (directions.size() > 0) return directions.get(0);
        else return Direction.STILL;
    }

    public Direction friendlyMoveToward(Location origin, Location destination) {
        List<Direction> directions = moveToward(origin, destination).stream()
                .filter(dir -> gameMap.getSite(gameMap.getLocation(origin, dir)).isFriendly)
                .collect(Collectors.toList());
        if (directions.size() > 0) return directions.get(0);
        else return Direction.STILL;
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

    public int getCost(Site site) {
        if(isFriendly(site)) {
            return 1 + site.production;
        } else {
            return 1 + site.strength;
        }
    }

    public Location aStar(Location start, Location goal) {
        Site startSite = gameMap.getSite(start);
        if(start.equals(goal)) return start;

        PriorityQueue<Site> frontier = new PriorityQueue<>(new AStarPrioritizer(this, goal));
        frontier.add(startSite);

        HashMap<Site, Integer> costSoFar = new HashMap<>();
        HashMap<Site, Site> cameFrom = new HashMap<>();

        costSoFar.put(startSite, 0);
        cameFrom.put(startSite, null);

        Site currentSite = startSite;
        while(!frontier.isEmpty()) {
            currentSite = frontier.remove();
            if(currentSite.location.equals(goal)) break;
//            if(gameMap.getDistance(start, currentSite.location) > 20.0) break;

            for (Location nextLoc : projectionMap.getNeighbors(currentSite.location)) {
                Site nextSite = gameMap.getSite(nextLoc);
                int newCost = costSoFar.get(currentSite) + getCost(nextSite);
                if((!costSoFar.containsKey(nextSite)) || newCost < costSoFar.get(nextSite) ) {
                    costSoFar.put(nextSite, newCost);
                    frontier.add(nextSite);
                    cameFrom.put(nextSite, currentSite);
                }
            }
        }

        final ArrayList<Location> pathList = new ArrayList<>();
        Site destination = currentSite;
        pathList.add(destination.location);

        while (cameFrom.containsKey(destination)) {
            destination = cameFrom.get(destination);
            if(destination != null && !destination.location.equals(start))
                pathList.add(destination.location);
        }
        Collections.reverse(pathList);
        out.printf("\tPath for %s -> %s: %s\n", start, goal, pathList);
        return pathList.get(0);
    }
}
