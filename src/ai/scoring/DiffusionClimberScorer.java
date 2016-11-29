package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.DiffusionMap;
import game.GameMap;
import game.Location;

public class DiffusionClimberScorer implements Scorer {
    private static double COEFFICIENT = 0.3;
    private Location targetLoc;
    private DiffusionMap diffusionMap;

    public DiffusionClimberScorer(DiffusionMap diffusionMap) {
        this.diffusionMap = diffusionMap;
    }

    @Override
    public double score(Context context, Action action) {
        Location loc;
        if(targetLoc == null)
            loc = action.getTarget();
        else
            loc = targetLoc;

        if(diffusionMap.getValue(loc) < diffusionMap.getValue(context.agentLocation))
            return 0.0;

        double unnormalizedValue = Math.pow(Math.max(diffusionMap.getValue(loc), .001), COEFFICIENT);
        return unnormalizedValue / Math.pow(GameMap.MAX_PRODUCTION, COEFFICIENT);
    }
}
