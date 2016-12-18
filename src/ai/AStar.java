package ai;

import game.GameMap;
import game.Location;
import game.Site;

import java.util.*;

import static util.Logger.out;

public class AStar {
    private final GameMap gameMap;
    private final int myID;
    private HashMap<Location, HashMap<Location, List<Location>>> pathCache;

    public AStar(GameMap gameMap, int myID) {
        this.gameMap = gameMap;
        this.myID = myID;
        pathCache = new HashMap<>();
    }

    public List<Location> aStar(Location start, Location goal) {
        HashMap<Location, List<Location>> startHash = pathCache.get(start);
        if(startHash != null) {
            if(startHash.get(goal) != null)
                return startHash.get(goal);
        } else {
            pathCache.put(start, new HashMap<>());
        }

        Site startSite = gameMap.getSite(start);
        final ArrayList<Location> pathList = new ArrayList<>();

        if(start.equals(goal)) {
            pathList.add(start);
            return pathList;
        }

        PriorityQueue<Site> frontier = new PriorityQueue<>(new AStarPrioritizer(gameMap, goal, myID));
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

            for (Location nextLoc : gameMap.getNeighbors(currentSite.location)) {
                Site nextSite = gameMap.getSite(nextLoc);
                int newCost = costSoFar.get(currentSite) + gameMap.getCost(nextSite, myID);
                if((!costSoFar.containsKey(nextSite)) || newCost < costSoFar.get(nextSite) ) {
                    costSoFar.put(nextSite, newCost);
                    frontier.add(nextSite);
                    cameFrom.put(nextSite, currentSite);
                }
            }
        }

        Site destination = currentSite;
        pathList.add(destination.location);

        while (cameFrom.containsKey(destination)) {
            destination = cameFrom.get(destination);
            if(destination != null && !destination.location.equals(start))
                pathList.add(destination.location);
        }
        Collections.reverse(pathList);
        out.printf("\tPath for %s -> %s: %s\n", start, goal, pathList);

        pathCache.get(start).put(goal, pathList);

        return pathList;
    }

    public Location aStarFirstStep(Location start, Location goal) {
        return aStar(start, goal).get(0);
    }

    public double pathDistance(Location start, Location goal) {
        return (double) aStar(start, goal).size();
    }
}
