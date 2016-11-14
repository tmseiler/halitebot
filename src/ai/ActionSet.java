package ai;

import game.Move;

import java.util.ArrayList;

import static util.Logger.out;

public class ActionSet {
    private ArrayList<Action> actions;

    public ActionSet() {
        actions = new ArrayList<>(0);
    }

    public void add(Action action) {
        out.printf("Adding action %s\n", action);
        actions.add(action);
    }

    public Move evaluate() {
        double bestScore = 0.0;
        Move bestMove = null;
        for (Action action : actions) {
            double score = action.getScore();
            out.printf("Action %s has score %s\n", action, score);
            if(score > bestScore) {
                bestScore = score;
                bestMove = action.perform();
                out.printf("Best action is now %s (%s)\n", action, score);
            }
        }
        return bestMove;
    }
}
