package ai;

import game.*;

import java.util.ArrayList;


public class Context {
    public final Location agentLocation;
    public final GameMap gameMap;
    public final ArrayList<Location> friendlyBoundaries;
    public final int myID;
    public GameMap projectionMap;

    public Context(Location agentLocation, GameMap gameMap, ArrayList<Location> friendlyBoundaries, int myID, GameMap projectionMap) {
        this.agentLocation = agentLocation;
        this.gameMap = gameMap;
        this.friendlyBoundaries = friendlyBoundaries;
        this.myID = myID;
        this.projectionMap = projectionMap;
    }

    public ArrayList<Location> getAgentNeighbors() {
        return gameMap.getNeighbors(agentLocation);
    }

    public ArrayList<Location> getAgentEnemyNeighbors() {
        ArrayList<Location> neighbors = new ArrayList<>(0);
        for (Location loc : getAgentNeighbors()) {
            if(!isFriendly(gameMap.getSite(loc))) neighbors.add(loc);
        }
        return neighbors;
    }

    public boolean isFriendly(Site site) {
        return site.owner == myID;
    }

    public Location findClosestBoundary() {
        double minDistance = 10000000.00;
        Location closestBoundary = null;
        double distance;
        for (Location boundaryLoc : friendlyBoundaries) {
            distance = gameMap.getDistance(agentLocation, boundaryLoc);
            if (!agentLocation.equals(boundaryLoc)) {
                if (distance < minDistance) {
                    minDistance = distance;
                    closestBoundary = boundaryLoc;
                }
            }
            if (minDistance < 2.0) break;
        }
        return closestBoundary;
    }

    public Move moveToward(Location origin, Location destination, boolean friendlyOnly) {
        Move move;
        Direction direction = Direction.STILL;

        ArrayList<Direction> possibleDirections = new ArrayList<>(0);
        if (destination == null)
            return new Move(origin, Direction.STILL);
        if (destination.x > origin.x)
            possibleDirections.add(Direction.EAST);
        if (destination.y > origin.y)
            possibleDirections.add(Direction.SOUTH);
        if (destination.x < origin.x)
            possibleDirections.add(Direction.WEST);
        if (destination.y < origin.y)
            possibleDirections.add(Direction.NORTH);

        for (Direction possibleDirection : possibleDirections) {
            if (friendlyOnly && isFriendly(gameMap.getSite(origin, possibleDirection))) {
                direction = possibleDirection;
                break;
            }
        }
//        out.printf("moveToward %s to %s (%s)\n", origin, destination, direction);
        return new Move(origin, direction);
    }
}
