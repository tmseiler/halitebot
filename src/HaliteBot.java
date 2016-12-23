import ai.AStar;
import ai.BFSScorer;
import game.*;

import static util.Logger.out;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class HaliteBot {
    private static final int WAIT_FACTOR = 5;
    private int myID;
    private GameMap gameMap;
    private long turnStartTime;

    ArrayList<Location> friendlyLocations;
    ArrayList<Location> expansionTargets;
    ArrayList<Location> friendlyBoundaries;
    ArrayList<Location> friendlyFrontiers;
    PriorityQueue<Location> nonFrontiers;
    DiffusionMap expansionMap;
    ArrayList<Location> combatParticipants;

    void run() {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("Dahlia mk19");


        while (true) {
            String frameString = Networking.getString();
            turnStartTime = System.currentTimeMillis();
            out.printf("\n\nNew turn\n");
            ArrayList<Move> moves = new ArrayList<>();

            gameMap = Networking.deserializeGameMap(frameString);
            out.printf("[%s] created maps\n", getTimeRemaining());

            friendlyLocations = new ArrayList<>(0);
            friendlyBoundaries = new ArrayList<>(0);
            friendlyFrontiers = new ArrayList<>(0);
            combatParticipants = new ArrayList<>(0);
            nonFrontiers = new PriorityQueue<>((loc1, loc2) -> {
                Site s1 = gameMap.getSite(loc1);
                Site s2 = gameMap.getSite(loc2);
                if (s1.strength > s2.strength) return -1;
                else if (s1.strength == s2.strength) return 0;
                else return 1;
            });
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
                                if (gameMap.getSite(neighbor).owner == GameMap.NEUTRAL_OWNER)
                                    expansionTargets.add(neighbor);
                            }
                            friendlyBoundaries.add(location);
                        }
                    }
                }
            }

            out.printf("[%s] preprocessed map\n", getTimeRemaining());
            GameMap locMap = gameMap.copy();
            for (Location frontierLoc : friendlyFrontiers) {
                int bestScore = 0;
                locMap = gameMap.copy();
                locMap.simulateTurn(myID, moves);
                Site site = locMap.getSite(frontierLoc);
                if (site.strength < site.production * WAIT_FACTOR) {
                    nonFrontiers.add(frontierLoc);
                    continue;
                }
                Move bestMove = new Move(frontierLoc, Direction.STILL, myID);
                for (Direction d : Direction.DIRECTIONS) {
                    Site moveSite = locMap.getSite(frontierLoc, d);
                    GameMap simMap = locMap.copy();
                    if (moveSite.strength != 0 || moveSite.owner != GameMap.NEUTRAL_OWNER)
                        continue;
                    ArrayList<Move> frontierMoves = new ArrayList<>();
                    Move frontierMove = new Move(frontierLoc, d, myID);
                    frontierMoves.add(frontierMove);
                    int score = simMap.simulateTurn(myID, frontierMoves);
                    out.printf("[%s]\tSimulated turn %s -> %s: score %s\n", getTimeRemaining(), frontierLoc, frontierMove.dir, score);

                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = frontierMove;
                        out.printf("[%s]\tBest move for %s is now %s: score %s\n", getTimeRemaining(), frontierLoc, frontierMove.dir, score);
                    }
                }
                out.printf("[%s] Best move for %s is %s\n", getTimeRemaining(), frontierLoc, bestMove.dir);
                moves.add(bestMove);
                combatParticipants.add(frontierLoc);
            }

            expansionMap = new DiffusionMap(gameMap, gameMap -> {
                double[][] newMap = new double[gameMap.width][gameMap.height];
                for (int y = 0; y < gameMap.height; y++) {
                    for (int x = 0; x < gameMap.width; x++) {
                        Site site = gameMap.getSite(new Location(x, y));
                        if (site.owner == GameMap.NEUTRAL_OWNER && site.strength != 0) {
                            newMap[x][y] = site.individualAcquisitionScore();
                        }
                    }
                }
                return newMap;
            });

            out.printf("Time left after diffusion: %s\n", getTimeRemaining());

            while (getTimeRemaining() > 75 && !nonFrontiers.isEmpty()) {
                boolean makeGoodDecisions = getTimeRemaining() > 200;
                Location friendlyLoc = nonFrontiers.remove();
                if (!makeGoodDecisions && locMap.getSite(friendlyLoc).strength < locMap.getSite(friendlyLoc).production * WAIT_FACTOR) {
                    continue;
                }

                GameMap simMap;
                if (makeGoodDecisions)
                    simMap = gameMap.copy();
                else
                    simMap = locMap;

                simMap.simulateTurn(myID, moves);

                AStar astar = new AStar(simMap, myID);

                Site site = simMap.getSite(friendlyLoc);

                out.printf("\n%s (%s strength, %s prod)\n", friendlyLoc, site.strength, site.production);
                if (site.strength < site.production * WAIT_FACTOR) continue;

                Move move;

                // goal selection
                Location target = null;

                // move toward the nearest frontier if it's very close
                target = getNearestFrontier(friendlyLoc);
                out.printf("\t[%s] Nearest frontier is: %s\n", getTimeRemaining(), target);
                if (target != null) {
                    double distance;
                    if (makeGoodDecisions)
                        distance = astar.pathDistance(friendlyLoc, target);
                    else
                        distance = simMap.getDistance(friendlyLoc, target);

                    if (distance < Math.max(simMap.height, simMap.width) / 6.0)
                        out.printf("\t[%s] Nearest frontier is close enough to act: %s\n", getTimeRemaining(), target);
                    else
                        target = null;
                }

                // move high-strength units to nearby frontiers
                if (site.strength > 200) {
                    target = getNearestFrontier(friendlyLoc);
                    if (target != null) {
                        if(simMap.getDistance(friendlyLoc, target) < Math.max(gameMap.height, gameMap.width) / 3.0)
                        out.printf("\t[%s] There is a nearby frontier, and I am strong: %s\n", getTimeRemaining(), target);
                    } else {
                        // todo bfs enemy, A* to them, start digging

                        target = null;
                    }
                }

                // Expanding
                if (target == null) {
                    Location bestAdjacent = bfsBest(friendlyLoc, 1.0, loc -> {
                        Site s = simMap.getSite(loc);
                        if (s.owner == GameMap.NEUTRAL_OWNER)
                            return expansionMap.getValue(loc);
                        else
                            return 0.0;
                    });
                    out.printf("\t[%s] bestAdjacent is %s\n", getTimeRemaining(), bestAdjacent);

                    Location bestNearby = bfsBestFriendlyBoundary(friendlyLoc, 1);

                    out.printf("\t[%s] best nearby friendly is %s\n", getTimeRemaining(), bestNearby);
                    if (bestAdjacent != null && bestNearby != null) {
                        boolean veryWeak = simMap.getSite(bestAdjacent).strength < WAIT_FACTOR * simMap.getSite(bestAdjacent).production;
                        if (expansionMap.getValue(bestAdjacent) >= expansionMap.getValue(bestNearby)
                                || veryWeak
                                ) {
                            target = bestAdjacent;
                            out.printf("\t[%s] Best expansion target is adjacent: %s\n", getTimeRemaining(), target);
                        } else {
                            target = bestNearby;
                            out.printf("\t[%s] Best expansion target is nearby: %s\n", getTimeRemaining(), target);
                        }
                    } else if (bestAdjacent != null) {
                        target = bestAdjacent;
                        out.printf("\t[%s] Best expansion target is adjacent: %s\n", getTimeRemaining(), target);
                    } else if (bestNearby != null) {
                        target = bestNearby;
                        out.printf("\t[%s] Best expansion target is nearby: %s\n", getTimeRemaining(), target);
                    } else {
                        out.printf("\t[%s] No expansion targets nearby.\n", getTimeRemaining());
                    }
                }

                // no immediate expansion targets found, move toward something
                if (target == null) {
                    target = bfsBest(friendlyLoc, 1.0, loc -> expansionMap.getValue(loc));
                }

                // movement
                if (target != null) {
                    Location nextStep = astar.aStarFirstStep(friendlyLoc, target);
                    out.printf("\t[%s] A* says next step is %s\n", getTimeRemaining(), nextStep);
                    Direction direction = simMap.anyMoveToward(friendlyLoc, nextStep);

                    Site nextStepSite = simMap.getSite(friendlyLoc, direction);
                    if(combatParticipants.contains(nextStep)) continue;

                    if (isFriendly(nextStepSite)) {
                        int capWaste = (site.strength + nextStepSite.strength) - GameMap.MAX_STRENGTH;
                        out.printf("\t[%s] Cap waste is (%s + %s) - 255 = %s\n", getTimeRemaining(), site.strength, nextStepSite.strength, capWaste);

                        if (capWaste > 20) {
                            if (nonFrontiers.contains(nextStep)
                                    && nextStepSite.strength < site.strength
                                    && nextStepSite.strength < 200) {
                                out.printf("\t[%s] Swapping with %s\n", getTimeRemaining(), nextStep);
                                Move nextStepMove = new Move(nextStep, simMap.anyMoveToward(nextStep, friendlyLoc), myID);
                                nonFrontiers.remove(nextStep);
                                moves.add(nextStepMove);
                            } else {
                                direction = Direction.STILL;
                            }
                        } else {
                            int gatherStrength = site.strength;
                            if (nonFrontiers.contains(nextStep)
                                    && nextStepSite.strength < 200) {
                                out.printf("\t[%s] Telling %s to stay put\n", getTimeRemaining(), nextStep);
                                Move nextStepMove = new Move(nextStep, Direction.STILL, myID);
                                nonFrontiers.remove(nextStep);
                                moves.add(nextStepMove);
                                gatherStrength += nextStepSite.strength;
                            }
                            // gather
                            for (Location nextNeighbor : simMap.getNeighbors(nextStep)) {
                                if (nonFrontiers.contains(nextNeighbor) && !friendlyBoundaries.contains(nextNeighbor)) {
                                    Site nextNeighborSite = simMap.getSite(nextNeighbor);
                                    if (nextNeighborSite.strength > nextNeighborSite.production * WAIT_FACTOR) {
                                        if (nextNeighborSite.strength + gatherStrength < GameMap.MAX_STRENGTH) {
                                            Move gatherMove = new Move(nextNeighbor, simMap.anyMoveToward(nextNeighbor, nextStep), myID);
                                            moves.add(gatherMove);
                                            nonFrontiers.remove(nextNeighbor);
                                        }
                                    }
                                }
                            }

                        }
                    } else {
                        if (!shouldCapture(friendlyLoc, nextStep))
                            direction = Direction.STILL;
                    }

                    move = new Move(friendlyLoc, direction, myID);
                    moves.add(move);
                    out.printf("\t[%s] Added move %s\n", getTimeRemaining(), move);
                }
            }

            out.printf("Time left after assigning all moves: %s\n", getTimeRemaining());

            out.printf("Moves: %s\n", moves);
            Networking.sendFrame(moves);
        }

    }

    private Location bfsBestFriendlyBoundary(Location location, double maxDistance) {
//        out.printf("\t[%s] bfsBestFriendlyBoundary %s\n", getTimeRemaining(), location);
        Queue<Location> frontier = new LinkedBlockingQueue<>();
        HashMap<Site, Boolean> visited = new HashMap<>();
        frontier.add(location);
        Location bestLoc = null;
        double bestVal = 0.0;
        while (!frontier.isEmpty()) {
            Location currentLoc = frontier.remove();
            visited.put(gameMap.getSite(currentLoc), true);
            if (gameMap.getDistance(location, currentLoc) > maxDistance) break;

            for (Location nextLoc : gameMap.getNeighbors(currentLoc)) {
                // add to search
                Site nextSite = gameMap.getSite(nextLoc);
                if (isFriendly(nextSite)) {
                    if (!visited.containsKey(nextSite)) {
                        frontier.add(nextLoc);
                    }
                } else {
                    // evaluate score
                    double val = expansionMap.getValue(nextLoc);
                    if (val > bestVal) {
                        bestVal = val;
                        bestLoc = nextLoc;
                    }
                }
            }
        }
        return bestLoc;
    }

    private Location bfsBest(Location loc, double maxDistance, BFSScorer scorer) {
        Queue<Location> frontier = new LinkedBlockingQueue<>();
        HashMap<Site, Boolean> visited = new HashMap<>();
        frontier.add(loc);
        Location bestLoc = null;
        double bestVal = 0.0;

        while (!frontier.isEmpty()) {
            Location currentLoc = frontier.remove();
            visited.put(gameMap.getSite(currentLoc), true);
            if (gameMap.getDistance(loc, currentLoc) > maxDistance) break;

            // evaluate score
            double score = scorer.evaluatePosition(currentLoc);
            if (score > bestVal) {
                bestVal = score;
                bestLoc = currentLoc;
            }

            for (Location nextLoc : gameMap.getNeighbors(currentLoc)) {
                // add to search
                Site nextSite = gameMap.getSite(nextLoc);
                if (!visited.containsKey(nextSite)) {
                    frontier.add(nextLoc);
                }
            }
        }
//        out.printf("\t[%s] bfsBest bestLoc is %s\n", getTimeRemaining(), bestLoc);
        return bestLoc;
    }

    private Location bfsFirst(Location loc, double maxDistance, BFSScorer scorer) {
//        out.printf("\t[%s] bfsFirst %s\n", getTimeRemaining(), loc);
        Queue<Location> frontier = new LinkedBlockingQueue<>();
        HashMap<Site, Boolean> visited = new HashMap<>();
        frontier.add(loc);

        Location bestLoc = loc;
        while (!frontier.isEmpty()) {
            Location currentLoc = frontier.remove();
            visited.put(gameMap.getSite(currentLoc), true);
            if (gameMap.getDistance(loc, currentLoc) > maxDistance) break;

            // evaluate score
            if (scorer.evaluatePosition(currentLoc) > 0) {
                bestLoc = currentLoc;
                break;
            }

            for (Location nextLoc : gameMap.getNeighbors(currentLoc)) {
                // add to search
                Site nextSite = gameMap.getSite(nextLoc);
                if (!visited.containsKey(nextSite)) {
                    frontier.add(nextLoc);
                }
            }
        }
//        out.printf("\t[%s] bfsFirst bestLoc is %s\n", getTimeRemaining(), bestLoc);
        return bestLoc;
    }

    private boolean shouldCapture(Location myLocation, Location target) {
        Site mySite = gameMap.getSite(myLocation);
        Site targetSite = gameMap.getSite(target);
        return (targetSite.owner == GameMap.NEUTRAL_OWNER && targetSite.strength < mySite.strength);
    }

    private boolean isFrontier(Location location) {
        for (Location neighbor : gameMap.getNeighbors(location)) {
            Site s = gameMap.getSite(neighbor);
            if (s.strength == 0 && s.owner == GameMap.NEUTRAL_OWNER)
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
        return 1000 - (System.currentTimeMillis() - turnStartTime);
    }

    private Location getNearestFrontier(Location loc) {
        Location nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Location frontier : friendlyFrontiers) {
            double distance = gameMap.getDistance(loc, frontier);
            if (distance < minDist) {
                minDist = distance;
                nearest = frontier;
            }
        }
        return nearest;
    }

    private Mission determineMission(GameMap gameMap, Location location) {

        return Mission.GESTATE;
    }
}
