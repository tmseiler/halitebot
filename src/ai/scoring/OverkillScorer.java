package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.GameMap;
import game.Location;
import game.Site;

public class OverkillScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        double damageSum = 0.0;
        Site agentSite = context.gameMap.getSite(context.agentLocation);
        for (Location neighbor : context.gameMap.getNeighbors(action.getTarget())) {
            Site site = context.gameMap.getSite(neighbor);
            if( (!site.isFriendly) && site.owner != GameMap.NEUTRAL_OWNER)
                damageSum += Math.min(agentSite.strength, site.strength);
        }
        return damageSum / 4.0 * (double)GameMap.MAX_STRENGTH;
    }
}
