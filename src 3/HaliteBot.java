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

        try {
            out = new PrintWriter(new FileWriter("debug.log"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Networking.sendInit("MyJavaBot");


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
                                potentialEnemy.setStrengthProductionIndex();
//                                enemyNeighbors.add(gameMap.getLocation(location, d));
                            }
                        }
                    }
                }
            }

            out.printf("%s\n", friendlyBoundaries);
            out.printf("%s\n", friendlyInteriors);

            for (Location location : friendlyBoundaries) {
                Move move;
                Direction dir = getAttackDirection(location);
                Site site = gameMap.getSite(location);
                if (site.strength > gameMap.getSite(location, dir).strength) {
                    move = new Move(location, dir);
                } else {
                    Location friendlyBoundary = findClosestBoundary(location);

                    if (friendlyBoundary != null) {
                        Site boundarySite = gameMap.getSite(friendlyBoundary);
                        if (site.strength > site.production * 5 &&
                                boundarySite.strength + site.strength < 255 &&
                                site.strength < boundarySite.strength) {
                            move = moveToward(location, friendlyBoundary);
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
                if (site.strength > site.production * 8) {
                    move = moveToward(loc, findClosestBoundary(loc));
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

    private Move moveToward(Location origin, Location destination) {
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

        for (Direction d : possibleDirections) {
            if (isFriendly(gameMap.getSite(origin, d))) {
                direction = d;
                break;
            }
        }
        out.printf("moveToward %s to %s (%s)\n", origin, destination, direction);
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
        for (Direction d : Direction.CARDINALS) {
            if (!isFriendly(gameMap.getSite(location, d)))
                return true;
        }
        return false;
    }
}
