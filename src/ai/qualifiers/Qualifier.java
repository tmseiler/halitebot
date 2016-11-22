package ai.qualifiers;

import ai.actions.Action;
import ai.scoring.Context;
import ai.scoring.Scorer;

import java.util.ArrayList;

import static util.Logger.out;

public class Qualifier {
    protected Context context;
    protected final Action action;
    protected final ArrayList<Scorer> scorers;
    protected final ArrayList<Scorer> weightScorers;

    public Qualifier(Context context, Action action) {
        this.context = context;
        this.action = action;
        scorers = new ArrayList<>(0);
        weightScorers = new ArrayList<>(0);
    }

    public void addScorer(Scorer scorer) {
        scorers.add(scorer);
    }

    public void addWeightScorer(Scorer scorer) {
        weightScorers.add(scorer);
    }

    public double getScore() {
        double scoreSum = 0.0;
        for (Scorer scorer: scorers) {
            scoreSum += scorer.score(context, action);
//            out.printf("\tscoreSum is now %s\n", scoreSum);
        }
        double weightedScore = scoreSum / scorers.size();
//        out.printf("\tweightedScore is %s before weighting\n", weightedScore);
        for (Scorer weightedScorer : weightScorers) {
            weightedScore *= weightedScorer.score(context, action);
        }
//        out.printf("\tweightedScore is now %s after weighting\n", weightedScore);
        return weightedScore;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public String toString() {
        return getClass() + "{" +
                "action=" + action +
                '}';
    }
}
