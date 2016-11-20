package ai.scoring;

import ai.actions.Action;
import game.Site;

public class RipenessScorer implements Scorer {
    @Override
    public double score(Context context, Action action) {
        Site agentSite = context.gameMap.getSite(context.agentLocation);
        if(agentSite.strength > agentSite.production * 3)
            return 1;
        else
            return 0;
    }
}
