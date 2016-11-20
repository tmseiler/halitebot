package ai.scoring;


import ai.actions.Action;
import game.GameMap;
import game.Site;

import static util.Logger.out;

public class StrengthAdvantageScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        Site friendlySite = context.gameMap.getSite(context.agentLocation);
        Site enemySite = context.gameMap.getSite(context.agentLocation, action.getTargetDirection());
//        out.printf("\t\tfriendlySite(%s), enemySite(%s), %s\n", friendlySite, enemySite, action.getTargetDirection());
//        out.printf("\t\tfriendlyLocation is %s, enemyLocation is %s\n", context.agentLocation, action.getTarget());
        double score = (Math.max(friendlySite.strength - enemySite.strength, 0)) / (double) GameMap.MAX_STRENGTH;
//        out.printf("\t\tscore is %s (%s, %s)\n", score, friendlySite.strength, enemySite.strength);
        return score;
    }
}
