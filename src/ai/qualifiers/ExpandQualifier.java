package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.scoring.*;

public class ExpandQualifier extends Qualifier {
    public ExpandQualifier(Context context, Action action) {
        super(context, action);
        addScorer(new DiffusionClimberScorer(context.acquisitionMap));
        addWeightScorer(new StrengthCostScorer());
//        addWeightScorer(new RipenessScorer(2));
        addWeightScorer(new CollisionScorer());
        addWeightScorer(new EnemynessScorer());
        addWeightScorer(new StrengthAdvantageScorer());
    }
}
