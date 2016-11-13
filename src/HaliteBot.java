import java.io.*;
import java.util.ArrayList;

class HaliteBot {
    private int myID;
    private GameMap gameMap;
    //    private ArrayList<Location> friendlyLocations;
    private ArrayList<Location> friendlyBoundaries;
    private ArrayList<Location> friendlyInteriors;
//    private ArrayList<Location> enemyNeighbors;

    PrintWriter out;

    void run() {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        int seekDistance = Math.max(gameMap.height, gameMap.width) / 4;

        try {
            out = new PrintWriter(new FileWriter("debugv4.log"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Networking.sendInit("Dahlia mk4");


        while (true) {
            ArrayList<Move> moves = new ArrayList<>();

            gameMap = Networking.getFrame();

//        friendlyLocations = new ArrayList<>(0);
            friendlyBoundaries = new ArrayList<>(0);
            friendlyInteriors = new ArrayList<>(0);
//        enemyNeighbors = new ArrayList<>(0);


            // pre-process locations
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Location location = new Location(x, y);
                    Site site = gameMap.getSite(location);
                    if (isFriendly(site)) {
//                        friendlyLocations.add(location);
                        if (isBoundary(location))
                            friendlyBoundaries.add(location);
                        else
                            friendlyInteriors.add(location);

                        for (Direction d : Direction.CARDINALS) {
                            Site potentialEnemy = gameMap.getSite(location, d);
                            if (!isFriendly(potentialEnemy)) {
                                potentialEnemy.individualAcquisitionScore();
//                                enemyNeighbors.add(gameMap.getLocation(location, d));
                            }
                        }
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

            for (Location friendlyBoundary : friendlyBoundaries) {
                Site site = gameMap.getSite(friendlyBoundary);
                for (Direction d : Direction.CARDINALS) {
                    Site neighborSite = gameMap.getSite(friendlyBoundary, d);
                    if (!isFriendly(neighborSite)) {
                        site.bolsterScore += neighborSite.strengthProductionIndex;
//                        out.printf("bolsterScore for %s is %s\n", friendlyBoundary, site.bolsterScore);
                    }
                }
            }

//            out.printf("%s\n", friendlyBoundaries);
//            out.printf("%s\n", friendlyInteriors);

            for (Location location : friendlyBoundaries) {
                Location nearbyTarget = findBestValueNearby(location, 3);
                double targetDistance = gameMap.getDistance(location, nearbyTarget);

                Move move;
                Direction dir = getAttackDirection(location);
                Site site = gameMap.getSite(location);
                if (site.strength > gameMap.getSite(location, dir).strength) {
                    move = new Move(location, dir);
                } else {
                    out.printf("%s sees %s nearby. (Value %s, Distance %s)\n", location, nearbyTarget,
                            gameMap.getSite(nearbyTarget).clusterAcquisitionScore, targetDistance);

                    Location friendlyBoundary = findClosestBoundary(location);

                    if (friendlyBoundary != null) {
                        Site boundarySite = gameMap.getSite(friendlyBoundary);
                        if (site.strength > site.production * 3
                                && boundarySite.strength + site.strength < 255
                                && site.strength < boundarySite.strength
                                ) {
                            move = moveToward(location, nearbyTarget, true);
                        } else {
                            move = new Move(location, Direction.STILL);
                        }
                    } else {
                        move = new Move(location, Direction.STILL);
                    }
                }
                moves.add(move);
            }

            for (Location loc : friendlyInteriors) {
                Site site = gameMap.getSite(loc);
                Move move;
                if (site.strength > site.production * 3) {
                    move = moveToward(loc, findClosestBoundary(loc), true);
                } else {
                    move = new Move(loc, Direction.STILL);
                }
                moves.add(move);
            }
            Networking.sendFrame(moves);
        }
    }

    private Direction getAttackDirection(Location loc) {
        // todo maybe nuke this
        double bestScore = -1;
        Site site = gameMap.getSite(loc);
        Direction bestDirection = null;
        for (Direction d : Direction.CARDINALS) {
            Site checkSite = gameMap.getSite(loc, d);
            if (!isFriendly(checkSite) && checkSite.strengthProductionIndex > bestScore) {
                bestScore = checkSite.strengthProductionIndex;
                bestDirection = d;
            }
        }
        return bestDirection;
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
