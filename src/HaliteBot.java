import ai.*;
import game.*;

import static util.Logger.out;

import java.util.ArrayList;

class HaliteBot {
    private int myID;
    private GameMap gameMap;
    private ArrayList<Location> friendlyLocations;
    private ArrayList<Location> friendlyBoundaries;


    void run() {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("Dahlia mk5");

        while (true) {
            ArrayList<Move> moves = new ArrayList<>();

            gameMap = Networking.getFrame();
            out.printf("New turn\n\n");
            friendlyLocations = new ArrayList<>(0);
            friendlyBoundaries = new ArrayList<>(0);

            // pre-process locations
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Location location = new Location(x, y);
                    Site site = gameMap.getSite(location);
                    if (isFriendly(site)) {
                        friendlyLocations.add(location);
                        if (isBoundary(location))
                            friendlyBoundaries.add(location);
                    } else {
                        // calculate desirability based on neighbors (poor man's clustering)
                        site.clusterAcquisitionScore = 0;
                        for (Direction d : Direction.CARDINALS) {
                            Site neighborSite = gameMap.getSite(location, d);
                            if (!isFriendly(neighborSite)) {
                                site.clusterAcquisitionScore += neighborSite.individualAcquisitionScore();
                            }
                        }
                    }
                }
            }

            for (Location friendlyLoc : friendlyLocations) {
                ActionSet actions = new ActionSet();
//                actions.add(new WaitAction(gameMap, friendlyLoc));

                for (Direction direction : Direction.CARDINALS) {
                    Site neighbor = gameMap.getSite(friendlyLoc, direction);
                    if (isFriendly(neighbor)) {
                        actions.add(new BolsterAction(gameMap, friendlyLoc, direction));
                    } else {
                        actions.add(new AttackAction(gameMap, friendlyLoc, direction));
                    }
                }
                Move evaluatedMove = actions.evaluate();
                if(evaluatedMove == null) {
                    out.printf("Null move for %s\n", friendlyLoc);
                    evaluatedMove = new Move(friendlyLoc, Direction.STILL);
                }
                moves.add(evaluatedMove);
            }
            out.printf("Moves: %s\n", moves);
            Networking.sendFrame(moves);
        }
    }

    private Location findClosestBoundary(Location loc) {
        double minDistance = 10000000.00;
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

    private Location findBestBoundary(Location loc) {
        double bestScore = -1.0;
        Location bestBoundary = null;
        double score;
//        out.printf("findBestBoundary for %s\n", loc);
        for (Location boundaryLoc : friendlyBoundaries) {
            if (!loc.equals(boundaryLoc)) {
                Site site = gameMap.getSite(boundaryLoc);
                double distance = gameMap.getDistance(loc, boundaryLoc);
                score = (site.bolsterScore - site.strength) / (5 * distance * distance);
//                out.printf("\tScore for %s is %s (distance %s, bolsterscore %s)\n", boundaryLoc, score, distance, site.bolsterScore);
                if (score > bestScore) {
                    bestBoundary = boundaryLoc;
                    bestScore = score;
                }
            }
        }
//        out.printf("Best Boundary for %s is %s\n", loc, bestBoundary);
        return bestBoundary;
    }

    private Location findBestValueNearby(Location loc, int depth) {
        if (depth <= 1) return loc;
        Site site = gameMap.getSite(loc);
        Location bestLoc = loc;
        for (Direction d : Direction.CARDINALS) {
            Location checkLoc = findBestValueNearby(gameMap.getLocation(loc, d), depth - 1);
            Site checkSite = gameMap.getSite(checkLoc);
            if (checkSite.clusterAcquisitionScore > gameMap.getSite(bestLoc).clusterAcquisitionScore) {
                bestLoc = checkLoc;
            }
        }
        return bestLoc;
    }

    private Move moveToward(Location origin, Location destination, boolean friendlyOnly) {
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

    private boolean isFriendly(Site site) {
        return site.owner == myID;
    }

    private boolean hasFriendlyNeighbor(Location location) {
        for (Direction d : Direction.CARDINALS) {
            if (isFriendly(gameMap.getSite(location, d)))
                return true;
        }
        return false;
    }

    private boolean isBoundary(Location location) {
        Site friendlySite = gameMap.getSite(location);
        for (Direction d : Direction.CARDINALS) {
            if (!isFriendly(gameMap.getSite(location, d)))
                return true;
        }
        return false;
    }
}
