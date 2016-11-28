package ai.qualifiers;

import ai.Context;
import ai.actions.MoveAction;
import ai.scoring.*;

public class ReinforceQualifier extends Qualifier {
    public ReinforceQualifier(Context context, MoveAction moveAction) {
        super(context, moveAction);
        addWeightScorer(new FriendlinessScorer());
        addWeightScorer(new CollisionScorer());
        addWeightScorer(new RipenessScorer(5));
        addWeightScorer(new BoundarynessScorer());
        addScorer(new DiffusionClimberScorer());
    }
}
