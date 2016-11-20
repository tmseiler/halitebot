package ai.scoring;

import ai.actions.Action;

public class AttackQualifier extends Qualifier {
    public AttackQualifier(Context context, Action action) {
        super(context, action);
        addScorer(new StrengthAdvantageScorer());
        addWeightScorer(new EnemynessScorer());
    }
}
