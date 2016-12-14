import ai.Context;
import game.*;

import static util.Logger.out;

import java.util.ArrayList;

class HaliteBot {
    private int myID;
    private GameMap gameMap;
    private long turnStartTime;

    ArrayList<Location> expansionTargets;
    DiffusionMap expansionMap;

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
            out.printf("Time left after creating maps %s\n", getTimeRemaining());

            ArrayList<Location> friendlyLocations = new ArrayList<>(0);
            ArrayList<Location> friendlyBoundaries = new ArrayList<>(0);
            ArrayList<Location> friendlyFrontiers = new ArrayList<>(0);
            ArrayList<Location> nonFrontiers = new ArrayList<>(0);
            expansionTargets = new ArrayList<>(0);

            // pre-process locations
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Location location = new Location(x, y);
                    Site site = gameMap.getSite(location);
                    site.isFriendly = isFriendly(site);
                    if (site.isFriendly) {
                        friendlyLocations.add(location);
                        if (isFrontier(location)) {
                            friendlyFrontiers.add(location);
                        } else {
                            nonFrontiers.add(location);
                        }
                        if (isBoundary(location)) {
                            for (Location neighbor : gameMap.getNeighbors(location)) {
                                if(gameMap.getSite(neighbor).owner == GameMap.NEUTRAL_OWNER)
                                    expansionTargets.add(neighbor);
                            }
                            friendlyBoundaries.add(location);
                        }
                    }
                }
            }

            out.printf("Time left after preprocessing: %s\n", getTimeRemaining());
            GameMap bestMap = Networking.deserializeGameMap(frameString);
            for (Location frontierLoc : friendlyFrontiers) {
                int bestScore = 0;

                for (Direction d : Direction.CARDINALS) {
                    Site site = bestMap.getSite(frontierLoc, d);
                    if(site.strength != 0 && site.owner != GameMap.NEUTRAL_OWNER)
                        continue;
                    ArrayList<Move> frontierMoves = new ArrayList<>();
                    Move frontierMove = new Move(frontierLoc, d, myID);
                    frontierMoves.add(frontierMove);
                    int score = bestMap.simulateTurn(myID, frontierMoves);
                    out.printf("Time left after simulating turn (%s, %s, score %s): %s\n", frontierLoc, frontierMove, score, getTimeRemaining());

                    if(score > bestScore) {
                        bestMap = bestMap.copy();
                        moves.add(frontierMove);
                        out.printf("Best move for %s is now %s (score %s)\n", frontierLoc, frontierMove, score);
                    }
                }
            }


            expansionMap = new DiffusionMap(gameMap, gameMap -> {
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
                    myID, bestMap, expansionMap);

            for (Location friendlyLoc : nonFrontiers) {
                Site site = gameMap.getSite(friendlyLoc);
                if(site.strength < site.production * 5) continue;

                out.printf("\n%s (%s strength)\n", friendlyLoc, gameMap.getSite(friendlyLoc).strength);

                Move move;
                Location expTarget = getBestExpansionTarget();
                out.printf("\t[%s] Best exp target: %s\n", getTimeRemaining(), expTarget);
                if (expTarget != null) {
                    Location nextStep = context.aStar(friendlyLoc, expTarget);
                    out.printf("\t[%s] A* says next step is %s\n", getTimeRemaining(), nextStep);
                    Direction direction = context.anyMoveToward(friendlyLoc, nextStep);
                    if(!shouldMove(friendlyLoc, nextStep))
                        direction = Direction.STILL;

                    move = new Move(friendlyLoc, direction, myID);
                    moves.add(move);
                    bestMap.simulateTurn(myID, move);
                    bestMap = bestMap.copy();
                }
            }

            out.printf("Time left after assigning all moves: %s\n", getTimeRemaining());

            out.printf("Moves: %s\n", moves);
            Networking.sendFrame(moves);
        }

    }

    private Location getBestExpansionTarget() {
        Location bestLoc = null;
        double bestVal = 0.0;
        for (Location expansionTarget : expansionTargets) {
            double val = expansionMap.getValue(expansionTarget.x, expansionTarget.y);
            out.printf("\tExpansion target %s is %s\n", expansionTarget, val);
            if(val > bestVal) {
                bestVal = val;
                bestLoc = expansionTarget;
            }
        }
        return bestLoc;
    }

    private boolean shouldMove(Location myLocation, Location target) {
        Site mySite = gameMap.getSite(myLocation);
        Site targetSite = gameMap.getSite(target);
        if(isFriendly(targetSite)) {
            return (targetSite.strength + mySite.strength < GameMap.MAX_STRENGTH);
        } else {
            return (targetSite.owner == GameMap.NEUTRAL_OWNER && targetSite.strength < mySite.strength);
        }
    }

    private boolean isFrontier(Location location) {
        for (Location neighbor : gameMap.getNeighbors(location)) {
            Site s = gameMap.getSite(neighbor);
            if(s.strength == 0 && s.owner == GameMap.NEUTRAL_OWNER)
                return true;
        }
        return false;
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
