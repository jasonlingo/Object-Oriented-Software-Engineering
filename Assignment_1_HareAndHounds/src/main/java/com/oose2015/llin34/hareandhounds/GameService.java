/**
 * This class structure is based on TodoService.java.
 * @author Li-Yi Lin
 */

package com.oose2015.llin34.hareandhounds;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

public class GameService {

    private Sql2o db;

    private static int gameIds = 0;

    private static int playerIds = 0;

    public List<Game> Games;

    private final Logger logger = LoggerFactory.getLogger(GameService.class);


    /**
     * Construct the model with a pre-defined datasource. The current implementation
     * also ensures that the DB schema is created if necessary.
     *
     * @param dataSource
     */
    public GameService(DataSource dataSource) throws GameServiceException {
        this.Games = new ArrayList<>();
        db = new Sql2o(dataSource);

        //Create the schema for the database if necessary. This allows this
        //program to mostly self-contained. But this is not always what you want;
        //sometimes you want to create the schema externally via a script.
        try (Connection conn = db.open()) {
            String sql = "CREATE TABLE IF NOT EXISTS game (game_id INTEGER PRIMARY KEY, " +
                                                          "bigger_player_id INTEGER, " +
                                                          "game_json STRING, game_state INTEGER)";
            conn.createQuery(sql).executeUpdate();

            // Load existing but not finished games.
            loadGames();

            // Update the counters for game id and player id if some games are
            // already stored in the database.
            int val = 0;
            sql = "SELECT MAX(game_id) FROM game";
            if (conn.createQuery(sql).executeScalar(Integer.class) != null) {
                val = conn.createQuery(sql).executeScalar(Integer.class) + 1;
                this.gameIds = val;
            }

            sql = "SELECT MAX(bigger_player_id) FROM game";
            if (conn.createQuery(sql).executeScalar(Integer.class) != null){
                val = conn.createQuery(sql).executeScalar(Integer.class) + 1;
                this.playerIds = val;
            }

        } catch(Sql2oException ex) {
            logger.error("Failed to create schema at startup", ex);
            throw new GameServiceException("Failed to create schema at startup", ex);
        }
    }


    /**
     * Fetch all Game entries in the list
     * @return List of all Game entries
     */
    public void loadGames() throws GameServiceException {
        // Only reload games that are not finished yet (game_state <= 2).
        String sql = "SELECT game_json FROM game WHERE game_state <= 2";
        try (Connection conn = db.open()) {
            List<String> gameStrings =  conn.createQuery(sql)
                .executeAndFetch(String.class);

            // Restore games from query result.
            Gson gson = new Gson();
            for (String gameString: gameStrings){
                Game game = gson.fromJson(gameString, Game.class);
                this.Games.add(game);
            }

        } catch(Sql2oException ex) {
            logger.error("GameService.findAll: Failed to query database", ex);
            throw new GameServiceException("GameService.findAll: Failed to query database", ex);
        }
    }


    /**
     * Use binary search to find the desired game in the Games list.
     * @param gameId
     * @return the target game; null if game not found
     * @throws GameServiceException
     */
    public Game searchGame(int gameId) throws GameServiceException{
        int start = 0;
        int end = Games.size() - 1;

        try {
            while (start <= end){
                int mid = start + (end - start) / 2;
                int midId = Games.get(mid).getGameId();
                if (midId == gameId){
                    return Games.get(mid);
                } else if (midId < gameId){
                    start = mid + 1;
                } else {
                    end = mid - 1;
                }
            }
            return null;
        } catch(NumberFormatException ex) {
            logger.error("GameService.searchGame: Game not found", ex);
            throw new GameServiceException("GameService.searchGame: Game not found", ex);
        }
    }


    /**
     * Create a new game.
     * @param body
     * @return
     * @throws GameServiceException
     */
    public GameInfo createNewGame(String body) throws GameServiceException {
    	String pieceType = body.contains("HOUND")? "HOUND":"HARE";
        Game game = new Game(this.gameIds, pieceType, this.playerIds);
        GameInfo gameInfo = new GameInfo(this.gameIds, this.playerIds, pieceType, game.getState());

        // Insert this game to database
        String gameJson = new Gson().toJson(game);

        String sql = "INSERT INTO game (game_id, bigger_player_id, game_json, game_state) " +
                                "VALUES ( :gameId, :bigger_player_id, :game_json, :game_state)";

        try (Connection conn = db.open()) {
            // Insert this game into database.
        	conn.createQuery(sql)
                .addParameter("gameId", this.gameIds)
                .addParameter("bigger_player_id", this.playerIds)
                .addParameter("game_json", gameJson)
                .addParameter("game_state", game.getStateInt())
                .executeUpdate();

            // Add this game to the Games list for future query.
            this.Games.add(game);

            // Increase the static variables.
            this.gameIds += 1;
            this.playerIds += 1;

            return gameInfo;
        } catch(Sql2oException ex) {
            logger.error("GameService.createNewGame: Failed to create new game", ex);
            throw new GameServiceException("GameService.createNewGame: Failed to create new game", ex);
        }
    }


    /**
     * Join a game according to a given gameId.
     * @param gameId
     * @return the GameInfo of a joined game
     * @throws GameServiceException
     */
    public GameInfo joinGame(String gameId) throws GameServiceException{
        try {
            int gId = Integer.parseInt(gameId);

            // Create return GameInfo.
            GameInfo gameInfo = new GameInfo(gId, this.playerIds, "", "");

            // Find the game.
            Game game = searchGame(gId);
            if (game == null){
                gameInfo.state = "404";
                return gameInfo;
            }

            // Join the game.
            String pieceType = game.joinGame(this.playerIds);
            if (pieceType == null) {
                gameInfo.state = "410";
                return gameInfo;
            }

            gameInfo.state = game.getState();
            gameInfo.pieceType = pieceType;

            // Increase the static variable playerIds for next player.
            this.playerIds += 1;

            updateGameDB(game);

            return gameInfo;
        } catch(NumberFormatException ex) {
            logger.error("GameService.searchGame: Game id is wrong", ex);
            throw new GameServiceException("GameService.searchGame: Game id is wrong", ex);
        }
    }


    /**
     * Check the move and make the move.
     * @param move
     * @return status code
     * @throws GameServiceException
     */
    public String playGame(Move move) throws GameServiceException{
        try {
            // Find the game.
            Game game = searchGame(move.getGameId());
            if (game == null) { return "INVALID_GAME_ID"; }

            // Check turns.
            String check = game.checkTurns(move.getPlayerId());
            if (!check.equals("OK")) { return check; }

            // Move this piece.
            if (!game.movePiece(move)){ return "ILLEGAL_MOVE"; }

            updateGameDB(game);
            return "MOVE_OK";

        } catch(GameServiceException ex) {
            logger.error("GameService.movePiece: Move fail", ex);
            throw new GameServiceException("GameService.searchGame: Move fail", ex);
        }
    }


    /**
     * Search the target game and return its game info.
     * @param gameId
     * @return gameInfo
     */
    public GameInfo getGameState(String gameId) throws GameServiceException{
        try {
            int gId = Integer.parseInt(gameId);
            Game game = searchGame(gId);
            if (game == null){
                return null;
            }
            return new GameInfo(gId, 0, "", game.getState());
        } catch(NumberFormatException ex) {
            logger.error("GameService.searchGame: Game id is wrong", ex);
            throw new GameServiceException("GameService.searchGame: Game id is wrong", ex);
        }
    }


    /**
     * Search the target game and return its board info (the position of each pieces).
     * @param gameId
     * @return a list of PieceInfo
     * @throws GameServiceException
     */
    public List<Piece> getGameBoard(String gameId) throws GameServiceException{
        try {
            int gId = Integer.parseInt(gameId);
            Game game = searchGame(gId);
            if (game == null) {
                return null;
            }
            return game.getPieces();
        } catch(NumberFormatException ex) {
            logger.error("GameService.searchGame: Game id is wrong", ex);
            throw new GameServiceException("GameService.searchGame: Game id is wrong", ex);
        }
    }


    /**
     * Update the status of a game in database.
     * @param game
     * @throws GameServiceException
     */
    public void updateGameDB(Game game) throws GameServiceException{
        String gameJson = new Gson().toJson(game);

        int biggerPlayerId = game.getHareId() > game.getHoundId()? game.getHareId():game.getHoundId();

        String sql = "UPDATE game SET game_json = :game_json, bigger_player_id = :bigger_player_id" +
                ", game_state = :game_state WHERE game_id = :game_id";
        try (Connection conn = db.open()) {
            conn.createQuery(sql)
                    .addParameter("game_json", gameJson)
                    .addParameter("bigger_player_id", biggerPlayerId)
                    .addParameter("game_state", game.getStateInt())
                    .addParameter("game_id", game.getGameId())
                    .executeUpdate();
        } catch(Sql2oException ex) {
            logger.error("Failed to update game database", ex);
            throw new GameServiceException("Failed to update game database", ex);
        }
    }

    //-----------------------------------------------------------------------------//
    // Helper Classes and Methods
    //-----------------------------------------------------------------------------//

    public static class GameServiceException extends Exception {
        public GameServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
