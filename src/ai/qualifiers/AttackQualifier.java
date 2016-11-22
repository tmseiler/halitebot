package ai.qualifiers;

import ai.actions.Action;
import ai.scoring.*;

public class AttackQualifier extends Qualifier {
    public AttackQualifier(Context context, Action action) {
        super(context, action);
        addScorer(new StrengthCostScorer());
        addWeightScorer(new EnemynessScorer());
        addWeightScorer(new ClusterValueScorer());
        addWeightScorer(new StrengthAdvantageScorer());

    }
}
