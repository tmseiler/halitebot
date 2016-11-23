package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;

public class ClusterValueScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        return (action.getTargetSite().clusterAcquisitionScore) / (4 * GameMap.MAX_PRODUCTION);
    }
}
