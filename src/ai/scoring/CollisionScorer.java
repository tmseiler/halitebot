package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Site;

public class CollisionScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        Site friendlySite = context.gameMap.getSite(context.agentLocation);
        Site stepSite = context.projectionMap.getSite(action.getStep());
        int wastedStrength;
        if(stepSite.isFriendly) {
            wastedStrength = Math.max(friendlySite.strength + stepSite.strength - GameMap.MAX_STRENGTH, 0);
        } else {
            wastedStrength = 0;
        }

        return 1.0 - ((double)wastedStrength/(double)GameMap.MAX_STRENGTH);
    }
}
