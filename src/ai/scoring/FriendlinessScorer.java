package ai.scoring;

import ai.actions.Action;

public class FriendlinessScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        if(context.isFriendly(action.getTargetSite()))
            return 1.0;
        else
            return 0;
    }
}
