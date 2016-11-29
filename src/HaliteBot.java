import ai.actions.MoveAction;
import ai.actions.WaitAction;
import ai.qualifiers.ExpandQualifier;
import ai.actions.ActionSelector;
import ai.Context;
import ai.qualifiers.MobilizeQualifier;
import ai.qualifiers.ReinforceQualifier;
import ai.qualifiers.WaitQualifier;
import game.*;

import static util.Logger.out;

import java.util.ArrayList;

class HaliteBot {
    private int myID;
    private GameMap gameMap;
    private GameMap projectionMap;
    private ArrayList<Location> friendlyLocations;
    private ArrayList<Location> friendlyBoundaries;


    void run() {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("Dahlia mk11");

        while (true) {
            ArrayList<Move> moves = new ArrayList<>();

            String frameString = Networking.getString();
            gameMap = Networking.deserializeGameMap(frameString);
            projectionMap = Networking.deserializeGameMap(frameString);

            out.printf("\n\nNew turn\n");
            friendlyLocations = new ArrayList<>(0);
            friendlyBoundaries = new ArrayList<>(0);

            // pre-process locations
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Location location = new Location(x, y);
                    Site site = gameMap.getSite(location);
                    site.isFriendly = isFriendly(site);
                    if (site.isFriendly) {
                        friendlyLocations.add(location);
                        if (isBoundary(location))
                            friendlyBoundaries.add(location);
                    }
                }
            }

            DiffusionMap acquisitionMap = new DiffusionMap(gameMap, new DiffusionSeeder() {
                @Override
                public double[][] seedMap(GameMap gameMap) {
                    double[][] newMap = new double[gameMap.width][gameMap.height];
                    for (int y = 0; y < gameMap.height; y++) {
                        for (int x = 0; x < gameMap.width; x++) {
                            Site site = gameMap.getSite(new Location(x, y));
                            if (site.isFriendly) {
                                newMap[x][y] = 0.0;
                            } else {
                                newMap[x][y] = site.individualAcquisitionScore();
                            }
                        }
                    }
                    return newMap;
                }
            });

            DiffusionMap enemyFrontierMap = new DiffusionMap(gameMap, new DiffusionSeeder() {
                @Override
                public double[][] seedMap(GameMap gameMap) {
                    double[][] newMap = new double[gameMap.width][gameMap.height];
                    for (int y = 0; y < gameMap.height; y++) {
                        for (int x = 0; x < gameMap.width; x++) {
                            Site site = gameMap.getSite(new Location(x, y));
                            if ((!site.isFriendly) && site.owner != GameMap.NEUTRAL_OWNER) {
                                newMap[x][y] = site.individualAcquisitionScore();
                            } else {
                                newMap[x][y] = 0.0;
                            }
                        }
                    }
                    return newMap;
                }
            });

            for (Location friendlyLoc : friendlyLocations) {
                out.printf("\n%s (%s strength)\n", friendlyLoc, gameMap.getSite(friendlyLoc).strength);
                Context context = new Context(friendlyLoc, gameMap, friendlyBoundaries,
                        myID, projectionMap, acquisitionMap, enemyFrontierMap);
                ArrayList<ActionSelector> selectors = new ArrayList<>(0);

                ActionSelector mobilizationSelector = new ActionSelector(context, new WaitAction(context));
                ActionSelector expandSelector = new ActionSelector(context, new MoveAction(context, Direction.STILL));
                ActionSelector waitSelector = new ActionSelector(context, null);
                ActionSelector reinforcementSelector = new ActionSelector(context, null);
                waitSelector.add(new WaitQualifier(context, new MoveAction(context, Direction.STILL)));

                if((double)friendlyLocations.size() > (double)gameMap.width * gameMap.height / (double)8)
                    selectors.add(mobilizationSelector);
                selectors.add(expandSelector);
                selectors.add(waitSelector);
                selectors.add(reinforcementSelector);

                for (Direction direction : Direction.CARDINALS) {
                    expandSelector.add(new ExpandQualifier(context, new MoveAction(context, direction)));
                    reinforcementSelector.add(new ReinforceQualifier(context, new MoveAction(context, direction)));
                    mobilizationSelector.add(new MobilizeQualifier(context, new MoveAction(context, direction)));
                }

                Move decidedMove = null;
                for (ActionSelector selector : selectors) {
                    Move evaluatedMove = selector.evaluate();
                    if (evaluatedMove != null) {
                        decidedMove = evaluatedMove;
                        break;
                    }
                }

                if (decidedMove == null) {
                    out.printf("%s Performing default action.\n", context.agentLocation);
                    decidedMove = new WaitAction(context).perform();
                }

                projectionMap.evaluateMove(myID, decidedMove);
                moves.add(decidedMove);
            }
            out.printf("Moves: %s\n", moves);
            Networking.sendFrame(moves);
        }
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
}
