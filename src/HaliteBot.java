import ai.Context;
import game.*;

import static util.Logger.out;

import java.util.ArrayList;

class HaliteBot {
    private int myID;
    private GameMap gameMap;
    private long turnStartTime;


    void run() {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("Dahlia mk13");


        while (true) {
            turnStartTime = System.currentTimeMillis();
            out.printf("\n\nNew turn\n");
            ArrayList<Move> moves = new ArrayList<>();

            String frameString = Networking.getString();
            gameMap = Networking.deserializeGameMap(frameString);
            GameMap projectionMap = Networking.deserializeGameMap(frameString);
            out.printf("Time left after creating maps %s\n", getTimeRemaining());

            ArrayList<Location> friendlyLocations = new ArrayList<>(0);
            ArrayList<Location> friendlyBoundaries = new ArrayList<>(0);
            ArrayList<Location> friendlyFrontiers = new ArrayList<>(0);

            // pre-process locations
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Location location = new Location(x, y);
                    Site site = gameMap.getSite(location);
                    site.isFriendly = isFriendly(site);
                    if (site.isFriendly) {
                        friendlyLocations.add(location);
                        if (isBoundary(location)) {
                            friendlyBoundaries.add(location);
                            if(isFrontier(location)) {
                                friendlyFrontiers.add(location);
                            }
                        }
                    }
                }
            }

            out.printf("Time left after preprocessing: %s\n", getTimeRemaining());

            for (Direction d : Direction.CARDINALS) {
                ArrayList<Move> frontierMoves = new ArrayList<>();
                for (Location frontierLoc : friendlyFrontiers) {
                    frontierMoves.add(new Move(frontierLoc, d, myID));
                }
                projectionMap.performMoves(frontierMoves);
                out.printf("Time left after simulating one turn (%s): %s\n", d, getTimeRemaining());
            }

            DiffusionMap expansionMap = new DiffusionMap(gameMap, gameMap -> {
                double[][] newMap = new double[gameMap.width][gameMap.height];
                for (int y = 0; y < gameMap.height; y++) {
                    for (int x = 0; x < gameMap.width; x++) {
                        Site site = gameMap.getSite(new Location(x, y));
                        if (site.owner == GameMap.NEUTRAL_OWNER) {
                            newMap[x][y] = site.individualAcquisitionScore();
                        }
                    }
                }
                return newMap;
            });

            out.printf("Time left after diffusion: %s\n", getTimeRemaining());

            Context context = new Context(gameMap, friendlyBoundaries,
                    myID, projectionMap, expansionMap);

            friendlyLocations.stream()
                    .filter(location -> {
                        Site site = gameMap.getSite(location);
                        return site.strength > site.production * 5;
                    })
                    .forEach(friendlyLoc -> {
                        out.printf("\n%s (%s strength)\n", friendlyLoc, gameMap.getSite(friendlyLoc).strength);

                        Move move;
                        Location expTarget = context.getBestExpansionNeighbor(friendlyLoc);
                        if (expTarget != null) {
                            move = new Move(friendlyLoc, context.unfriendlyMoveToward(friendlyLoc, expTarget), myID);
                            moves.add(move);
                        } else {
                            out.printf("No expansion target found\n");
                            move = new Move(friendlyLoc, context.friendlyMoveToward(friendlyLoc, context.findClosestBoundary(friendlyLoc)), myID);
                            moves.add(move);
                        }
                    });

            out.printf("Time left after assigning all moves: %s\n", getTimeRemaining());

            out.printf("Moves: %s\n", moves);
            Networking.sendFrame(moves);
        }
    }

    private boolean isFrontier(Location location) {
        return gameMap.getNeighbors(location)
                .stream()
                .anyMatch(neighbor -> gameMap.getSite(neighbor).strength == 0 && gameMap.getSite(neighbor).owner == GameMap.NEUTRAL_OWNER);
    }

    private boolean isFriendly(Site site) {
        return site.owner == myID;
    }

    private boolean isBoundary(Location location) {
        for (Direction d : Direction.CARDINALS) {
            if (!isFriendly(gameMap.getSite(location, d)))
                return true;
        }
        return false;
    }

    private long getTimeRemaining() {
        return turnStartTime + 1000 - System.currentTimeMillis();
    }
}
