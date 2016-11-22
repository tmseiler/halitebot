package ai.scoring;

import ai.actions.Action;
import game.GameMap;

public class ClusterValueScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        return (4 * GameMap.MAX_PRODUCTION - action.getTargetSite().clusterAcquisitionScore) / (4 * GameMap.MAX_PRODUCTION);
    }
}
