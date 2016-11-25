package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Site;

public class ProductionScorer implements Scorer {
    private static double COEFFICIENT = 0.3;
    @Override
    public double score(Context context, Action action) {
        Site targetSite = action.getTargetSite();
        double unnormalizedValue = Math.pow(Math.max(targetSite.production, .001), COEFFICIENT);
        return unnormalizedValue / Math.pow(GameMap.MAX_PRODUCTION, COEFFICIENT);
    }
}
