package ai.scoring;

import ai.actions.Action;
import game.GameMap;
import game.Site;

public class StrengthCostScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        Site targetSite = action.getTargetSite();
        double cost = targetSite.strength / (double) Math.max(targetSite.production, 1);
        return 1.0 - (cost / GameMap.MAX_STRENGTH);
    }
}
