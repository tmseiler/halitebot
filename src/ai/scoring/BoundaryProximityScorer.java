package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Location;

import static util.Logger.out;

public class BoundaryProximityScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        Location closestBoundary = context.findClosestBoundary();
        if(closestBoundary == null) return 0;
        double distance = context.gameMap.getDistance(action.getTarget(), closestBoundary);
        out.printf("\t%s -> %s: %s\n", action.getTarget(), closestBoundary, distance);
        return ((double)GameMap.MAX_DISTANCE - distance) / (double)GameMap.MAX_DISTANCE;
    }
}
