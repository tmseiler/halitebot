import ai.AStar;
import ai.BFSScorer;
import ai.Mission;
import game.*;

import static util.Logger.out;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class HaliteBot {
    private static int WAIT_FACTOR = 5;
    private int myID;
    private GameMap gameMap;
    private long turnStartTime;

    ArrayList<Location> friendlyLocations;
    ArrayList<Location> expansionTargets;
    ArrayList<Location> friendlyBoundaries;
    ArrayList<Location> friendlyFrontiers;
    ArrayList<Location> enemyFrontiers;
    PriorityQueue<Location> nonFrontiers;
    DiffusionMap expansionMap;
    ArrayList<Location> combatParticipants;
    boolean contactMade = false;

    void run() {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("Dahlia mk28");


        while (true) {
            String frameString = Networking.getString();
            turnStartTime = System.currentTimeMillis();
            out.printf("\n\nNew turn\n");
            ArrayList<Move> myMoves = new ArrayList<>();

            gameMap = Networking.deserializeGameMap(frameString);
            out.printf("[%s] created maps\n", getTimeRemaining());

            friendlyLocations = new ArrayList<>(0);
            friendlyBoundaries = new ArrayList<>(0);
            friendlyFrontiers = new ArrayList<>(0);
            enemyFrontiers = new ArrayList<>(0);
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
                    } else {
                        if (isFrontier(location) && gameMap.getSite(location).owner != GameMap.NEUTRAL_OWNER)
                            enemyFrontiers.add(location);
                    }
                }
            }

            if (friendlyFrontiers.size() > 0) contactMade = true;
            WAIT_FACTOR = contactMade ? 7 : 5;

            out.printf("[%s] preprocessed map\n", getTimeRemaining());

            ArrayList<Move> enemyMoves = new ArrayList<>(0);
            for (Location enemyFrontier : enemyFrontiers) {
                Site frontierSite = gameMap.getSite(enemyFrontier);
                int bestCount = 0;
                Direction bestDirection = Direction.STILL;

                for (Direction d : Direction.CARDINALS) {
                    int count = 0;
                    Location targetLoc = gameMap.getLocation(enemyFrontier, d);

                    if (!isFrontier(targetLoc)) continue;
                    count++;

                    for (Location targetNeighbor : gameMap.getNeighbors(targetLoc)) {
                        Site neighborSite = gameMap.getSite(targetNeighbor);
                        if (neighborSite.owner != frontierSite.owner && neighborSite.owner != GameMap.NEUTRAL_OWNER)
                            count++;
                    }

                    if (count > bestCount) {
                        bestCount = count;
                        bestDirection = d;
                    }
                }
                enemyMoves.add(new Move(enemyFrontier, bestDirection, frontierSite.owner));
            }
            out.printf("[%s] Assumed enemy moves: %s\n", getTimeRemaining(), enemyMoves);

            GameMap locMap = gameMap.copy();
            for (Location frontierLoc : friendlyFrontiers) {
                int bestScore = 0;
                ArrayList<Move> allMoves = new ArrayList<>();
                allMoves.addAll(enemyMoves);
                allMoves.addAll(myMoves);
                locMap = gameMap.copy();

                locMap.simulateTurn(myID, allMoves);
                Site site = gameMap.getSite(frontierLoc);
                if (site.strength < site.production * WAIT_FACTOR) {
                    nonFrontiers.add(frontierLoc);
                    continue;
                }

                Move bestMove = new Move(frontierLoc, Direction.STILL, myID);
                for (Direction d : Direction.DIRECTIONS) {
                    Location moveLoc = locMap.getLocation(frontierLoc, d);
                    Site moveSite = locMap.getSite(moveLoc);

                    GameMap simMap = locMap.copy();
                    if (moveSite.strength != 0 || moveSite.owner != GameMap.NEUTRAL_OWNER)
                        continue;
                    ArrayList<Move> frontierMoves = new ArrayList<>();
                    Move frontierMove = new Move(frontierLoc, d, myID);
                    frontierMoves.add(frontierMove);
                    int score = simMap.simulateTurn(myID, frontierMoves);
                    out.printf("[%s]\tSimulated turn %s -> %s: score %s\n", getTimeRemaining(), frontierLoc, frontierMove.dir, score);

//                    for (Location moveNeighbor : gameMap.getNeighbors(moveLoc)) {
//                        if(site.strength < site.production * WAIT_FACTOR && isEnemy(gameMap.getSite(moveNeighbor))) {
//                            score *= 0;
//                        }
//                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = frontierMove;
                        out.printf("[%s]\tBest move for %s is now %s: score %s\n", getTimeRemaining(), frontierLoc, frontierMove.dir, score);
                    }
                }

//                if(site.strength < site.production * WAIT_FACTOR && bestMove.dir == Direction.STILL) {
//                    nonFrontiers.add(frontierLoc);
//                    continue;
//                }

                out.printf("[%s] Best move for %s (%s strength, %s prod) is %s\n", getTimeRemaining(), frontierLoc, site.strength, site.production, bestMove.dir);
                myMoves.add(bestMove);
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

            while (getTimeRemaining() > 50 && !nonFrontiers.isEmpty()) {
                boolean makeGoodDecisions = getTimeRemaining() > 100;
                Location friendlyLoc = nonFrontiers.remove();
                if (!makeGoodDecisions && locMap.getSite(friendlyLoc).strength < locMap.getSite(friendlyLoc).production * 5) {
                    continue;
                }

                GameMap simMap;
                if (makeGoodDecisions)
                    simMap = gameMap.copy();
                else
                    simMap = locMap;

                ArrayList<Move> allMoves = new ArrayList<>();
                allMoves.addAll(enemyMoves);
                allMoves.addAll(myMoves);

                simMap.simulateTurn(myID, allMoves);

                AStar astar = new AStar(simMap, myID);

                Site site = simMap.getSite(friendlyLoc);
                Site currentSite = gameMap.getSite(friendlyLoc);
                out.printf("\n%s (%s strength, %s prod)\n", friendlyLoc, currentSite.strength, currentSite.production);
                if (site.strength < site.production * WAIT_FACTOR) {
                    out.printf("\tShould not move -- skipping\n");
                    continue;
                }

                Mission mission = null;
                Move move;

                // goal selection
                Location target = null;

                // move toward the nearest frontier if it's very close
                Location nearestFrontier = getNearestFrontier(friendlyLoc);
                out.printf("\t[%s] Nearest frontier is: %s\n", getTimeRemaining(), target);
                if (nearestFrontier != null) {
                    target = nearestFrontier;
                    double distance;
                    if (makeGoodDecisions)
                        distance = astar.pathDistance(friendlyLoc, target);
                    else
                        distance = simMap.getDistance(friendlyLoc, target);

//                    if (distance < Math.max(simMap.height, simMap.width) / 4.0)
                    if (distance < 7.0)
                        out.printf("\t[%s] Nearest frontier is close enough to act: %s\n", getTimeRemaining(), target);
                    else {
                        target = null;
                        mission = null;
                    }

                    if (target != null)
                        mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                }

                // move high-strength units to nearby frontiers
                if (site.strength > 200) {
                    target = getNearestFrontier(friendlyLoc);
                    if (target != null && simMap.getDistance(friendlyLoc, target) < Math.max(gameMap.height, gameMap.width) / 3.0) {
                        out.printf("\t[%s] There is a nearby frontier, and I am strong: %s\n", getTimeRemaining(), target);
                    } else {
                        target = null;
                        mission = null;
                    }
                    if (target != null)
                        mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                }

                // Expanding
                if (mission == null) {
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
                        boolean veryWeak = simMap.getSite(bestAdjacent).strength < (WAIT_FACTOR + 1) * simMap.getSite(bestAdjacent).production;
                        if (expansionMap.getValue(bestAdjacent) >= expansionMap.getValue(bestNearby)
                                || veryWeak
                                ) {
                            target = bestAdjacent;
                            mission = new Mission(friendlyLoc, bestAdjacent, loc -> true);
                            out.printf("\t[%s] Best expansion target is adjacent: %s\n", getTimeRemaining(), target);
                        } else {
                            target = bestNearby;
                            out.printf("\t[%s] Best expansion target is nearby: %s\n", getTimeRemaining(), target);
                            mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                        }
                    } else if (bestAdjacent != null) {
                        target = bestAdjacent;
                        mission = new Mission(friendlyLoc, bestAdjacent, loc -> true);
                        out.printf("\t[%s] Best expansion target is adjacent: %s\n", getTimeRemaining(), target);
                    } else if (bestNearby != null) {
                        target = bestNearby;
                        out.printf("\t[%s] Best expansion target is nearby: %s\n", getTimeRemaining(), target);
                        mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                    } else {
                        out.printf("\t[%s] No expansion targets nearby.\n", getTimeRemaining());
                        mission = null;
                    }
                }

                // no immediate expansion targets found, move toward something
                if (mission == null) {
                    Location climbTarget = bfsBest(friendlyLoc, 1.0, loc -> expansionMap.getValue(loc));
                    if (climbTarget != null) {
                        target = climbTarget;
                        mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                        out.printf("\t[%s] No good priorities, moving outward to climb target: %s\n", getTimeRemaining(), target);
                    } else if (nearestFrontier != null) {
                        target = nearestFrontier;
                        mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                        out.printf("\t[%s] No good priorities, moving outward to frontier: %s\n", getTimeRemaining(), target);
                    } else {
                        target = getNearestBoundary(friendlyLoc);
                        mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                        out.printf("\t[%s] No good priorities, moving outward to boundary: %s\n", getTimeRemaining(), target);
                    }
                }

                // movement
                if (mission.shouldMove()) {
                    Location nextStep = astar.aStarFirstStep(friendlyLoc, mission.target);
                    out.printf("\t[%s] A* says next step is %s\n", getTimeRemaining(), nextStep);
                    Direction direction = simMap.anyMoveToward(friendlyLoc, nextStep);

                    Site nextStepSite = simMap.getSite(friendlyLoc, direction);
                    if (combatParticipants.contains(nextStep)) continue;

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
                                myMoves.add(nextStepMove);
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
                                myMoves.add(nextStepMove);
                                gatherStrength += nextStepSite.strength;
                            }
                            // gather
                            for (Location nextNeighbor : simMap.getNeighbors(nextStep)) {
                                if (nonFrontiers.contains(nextNeighbor) && !friendlyBoundaries.contains(nextNeighbor)) {
                                    Site nextNeighborSite = simMap.getSite(nextNeighbor);
                                    if (nextNeighborSite.strength > nextNeighborSite.production * WAIT_FACTOR) {
                                        if (nextNeighborSite.strength + gatherStrength < GameMap.MAX_STRENGTH) {
                                            Move gatherMove = new Move(nextNeighbor, simMap.anyMoveToward(nextNeighbor, nextStep), myID);
                                            myMoves.add(gatherMove);
                                            nonFrontiers.remove(nextNeighbor);
                                        }
                                    }
                                }
                            }

                        }
                    } else {
                        if (!shouldCapture(friendlyLoc, nextStep))
                            // todo gather?
                            direction = Direction.STILL;
                    }

                    move = new Move(friendlyLoc, direction, myID);
                    myMoves.add(move);
                    out.printf("\t[%s] Added move %s\n", getTimeRemaining(), move);
                }
            }

            out.printf("Time left after assigning all moves: %s\n", getTimeRemaining());

            out.printf("Moves: %s\n", myMoves);
            Networking.sendFrame(myMoves);
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

    private boolean isEnemy(Site site) {
        return site.owner != myID && site.owner != GameMap.NEUTRAL_OWNER;
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

    private Location getNearestBoundary(Location loc) {
        Location nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Location boundary : friendlyBoundaries) {
            double distance = gameMap.getDistance(loc, boundary);
            if (distance < minDist) {
                minDist = distance;
                nearest = boundary;
            }
        }
        return nearest;
    }
}
