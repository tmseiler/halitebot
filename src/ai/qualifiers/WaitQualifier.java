package ai.qualifiers;

import ai.Context;
import ai.actions.Action;
import ai.scoring.*;
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
        addWeightScorer(new BoundarynessScorer());
        addWeightScorer(new StrengthCostScorer(bestSite));
        addScorer(new ClusterValueScorer(bestSite));
    }

    public double getScore() {
        double scoreSum = 0.0;
        for (Scorer scorer: scorers) {
            scoreSum += scorer.score(context, action);
        }

        double weightedScore = scoreSum / scorers.size();
        for (Scorer weightedScorer : weightScorers) {
            weightedScore *= weightedScorer.score(context, action);
        }

        if(weightedScore > .50) {
            return weightedScore;
        } else {
            return 0.0;
        }
    }
}
