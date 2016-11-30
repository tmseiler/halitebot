package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.scoring.*;

public class AttackQualifier extends Qualifier {
    public AttackQualifier(Context context, Action action) {
        super(context, action);
        addScorer(new OverkillScorer());
        addWeightScorer(new ProductionScorer());
        addWeightScorer(new RipenessScorer(8));
        addWeightScorer(new CollisionScorer());
    }
}
