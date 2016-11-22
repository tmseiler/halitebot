package ai.scoring;

import ai.Context;
import ai.actions.Action;

public class EnemynessScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        if(context.isFriendly(action.getTargetSite()))
            return 0;
        else
            return 1.0;
    }
}
