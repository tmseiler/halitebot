package ai.scoring;

import ai.actions.Action;

public interface Scorer {
    double score(Context context, Action action);
}
