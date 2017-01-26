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
    private int turnNumber = 0;
    private int turnsLeft = 0;
    double AGGRESSION_DISTANCE = 8.0;
    int bestProd = Integer.MIN_VALUE;


    private ArrayList<Location> friendlyLocations;
    private ArrayList<Location> friendlyBoundaries;
    private PriorityQueue<Location> friendlyFrontiers;
    private ArrayList<Location> enemyCombatants;
    private PriorityQueue<Location> nonFrontiers;
    private PriorityQueue<Location> expansionTargets;
    private DiffusionMap expansionMap;
    private ArrayList<Location> combatParticipants;
    private boolean contactMade = false;

    void run() {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("Dahlia mk37");


        while (true) {
            String frameString = Networking.getString();
            turnStartTime = System.currentTimeMillis();
            log("New turn");
            ArrayList<Move> myMoves = new ArrayList<>();

            gameMap = Networking.deserializeGameMap(frameString);
            log("created maps");

            turnsLeft = 10 * Double.valueOf(Math.sqrt(gameMap.width * gameMap.height)).intValue() - turnNumber;
            log("%s turns left", turnsLeft);

            friendlyLocations = new ArrayList<>(0);
            friendlyBoundaries = new ArrayList<>(0);
            enemyCombatants = new ArrayList<>(0);
            combatParticipants = new ArrayList<>(0);
            friendlyFrontiers = new PriorityQueue<>((loc1, loc2) -> {
                Site s1 = gameMap.getSite(loc1);
                Site s2 = gameMap.getSite(loc2);
                if (s1.strength > s2.strength) return -1;
                else if (s1.strength == s2.strength) return 0;
                else return 1;
            });

            nonFrontiers = new PriorityQueue<>((loc1, loc2) -> {
                Site s1 = gameMap.getSite(loc1);
                Site s2 = gameMap.getSite(loc2);
                if (s1.strength > s2.strength) return -1;
                else if (s1.strength == s2.strength) return 0;
                else return 1;
            });

            expansionTargets = new PriorityQueue<>((loc1, loc2) -> {
                double v1 = expansionMap.getValue(loc1);
                double v2 = expansionMap.getValue(loc2);
                if (v1 > v2) return -1;
                else if (v1 == v2) return 0;
                else return 1;
            });

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

            // pre-process locations
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Location location = new Location(x, y);
                    Site site = gameMap.getSite(location);
                    if(site.production > bestProd) bestProd = site.production;
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
                        }
                    }
                }
            }

            if (friendlyFrontiers.size() > 0) contactMade = true;
            WAIT_FACTOR = contactMade ? 7 : 5;
            int COMBAT_WAIT_FACTOR = WAIT_FACTOR;

            log("preprocessed map");

            ArrayList<Move> enemyMoves = new ArrayList<>(0);
            predictEnemyMoves(enemyMoves);

            GameMap locMap = gameMap.copy();
            for (Location frontierLoc : friendlyFrontiers) {
                int bestScore = 0;
                ArrayList<Move> allMoves = new ArrayList<>();
                allMoves.addAll(enemyMoves);
                allMoves.addAll(myMoves);
                locMap = gameMap.copy();

                locMap.simulateTurn(myID, allMoves);
                Site site = gameMap.getSite(frontierLoc);
                if (site.strength < site.production * COMBAT_WAIT_FACTOR) {
                    nonFrontiers.add(frontierLoc);
                    continue;
                }

                Move bestMove = null;
                for (Direction d : Direction.DIRECTIONS) {
                    Location moveLoc = locMap.getLocation(frontierLoc, d);
                    Site moveSite = locMap.getSite(moveLoc);
                    int capWaste = locMap.getMovedStrength(moveLoc, myID) + site.strength - GameMap.MAX_STRENGTH;

                    GameMap simMap = locMap.copy();
                    log("\tcapWaste for %s attacking %s is %s", frontierLoc, d, capWaste);
                    if ((moveSite.strength != 0 && moveSite.owner == GameMap.NEUTRAL_OWNER)
                            || capWaste > 25)
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
                if (bestMove != null) {
                    log("Best move for %s (%s strength, %s prod) is %s", frontierLoc, site.strength, site.production, bestMove.dir);
                    myMoves.add(bestMove);
                    combatParticipants.add(frontierLoc);
                } else {
                    nonFrontiers.add(frontierLoc);
                }

            }

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
                log("%s (%s strength, %s prod)", friendlyLoc, currentSite.strength, currentSite.production);

                Mission mission = null;
                Move move;

                // goal selection
                Location target = null;

                // move toward the nearest frontier if it's very close
                Location nearestFrontier = getNearestFrontier(friendlyLoc);
                Location climbTarget = bfsBest(friendlyLoc, 1.0, loc -> expansionMap.getValue(loc));
//                Location bestExpansion = getBestGlobalExpansion();
                Location bestBoundary = getBestBoundary();
                Location bestNearbyNeutral = bfsBest(friendlyLoc, 2.0, loc -> {
                    Site s = gameMap.getSite(loc);
                    if (s.owner == GameMap.NEUTRAL_OWNER)
                        return s.individualAcquisitionScore();
                    else
                        return 0.0;
                });

                Location bestAdjacent = bfsBest(friendlyLoc, 1.0, loc -> {
                    Site s = gameMap.getSite(loc);
                    if (s.owner == GameMap.NEUTRAL_OWNER)
                        return s.individualAcquisitionScore();
                    else
                        return 0.0;
                });

                Location nearestBoundary = getNearestBoundary(friendlyLoc);

                Location bestNearby = bfsBestFriendlyBoundary(friendlyLoc, 1);

                log("\tNearest frontier is: %s", nearestFrontier);
                if (nearestFrontier != null) {
                    target = nearestFrontier;
                    double distance;
                    if (makeGoodDecisions)
                        distance = astar.pathDistance(friendlyLoc, target);
                    else
                        distance = simMap.getDistance(friendlyLoc, target);

                    if (distance < AGGRESSION_DISTANCE) {
                        log("\tNearest frontier is close enough to act: %s (%s)", target, distance);
                        mission = new Mission(friendlyLoc, target, loc -> gameMap.getSite(loc).strength > WAIT_FACTOR * simMap.getSite(loc).production);
                    } else {
                        log("\tNearest frontier is too far: %s (%s)", target, distance);
                    }
                }

                // Expanding
                if (mission == null && bestAdjacent != null) {
                    log("\tbestNearbyNeutral is %s", bestNearbyNeutral);
                    log("\tbestAdjacent is %s", bestAdjacent);
                    log("\tBest nearby friendly is %s", bestNearby);
                    int adjacentROI = getCaptureROI(gameMap.getSite(bestAdjacent));

                    // always capture if climb target is the best
                    boolean efficient = site.strength < site.production * WAIT_FACTOR;
//                    boolean veryWeak = site.strength < 25 && site.production > 2;
//                    boolean veryProductive = site.production > .7 * bestProd;

                    boolean goodEnoughToGetNow = adjacentROI > 0 && (efficient);
                    if (shouldCapture(friendlyLoc, bestAdjacent) && (bestAdjacent == climbTarget || goodEnoughToGetNow)) {
                        log("\tadjacent is pretty good, capturing it. Expected ROI:", adjacentROI);
                        mission = new Mission(friendlyLoc, bestAdjacent, loc -> gameMap.getSite(loc).strength > 0);
                    } else {
                        log("\tgoing to bestNearby %s instead of adjacent", bestNearby);
                        mission = new Mission(friendlyLoc, bestNearby, loc -> {
                            Site s = gameMap.getSite(loc);
                            return !isFriendly(s) || s.strength > WAIT_FACTOR * s.production;
                        });
                    }

                }

                if (mission == null) {
                    log("\tNo combat or expansion targets found.");
                    Site climbSite = gameMap.getSite(climbTarget);
                    if(isFriendly(climbSite)) {
                        mission = new Mission(friendlyLoc, climbTarget, loc -> gameMap.getSite(loc).strength > WAIT_FACTOR * gameMap.getSite(loc).production);
                    } else if (nearestFrontier != null) {
                        mission = new Mission(friendlyLoc, nearestFrontier, loc -> gameMap.getSite(loc).strength > WAIT_FACTOR * gameMap.getSite(loc).production);
                    } else {
                        mission = new Mission(friendlyLoc, nearestBoundary, loc -> gameMap.getSite(loc).strength > WAIT_FACTOR * gameMap.getSite(loc).production);
                    }

                    log("\tInterior unit moving to: %s", mission.target);
                }

                // movement
                if (mission.shouldMove()) {
                    List<Location> path = astar.aStar(friendlyLoc, mission.target);

                    Location nextStep = path.get(0);
                    log("\tA* says next step is %s", nextStep);
                    Direction direction = simMap.anyMoveToward(friendlyLoc, nextStep);

                    Site nextStepSite = simMap.getSite(friendlyLoc, direction);
                    if (combatParticipants.contains(nextStep)) continue;

                    if (isFriendly(nextStepSite)) {
                        int capWaste = (site.strength + nextStepSite.strength) - GameMap.MAX_STRENGTH;
                        log("\tCap waste is (%s + %s) - 255 = %s", site.strength, nextStepSite.strength, capWaste);

                        if (capWaste > 10) {
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
                            if (nonFrontiers.contains(nextStep)
                                    && nextStepSite.strength < 200) {
                                log("\tTelling %s to stay put because it is on the way", nextStep);
                                Move nextStepMove = new Move(nextStep, Direction.STILL, myID);
                                nonFrontiers.remove(nextStep);
                                myMoves.add(nextStepMove);
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
            turnNumber++;
        }

    }

    private void predictEnemyMoves(ArrayList<Move> enemyMoves) {
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
//                    double val = nextSite.individualAcquisitionScore();
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
        double bestVal = Double.MIN_VALUE;

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

    private boolean isDangerous(Location loc) {
        for (Location neighbor : gameMap.getNeighbors(loc)) {
            int owner = gameMap.getSite(neighbor).owner;
            if (owner != GameMap.NEUTRAL_OWNER && owner != myID) return true;
        }
        return false;
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

    private Location getBestBoundary() {
        double bestVal = 0.0;
        Location bestLoc = null;
        for (Location loc : friendlyBoundaries) {
            double val = expansionMap.getValue(loc);
            if (val > bestVal) {
                bestVal = val;
                bestLoc = loc;
            }
        }
        return bestLoc;
    }

    private Location getBestGlobalExpansion() {
        double bestVal = 0.0;
        Location bestLoc = null;
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                double val = expansionMap.getValue(x, y);
                if (val > bestVal) {
                    bestVal = val;
                    bestLoc = new Location(x, y);
                }
            }
        }
        return bestLoc;
    }

    private int getCaptureROI(Site site) {
        return turnsLeft * site.production - site.strength;
    }

    private void log(String fmtString, Object... oldArgs) {
        String builder = "[%s][%s] " + fmtString + "\n";
        Object[] stringArgs = new Object[oldArgs.length + 2];
        stringArgs[0] = turnNumber;
        stringArgs[1] = getTimeRemaining();
        System.arraycopy(oldArgs, 0, stringArgs, 2, oldArgs.length);
        out.printf(builder, stringArgs);
    }
}
