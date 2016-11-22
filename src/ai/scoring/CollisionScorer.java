package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Site;

public class CollisionScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        Site friendlySite = context.gameMap.getSite(context.agentLocation);
        Site targetSite = context.projectionMap.getSite(action.getTarget());
        int wastedStrength = Math.max(friendlySite.strength + targetSite.strength - GameMap.MAX_STRENGTH, 0);
        return 1.0 - ((double)wastedStrength/(double)GameMap.MAX_STRENGTH);
    }
}
