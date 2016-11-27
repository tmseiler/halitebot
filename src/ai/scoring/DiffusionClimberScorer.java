package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Location;
import game.Site;

public class DiffusionClimberScorer implements Scorer {
    private static double COEFFICIENT = 0.3;
    private Location targetLoc;


    public DiffusionClimberScorer(Location bestLoc) {
        targetLoc = bestLoc;
    }

    public DiffusionClimberScorer() {
        targetLoc = null;
    }

    @Override
    public double score(Context context, Action action) {
        Location loc;
        if(targetLoc == null)
            loc = action.getTarget();
        else
            loc = targetLoc;

        double unnormalizedValue = Math.pow(Math.max(context.diffusionMap.getValue(loc), .001), COEFFICIENT);
        return unnormalizedValue / Math.pow(GameMap.MAX_PRODUCTION, COEFFICIENT);
    }
}
