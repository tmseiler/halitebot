package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.Site;

public class StrengthCostScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        Site targetSite = action.getTargetSite();
        double cost = 1.0 / Math.max(targetSite.strength, 1);
        return cost;
    }
}
