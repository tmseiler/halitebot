package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.scoring.*;

public class MobilizeQualifier extends Qualifier {
    public MobilizeQualifier(Context context, Action action) {
        super(context, action);
        addWeightScorer(new FriendlinessScorer());
        addWeightScorer(new CollisionScorer());

        addWeightScorer(new Scorer() {
            @Override
            public double score(Context context, Action action) {
                if (context.gameMap.getSite(context.agentLocation).strength > 200)
                    return 1.0;
                else
                    return 0;
            }
        });

        addScorer(new DiffusionClimberScorer(context.enemyFrontierMap));
    }
}
