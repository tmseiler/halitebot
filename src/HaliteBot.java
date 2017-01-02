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
    ArrayList<Location> enemyCombatants;
    PriorityQueue<Location> nonFrontiers;
    DiffusionMap expansionMap;
    ArrayList<Location> combatParticipants;
    boolean contactMade = false;

    void run() {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("Dahlia mk31");


        while (true) {
            String frameString = Networking.getString();
            turnStartTime = System.currentTimeMillis();
            log("\n\nNew turn");
            ArrayList<Move> myMoves = new ArrayList<>();

            gameMap = Networking.deserializeGameMap(frameString);
            log("created maps");

            friendlyLocations = new ArrayList<>(0);
            friendlyBoundaries = new ArrayList<>(0);
            friendlyFrontiers = new ArrayList<>(0);
            enemyCombatants = new ArrayList<>(0);
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
                        if (isFrontier(location) && gameMap.getSite(location).owner != GameMap.NEUTRAL_OWNER) {
                            if (!enemyCombatants.contains(location))
                                enemyCombatants.add(location);
//                            for (Location frontierNeighbor : gameMap.getNeighbors(location)) {
//                                if (gameMap.getSite(frontierNeighbor).owner == gameMap.getSite(location).owner) {
//                                    if (!enemyCombatants.contains(frontierNeighbor))
//                                        enemyCombatants.add(frontierNeighbor);
//                                }
//                            }
                        }
                    }
                }
            }

            if (friendlyFrontiers.size() > 0) contactMade = true;
            WAIT_FACTOR = contactMade ? 7 : 5;

            log("preprocessed map");

            ArrayList<Move> enemyMoves = new ArrayList<>(0);
            for (Location enemyCombatant : enemyCombatants) {
                Site combatantSite = gameMap.getSite(enemyCombatant);
                int bestCount = 0;
                if (!isFrontier(enemyCombatant)) {
                    Location nearestFrontier = bfsFirst(enemyCombatant, 2.0, loc -> {
                        Site s = gameMap.getSite(loc);
                        return s.owner == GameMap.NEUTRAL_OWNER && s.strength == 0 ? 1.0 : 0.0;
                    });

                    log("Nearest frontier to enemy %s is %s", enemyCombatant, nearestFrontier);

                    Move move = new Move(enemyCombatant,
                            gameMap.anyMoveToward(enemyCombatant, nearestFrontier),
                            combatantSite.owner);
//                    log("Adding enemy move %s (non-frontier)", move);
                    enemyMoves.add(move);
                    continue;
                }

                Direction bestDirection = Direction.STILL;

                for (Direction d : Direction.CARDINALS) {
                    int count = 0;
                    Location targetLoc = gameMap.getLocation(enemyCombatant, d);

                    if (!isFrontier(targetLoc)) continue;
                    count++;

                    for (Location targetNeighbor : gameMap.getNeighbors(targetLoc)) {
                        Site neighborSite = gameMap.getSite(targetNeighbor);
                        if (neighborSite.owner != combatantSite.owner && neighborSite.owner != GameMap.NEUTRAL_OWNER)
                            count++;
                    }

                    if (count > bestCount) {
                        bestCount = count;
                        bestDirection = d;
                    }
                }
                Move move = new Move(enemyCombatant, bestDirection, combatantSite.owner);
//                log("Adding enemy move %s (frontier)", move);

                enemyMoves.add(move);
            }
            log("Assumed enemy moves: %s", enemyMoves);

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
                    log("\tSimulated turn %s -> %s: score %s", frontierLoc, frontierMove.dir, score);


                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = frontierMove;
                        log("\tBest move for %s is now %s: score %s", frontierLoc, frontierMove.dir, score);
                    }
                }

                log("Best move for %s (%s strength, %s prod) is %s", frontierLoc, site.strength, site.production, bestMove.dir);
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

            log("Executed diffusion");

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
                log("\n%s (%s strength, %s prod)", friendlyLoc, currentSite.strength, currentSite.production);

                Mission mission = null;
                Move move;

                // goal selection
                Location target = null;

                // move toward the nearest frontier if it's very close
                Location nearestFrontier = getNearestFrontier(friendlyLoc);
                log("\tNearest frontier is: %s", target);
                if (nearestFrontier != null) {
                    target = nearestFrontier;
                    double distance;
                    if (makeGoodDecisions)
                        distance = astar.pathDistance(friendlyLoc, target);
                    else
                        distance = simMap.getDistance(friendlyLoc, target);

                    if (distance < 7.0)
                        log("\tNearest frontier is close enough to act: %s", target);
                    else {
                        target = null;
                        mission = null;
                    }

                    if (target != null)
                        mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                }

                // Expanding
                if (mission == null) {
                    Location bestAdjacent = bfsBest(friendlyLoc, 1.0, loc -> {
                        Site s = gameMap.getSite(loc);
                        if (s.owner == GameMap.NEUTRAL_OWNER)
                            return expansionMap.getValue(loc);
                        else
                            return 0.0;
                    });
                    log("\tbestAdjacent is %s", bestAdjacent);

                    Location bestNearby = bfsBestFriendlyBoundary(friendlyLoc, 1);

                    log("\tbest nearby friendly is %s", bestNearby);
                    if (bestAdjacent != null && bestNearby != null) {
                        boolean veryWeak = simMap.getSite(bestAdjacent).strength < (WAIT_FACTOR + 1) * simMap.getSite(bestAdjacent).production;
                        if (expansionMap.getValue(bestAdjacent) >= expansionMap.getValue(bestNearby)
                                || veryWeak
                                ) {
                            target = bestAdjacent;
                            log("\tBest expansion target is adjacent: %s", target);
                            mission = new Mission(friendlyLoc, bestAdjacent, loc -> true);
                        } else {
                            target = bestNearby;
                            log("\tBest expansion target is nearby: %s", target);
                            mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                        }
                    } else if (bestAdjacent != null) {
                        target = bestAdjacent;
                        log("\tBest expansion target is adjacent: %s", target);
                        mission = new Mission(friendlyLoc, bestAdjacent, loc -> true);
                    } else if (bestNearby != null) {
                        target = bestNearby;
                        log("\tBest expansion target is nearby: %s", target);
                        mission = new Mission(friendlyLoc, target, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                    } else {
                        log("\tNo expansion targets nearby.");
                        mission = null;
                    }
                }

                if (mission == null) {
                    Location bestExpTarget = bfsBest(friendlyLoc, 3.0, loc -> {
                        Site s = gameMap.getSite(loc);
                        if (s.owner == GameMap.NEUTRAL_OWNER)
                            return expansionMap.getValue(loc);
                        else
                            return 0.0;
                    });
                    Location climbTarget = bfsBest(friendlyLoc, 1.0, loc -> expansionMap.getValue(loc));


                    if (bestExpTarget != null) {
                        log("\tInterior unit moving toward expansion area %s", bestExpTarget);
                        mission = new Mission(friendlyLoc, bestExpTarget, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                    } else if (nearestFrontier != null) {
                        log("\tInterior unit moving toward nearest frontier: %s", nearestFrontier);
                        mission = new Mission(friendlyLoc, nearestFrontier, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                    } else if (climbTarget != null) {
                        log("\tInterior unit moving climbing diffusion map: %s", nearestFrontier);
                        mission = new Mission(friendlyLoc, climbTarget, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                    } else {
                        Location nearestBoundary = getNearestBoundary(friendlyLoc);
                        log("\tInterior unit moving to nearest boundary: %s", nearestBoundary);
                        mission = new Mission(friendlyLoc, nearestBoundary, loc -> simMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                    }
                }

                // movement
                if (mission.shouldMove()) {
                    Location nextStep = astar.aStarFirstStep(friendlyLoc, mission.target);
                    log("\tA* says next step is %s", nextStep);
                    Direction direction = simMap.anyMoveToward(friendlyLoc, nextStep);

                    Site nextStepSite = simMap.getSite(friendlyLoc, direction);
                    if (combatParticipants.contains(nextStep)) continue;

                    if (isFriendly(nextStepSite)) {
                        int capWaste = (site.strength + nextStepSite.strength) - GameMap.MAX_STRENGTH;
                        log("\tCap waste is (%s + %s) - 255 = %s", site.strength, nextStepSite.strength, capWaste);

                        if (capWaste > 20) {
                            if (nonFrontiers.contains(nextStep)
                                    && nextStepSite.strength < site.strength
                                    && nextStepSite.strength < 200) {
                                log("\tSwapping with %s", nextStep);
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
                                log("\tTelling %s to stay put", nextStep);
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
                        if (!shouldCapture(friendlyLoc, nextStep)) {
                            direction = Direction.STILL;
                        }
                    }

                    move = new Move(friendlyLoc, direction, myID);
                    myMoves.add(move);
                    log("\tAdded move %s", move);
                }
            }

            log("Assigned all moves");

            log("Moves: %s", myMoves);
            Networking.sendFrame(myMoves);
        }

    }

    private Location bfsBestFriendlyBoundary(Location location, double maxDistance) {
//        log("\tbfsBestFriendlyBoundary %s", location);
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
//        log("\tbfsBest bestLoc is %s", bestLoc);
        return bestLoc;
    }

    private Location bfsFirst(Location loc, double maxDistance, BFSScorer scorer) {
//        log("\tbfsFirst %s", loc);
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
//        log("\tbfsFirst bestLoc is %s", bestLoc);
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

    private void log(String fmtString, Object... oldArgs) {
        StringBuilder builder = new StringBuilder();
        builder.append("[%s] ");
        builder.append(fmtString);
        builder.append("\n");
        Object[] stringArgs = new Object[oldArgs.length + 1];
        stringArgs[0] = getTimeRemaining();
        System.arraycopy(oldArgs, 0, stringArgs, 1, oldArgs.length);
        out.printf(builder.toString(), stringArgs);
    }
}
