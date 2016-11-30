package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.actions.MoveAction;
import ai.scoring.*;
import game.GameMap;
import game.Site;

public class ReinforceQualifier extends Qualifier {
    public ReinforceQualifier(Context context, MoveAction moveAction) {
        super(context, moveAction);
        addWeightScorer(new FriendlinessScorer());
        addWeightScorer(new CollisionScorer());
        addWeightScorer(new RipenessScorer(5));
        addWeightScorer(new Scorer() {
            @Override
            public double score(Context context, Action action) {
                Site friendlySite = context.gameMap.getSite(context.agentLocation);
                Site targetSite = context.projectionMap.getSite(action.getTarget());
                int totalStrength = friendlySite.strength + targetSite.strength;
                return ((double)totalStrength/(double)GameMap.MAX_STRENGTH);
            }
        });
        addScorer(new DiffusionClimberScorer(context.acquisitionMap));
    }
}
