package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.scoring.*;
import game.Site;

public class ExpandQualifier extends Qualifier {
    public ExpandQualifier(Context context, Action action) {
        super(context, action);
        addScorer(new DiffusionClimberScorer(context.expansionMap));
        addWeightScorer(new CollisionScorer());
        addWeightScorer(new EnemynessScorer());
        addWeightScorer((context1, action1) -> {
            Site agentSite = context1.gameMap.getSite(context1.agentLocation);
            Site targetSite = context1.gameMap.getSite(context1.agentLocation, action1.getStepDirection());
            return agentSite.strength > targetSite.strength ? 1.0 : 0.0;
        });
    }
}
