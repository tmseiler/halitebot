package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Site;

public class ProductionScorer implements Scorer {
    private static double COEFFICIENT = 0.3;
    private Site site;

    public ProductionScorer(Site site) {
        this.site = site;
    }

    public ProductionScorer() {
        this.site = null;
    }

    @Override
    public double score(Context context, Action action) {
        Site targetSite;
        if(site == null) {
            targetSite = action.getTargetSite();
        } else {
            targetSite = site;
        }

        double unnormalizedValue = Math.pow(Math.max(targetSite.production, .001), COEFFICIENT);
        return unnormalizedValue / Math.pow(GameMap.MAX_PRODUCTION, COEFFICIENT);
    }
}
