package ai.scoring;


import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Site;

public class StrengthAdvantageScorer implements Scorer {

    private boolean invert;

    public StrengthAdvantageScorer() {
        this(false);
    }

    public StrengthAdvantageScorer(boolean invert) {
        this.invert = invert;
    }

    @Override
    public double score(Context context, Action action) {
        Site agentSite = context.gameMap.getSite(context.agentLocation);
        Site targetSite = context.gameMap.getSite(context.agentLocation, action.getStepDirection());

        double score;
        if (invert)
            score = (Math.max(targetSite.strength - agentSite.strength, 0)) / (double) GameMap.MAX_STRENGTH;
        else
            score = (Math.max(agentSite.strength - targetSite.strength, 0)) / (double) GameMap.MAX_STRENGTH;

        return score;
    }
}
