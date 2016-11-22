package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.scoring.*;

public class MobilizeQualifier extends Qualifier {
    public MobilizeQualifier(Context context, Action action) {
        super(context, action);
        addWeightScorer(new FriendlinessScorer());
        addWeightScorer(new RipenessScorer(8));
        addWeightScorer(new Scorer() {
            @Override
            public double score(Context context, Action action) {
                if (context.friendlyBoundaries.contains(context.agentLocation))
                    return 0;
                else
                    return 1.0;
            }
        });
        addScorer(new BoundaryProximityScorer());
    }
}
