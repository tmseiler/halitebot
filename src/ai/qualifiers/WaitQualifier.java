package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.scoring.Scorer;
import game.GameMap;
import game.Location;

public class WaitQualifier extends Qualifier {
    public WaitQualifier(Context context, Action action) {
        super(context, action);
        addScorer(new Scorer() {
            @Override
            public double score(Context context, Action action) {
                int bestProd = 0;
                for (Location enemyNeighbor : context.getAgentEnemyNeighbors()) {
                    bestProd += context.gameMap.getSite(enemyNeighbor).production;
                }
                return bestProd / (GameMap.MAX_PRODUCTION);
            }
        });
    }
}
