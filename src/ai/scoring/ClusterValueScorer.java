package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Site;

public class ClusterValueScorer implements Scorer {
    private Site targetSite;

    public ClusterValueScorer() {
        targetSite = null;
    }

    public ClusterValueScorer(Site targetSite) {
        this.targetSite = targetSite;
    }

    @Override
    public double score(Context context, Action action) {
        Site site;
        if(targetSite == null)
            site = action.getTargetSite();
        else
            site = action.getTargetSite();

        return (Math.max(site.clusterAcquisitionScore, 1)) / (double)(9 * GameMap.MAX_PRODUCTION);
    }
}
