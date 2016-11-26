package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.actions.MoveAction;
import ai.scoring.*;
import game.GameMap;

public class ReinforceQualifier extends Qualifier {
    public ReinforceQualifier(Context context, MoveAction moveAction) {
        super(context, moveAction);
        addWeightScorer(new FriendlinessScorer());
        addWeightScorer(new CollisionScorer());
        addWeightScorer(new RipenessScorer(5));
        addWeightScorer(new BoundarynessScorer());
        addWeightScorer(new Scorer() {
            @Override
            public double score(Context context, Action action) {
                int targetStrength = context.projectionMap.getSite(action.getTarget()).strength;
                return targetStrength / (double)GameMap.MAX_STRENGTH;
            }
        });

        addScorer(new ClusterValueScorer());
    }
}
