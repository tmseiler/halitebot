package ai.actions;

import ai.qualifiers.Qualifier;
import ai.scoring.Context;
import game.Move;

import java.util.ArrayList;

import static util.Logger.out;

public class ActionSelector {
    private ArrayList<Qualifier> actions;
    private Context context;
    private Action defaultAction;

    public ActionSelector(Context context, Action defaultAction) {
        this.context = context;
        this.defaultAction = defaultAction;
        actions = new ArrayList<>(0);
    }

    public void add(Qualifier qualifier) {
        actions.add(qualifier);
    }

    public Move evaluate() {
        double bestScore = 0.0;
        Move bestMove = null;
        Qualifier bestQualifier = null;
        for (Qualifier qualifier : actions) {
            double score = qualifier.getScore();
            out.printf("Action %s has score %s\n", qualifier, score);
            if(score > bestScore) {
                bestScore = score;
                bestMove = qualifier.getAction().perform();
                bestQualifier = qualifier;
            }
        }
        out.printf("Best qualifier is now %s (%s)\n", bestQualifier, bestScore);

        if(bestMove == null || bestScore == 0) {
            out.printf("%s Performing default action.\n", context.agentLocation);
            bestMove = defaultAction.perform();
        }
        return bestMove;
    }
}
