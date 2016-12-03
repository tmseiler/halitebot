import ai.actions.MoveAction;
import ai.actions.MoveTowardAction;
import ai.actions.WaitAction;
import ai.qualifiers.*;
import ai.actions.ActionSelector;
import ai.Context;
import game.*;

import static util.Logger.out;

import java.util.ArrayList;
import java.util.Comparator;

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

        Networking.sendInit("Dahlia mk13");

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

            DiffusionMap enemyFrontierMap = new DiffusionMap(gameMap, gameMap -> {
                double[][] newMap = new double[gameMap.width][gameMap.height];
                for (int y = 0; y < gameMap.height; y++) {
                    for (int x = 0; x < gameMap.width; x++) {
                        Site site = gameMap.getSite(new Location(x, y));
                        if  (site.owner == GameMap.NEUTRAL_OWNER && site.strength == 0) {
                            newMap[x][y] = site.production;
                        } else {
                            newMap[x][y] = 0.0;
                        }
                    }
                }
                return newMap;
            }, true);

            friendlyBoundaries.sort(Comparator.comparingDouble(expansionMap::getValue));

            for (Location friendlyLoc : friendlyLocations) {
                out.printf("\n%s (%s strength)\n", friendlyLoc, gameMap.getSite(friendlyLoc).strength);
                Context context = new Context(friendlyLoc, gameMap, friendlyBoundaries,
                        myID, projectionMap, expansionMap, enemyFrontierMap);
                ArrayList<ActionSelector> selectors = new ArrayList<>(0);

                MoveAction stillAction = new MoveAction(context, Direction.STILL);

                ActionSelector attackSelector = new ActionSelector(context, stillAction);
                ActionSelector mobilizationSelector = new ActionSelector(context, stillAction);
                ActionSelector expandSelector = new ActionSelector(context, stillAction);
                ActionSelector reinforcementSelector = new ActionSelector(context, stillAction);

                selectors.add(attackSelector);
                selectors.add(expandSelector);
                selectors.add(reinforcementSelector);

//                if((double)friendlyLocations.size() > (double)gameMap.width * gameMap.height / 8.0)
//                    selectors.add(mobilizationSelector);

                for (Direction direction : Direction.CARDINALS) {
                    MoveAction moveAction = new MoveAction(context, direction);
                    expandSelector.add(new ExpandQualifier(context, moveAction));
                }

                reinforcementSelector.add(new ReinforceQualifier(context, new MoveTowardAction(context, context.getBestBoundary())));

                Move decidedMove = null;
                for (ActionSelector selector : selectors) {
                    Move evaluatedMove = selector.evaluate();
                    if (evaluatedMove != null) {
                        decidedMove = evaluatedMove;
                        break;
                    }
                }

                if (decidedMove == null) {
                    out.printf("%s performing default action.\n", context.agentLocation);
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
