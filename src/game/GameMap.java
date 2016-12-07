package game;

import java.util.ArrayList;
import java.util.HashMap;

public class GameMap {
    public ArrayList<ArrayList<Site>> contents;
    public ArrayList<ArrayList<HashMap<Integer, Piece>>> movedPieces;
    public ArrayList<ArrayList<Piece>> unmovedPieces;

    public int width;
    public int height;
    public static final int MAX_PRODUCTION = 16;
    public static final int MAX_SIZE = 50;
    public static final int MAX_DISTANCE = 2 * MAX_SIZE;
    public static final int MAX_STRENGTH = 255;
    public static final int NEUTRAL_OWNER = 0;

    public GameMap() {
        width = 0;
        height = 0;
        contents = new ArrayList<>(0);
    }

    public GameMap(int width_, int height_) {
        width = width_;
        height = height_;
        contents = new ArrayList<>(0);
        for (int y = 0; y < height; y++) {
            ArrayList<Site> row = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                row.add(new Site());
            }
            contents.add(row);
        }
    }

    public boolean inBounds(Location loc) {
        return loc.x < width && loc.x >= 0 && loc.y < height && loc.y >= 0;
    }

    public double getDistance(Location loc1, Location loc2) {
        int dx = Math.abs(loc1.x - loc2.x);
        int dy = Math.abs(loc1.y - loc2.y);

        if (dx > width / 2.0) dx = width - dx;
        if (dy > height / 2.0) dy = height - dy;

        return dx + dy;
    }

    public Location getLocation(Location loc, Direction dir) {
        Location l = new Location(loc);
        if (dir != Direction.STILL) {
            if (dir == Direction.NORTH) {
                if (l.y == 0) l.y = height - 1;
                else l.y--;
            } else if (dir == Direction.EAST) {
                if (l.x == width - 1) l.x = 0;
                else l.x++;
            } else if (dir == Direction.SOUTH) {
                if (l.y == height - 1) l.y = 0;
                else l.y++;
            } else if (dir == Direction.WEST) {
                if (l.x == 0) l.x = width - 1;
                else l.x--;
            }
        }
        return l;
    }

    public Location getLocation(Location loc, Direction d1, Direction d2) {
        return getLocation(getLocation(loc, d1), d2);
    }

    public ArrayList<Location> getNeighbors(Location loc) {
        ArrayList<Location> neighbors = new ArrayList<>(0);
        for (Direction d : Direction.CARDINALS) {
            neighbors.add(getLocation(loc, d));
        }
        return neighbors;
    }

    public Site getSite(Location loc, Direction dir) {
        Location l = getLocation(loc, dir);
        return contents.get(l.y).get(l.x);
    }

    public Site getSite(Location loc) {
        return contents.get(loc.y).get(loc.x);
    }

    public void evaluateMove(int playerId, Move evaluatedMove) {
        if (evaluatedMove.dir == Direction.STILL)
            return;

        Site startingSite = getSite(evaluatedMove.loc);
        Site targetSite = getSite(evaluatedMove.loc, evaluatedMove.dir);
        if (targetSite.owner == playerId) {
            targetSite.strength = Math.max(targetSite.strength + startingSite.strength, MAX_STRENGTH);
        } else {
            if (startingSite.strength < targetSite.strength) {
                targetSite.strength = targetSite.strength - startingSite.strength;
            } else if (startingSite.strength > targetSite.strength) {
                targetSite.strength = startingSite.strength - targetSite.strength;
                targetSite.owner = playerId;
                targetSite.isFriendly = true;
                for (Location neighbor : getNeighbors(getLocation(evaluatedMove.loc, evaluatedMove.dir))) {
                    Site s = getSite(neighbor);
                    if ((!s.isFriendly) && s.owner != GameMap.NEUTRAL_OWNER) {
                        s.strength = Math.max(0, s.strength - targetSite.strength);
                        if (s.strength == 0) {
                            s.owner = GameMap.NEUTRAL_OWNER;
                            s.isFriendly = false;
                        }
                    }
                }
            } else {
                targetSite.strength = 0;
            }
        }
        startingSite.strength = 0;
    }

    private Piece getMovedPiece(Location loc, int owner) {
        return movedPieces.get(loc.y).get(loc.x).get(owner);
    }

    private void movePiece(Piece piece, Location loc) {
        Piece existingPiece = getMovedPiece(loc, piece.owner);
        if (existingPiece == null) {
            movedPieces.get(loc.y).get(loc.x).put(piece.owner, piece);
        } else {
            existingPiece.strength = Math.min(piece.strength + existingPiece.strength, MAX_STRENGTH);
        }
    }

    private Piece getUnmovedPiece(int x, int y) {
        return unmovedPieces.get(y).get(x);
    }

    private Piece getUnmovedPiece(Location loc) {
        return getUnmovedPiece(loc.y, loc.x);
    }

    public void performMoves(ArrayList<Move> moves) {
        movedPieces = new ArrayList<>();
        unmovedPieces = new ArrayList<>();
        // represent board as pieces
        for (int y = 0; y < height; y++) {
            ArrayList<HashMap<Integer, Piece>> unresolvedRow = new ArrayList<>();
            ArrayList<Piece> resolvedRow = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                unresolvedRow.add(new HashMap<>());
                Site s = getSite(new Location(x, y));
                resolvedRow.add(new Piece(s.owner, s.strength));
            }
            movedPieces.add(unresolvedRow);
            unmovedPieces.add(resolvedRow);
        }

        // move pieces and combine
        for (Move move : moves) {
            Location targetLoc = getLocation(move.loc, move.dir);
            if (move.dir != Direction.STILL) {
                Piece piece = getUnmovedPiece(move.loc);
                movePiece(new Piece(piece.owner, 0, true), move.loc);
                unmovedPieces.get(move.loc.y).set(move.loc.x, null);
                movePiece(piece, targetLoc);
            }
        }

        // add production for unmoved, non-neutral pieces
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // add production to str, move to moved pieces
                Piece unmovedPiece = getUnmovedPiece(x, y);
                if (unmovedPiece != null) {
                    Location loc = new Location(x, y);
                    if (unmovedPiece.owner != NEUTRAL_OWNER) {
                        unmovedPiece.strength = Math.min(unmovedPiece.strength + getSite(loc).production, MAX_STRENGTH);
                    }
                    movePiece(unmovedPiece, loc);
                }

            }
        }

        // apply damage
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Location loc = new Location(x, y);
                for (int ownerId : movedPieces.get(y).get(x).keySet()) {
                    Piece piece = getMovedPiece(loc, ownerId);
                    // for each direction
                    damagePieces(piece, loc, true);
                    if (ownerId != NEUTRAL_OWNER) {
                        for (Direction d : Direction.CARDINALS) {
                            // apply damage to non-neutral enemies
                            damagePieces(piece, getLocation(loc, d), false);
                        }
                    }
                }
            }
        }

        // resolve state
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Location loc = new Location(x, y);
                for (Piece piece : getMovedPieces(loc).values()) {
                    piece.strength -= piece.damageTaken;
                    Site site = getSite(loc);
                    if (piece.strength > 0) {
                        site.strength = piece.strength;
                        site.owner = piece.owner;
                        continue;
                    }
                    if (piece.damageTaken > 0) {
                        site.owner = NEUTRAL_OWNER;
                    } else {
                        site.owner = piece.owner;
                    }
                    site.strength = 0;
                }
            }
        }
    }

    private void damagePieces(Piece piece, Location target, boolean damageNeutrals) {
        for (Piece enemyPiece : getMovedPieces(target).values()) {
            if (enemyPiece.owner != piece.owner) {
                if (enemyPiece.owner == NEUTRAL_OWNER && !damageNeutrals)
                    continue;
                enemyPiece.damageTaken += piece.strength;
            }
        }
    }

    private HashMap<Integer, Piece> getMovedPieces(Location loc) {
        return movedPieces.get(loc.y).get(loc.x);
    }
}
