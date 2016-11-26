package ai.scoring;

import ai.Context;
import ai.actions.Action;

public class BoundarynessScorer implements Scorer {

    @Override
    public double score(Context context, Action action) {
        if (context.friendlyBoundaries.contains(action.getTarget()))
            return 1.0;
        else
            return 0.0;
    }
}
