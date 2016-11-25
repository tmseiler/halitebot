package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.scoring.ProductionScorer;
import game.Location;
import game.Site;

public class WaitQualifier extends Qualifier {
    public WaitQualifier(Context context, Action action) {
        super(context, action);
        int bestProd = 0;
        Site bestSite = null;

        for (Location enemyNeighbor : context.getAgentEnemyNeighbors()) {
            Site enemySite = context.gameMap.getSite(enemyNeighbor);
            if(enemySite.production > bestProd)
                bestSite = enemySite;
                bestProd = enemySite.production;
        }

        addScorer(new ProductionScorer(bestSite));
    }
}
