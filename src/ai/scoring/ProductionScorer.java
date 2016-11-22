package ai.scoring;

import ai.actions.Action;
import game.GameMap;
import game.Site;

public class ProductionScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        Site targetSite = action.getTargetSite();
        return (Math.max(targetSite.production, 1)) / (double)GameMap.MAX_PRODUCTION;
    }
}
