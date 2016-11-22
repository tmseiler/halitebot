package ai.scoring;

import ai.Context;
import ai.actions.Action;
import game.Site;

public class RipenessScorer implements Scorer {

    private int ripeness;

    public RipenessScorer(int ripeness) {
        this.ripeness = ripeness;
    }

    @Override
    public double score(Context context, Action action) {
        Site agentSite = context.gameMap.getSite(context.agentLocation);
        if(agentSite.strength > agentSite.production * ripeness)
            return 1;
        else
            return 0;
    }
}
