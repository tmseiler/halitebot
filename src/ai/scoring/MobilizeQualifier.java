package ai.scoring;

import ai.actions.Action;

public class MobilizeQualifier extends Qualifier {
    public MobilizeQualifier(Context context, Action action) {
        super(context, action);
        addWeightScorer(new FriendlinessScorer());
        addWeightScorer(new RipenessScorer());
        addWeightScorer(new Scorer() {
            @Override
            public double score(Context context, Action action) {
                if(context.getAgentEnemyNeighbors().size() > 0)
                    return 0;
                else
                    return 1.0;
            }
        });
        addScorer(new BoundaryProximityScorer());
    }
}
