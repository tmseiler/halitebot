package game;

import static util.Logger.out;

public class DiffusionMap {
    private double[][] map;
    private GameMap gameMap;

    public DiffusionMap(GameMap gameMap) {
        this.gameMap = gameMap;
        map = new double[gameMap.width][gameMap.height];
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Site site = gameMap.getSite(new Location(x, y));
                if(site.isFriendly) {
                    map[x][y] = 0.0;
                } else {
                    map[x][y] = (float) site.individualAcquisitionScore();
                }
            }
        }

        int diffusionCount = Math.max(gameMap.width, gameMap.height) / 2;
        for (int i = 0; i < diffusionCount; i++) {
            diffuse();
        }
//        for (int i = 0; i < gameMap.height; i++) {
//            for (int j = 0; j < gameMap.width; j++) {
//                out.printf("%-8.2f", map[i][j]);
//            }
//            out.printf("\n");
//        }
    }

    private void diffuse() {
        double coefficient = .05;
        double[][] newmap = new double[gameMap.width][gameMap.height];
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                double cellVal = 0.0;
                cellVal += map[getIndex(y + 1, gameMap.height)][getIndex(x, gameMap.width)] - map[y][x];
                cellVal += map[getIndex(y - 1, gameMap.height)][getIndex(x, gameMap.width)] - map[y][x];
                cellVal += map[getIndex(y, gameMap.height)][getIndex(x + 1, gameMap.width)] - map[y][x];
                cellVal += map[getIndex(y, gameMap.height)][getIndex(x - 1, gameMap.width)] - map[y][x];
                cellVal *= coefficient;
                cellVal += map[y][x];
                newmap[y][x] = cellVal;
            }
        }
        this.map = newmap;
    }

    private static int getIndex(int index, int bounds) {
        if (index < 0) return index + bounds;
        else return index % bounds;
    }

    public double getValue(int x, int y) {
        return map[x][y];
    }

    public double getValue(Location loc) {
        return getValue(loc.x, loc.y);
    }
}
