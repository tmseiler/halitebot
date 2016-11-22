package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.actions.MoveAction;
import ai.scoring.*;

public class ReinforceQualifier extends Qualifier {
    public ReinforceQualifier(Context context, MoveAction moveAction) {
        super(context, moveAction);
        addWeightScorer(new RipenessScorer(5));
        addWeightScorer(new CollisionScorer());
        addWeightScorer(new FriendlinessScorer());
        addWeightScorer(new ClusterValueScorer());
        addWeightScorer(new Scorer(){
            @Override
            public double score(Context context, Action action) {
                if(context.friendlyBoundaries.contains(action.getTarget()))
                    return 1.0;
                else
                    return 0.0;
            }
        });

        addScorer(new StrengthAdvantageScorer(true));
    }
}
