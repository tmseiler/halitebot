package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Site;

public class StrengthCostScorer implements Scorer {

    private Site targetSite;

    public StrengthCostScorer(Site targetSite) {
        this.targetSite = targetSite;
    }

    public StrengthCostScorer() {
        targetSite = null;
    }

    @Override
    public double score(Context context, Action action) {
        Site site;
        if(targetSite == null)
            site = action.getTargetSite();
        else
            site = targetSite;
        double normalizedStrength = site.strength / GameMap.MAX_STRENGTH;
        double cost = 1.0 / (1 + Math.exp(5 * normalizedStrength - 3.0));
        return cost;
    }
}
