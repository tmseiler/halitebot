import ai.actions.MoveAction;
import ai.actions.WaitAction;
import ai.qualifiers.AttackQualifier;
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

        Networking.sendInit("Dahlia mk5");

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
                    if (isFriendly(site)) {
                        friendlyLocations.add(location);
                        if (isBoundary(location))
                            friendlyBoundaries.add(location);
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

            for (Location friendlyLoc : friendlyLocations) {
                out.printf("\n%s\n", friendlyLoc);
                Context context = new Context(friendlyLoc, gameMap, friendlyBoundaries, myID, projectionMap);
                ActionSelector selector = new ActionSelector(context, new WaitAction(context));
                for (Direction direction : Direction.CARDINALS) {
                    selector.add(new AttackQualifier(context, new MoveAction(context, direction)));
                    selector.add(new ReinforceQualifier(context, new MoveAction(context, direction)));
                    selector.add(new MobilizeQualifier(context, new MoveAction(context, direction)));
                }

                Move evaluatedMove = selector.evaluate();
                projectionMap.evaluateMove(myID, evaluatedMove);
                moves.add(evaluatedMove);
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
