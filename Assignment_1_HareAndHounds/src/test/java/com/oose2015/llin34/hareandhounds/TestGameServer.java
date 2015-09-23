/**
 * This testing framework is based on TestTodoServer.java
 * @author: Li-Yi Lin
 */

package com.oose2015.llin34.hareandhounds;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sqlite.SQLiteDataSource;
import spark.Spark;
import spark.utils.IOUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.*;
import com.google.gson.Gson;

import org.junit.*;
import static org.junit.Assert.*;

public class TestGameServer {

    //------------------------------------------------------------------------//
    // Setup
    //------------------------------------------------------------------------//

    @Before
    public void setup() throws Exception {
        //Clear the database and then start the server
        clearDB();

        //Start the main server
        Bootstrap.main(null);
        Spark.awaitInitialization();
    }

    @After
    public void tearDown() {
        //Stop the server
        clearDB();
        Spark.stop();
    }

    //------------------------------------------------------------------------//
    // Tests
    //------------------------------------------------------------------------//

    //Add a few games
    GameInfo[] games = new GameInfo[]{
            new GameInfo(0, 0, "HOUND", ""),
            new GameInfo(0, 0, "HOUND", ""),
            new GameInfo(0, 0, "HARE", "")
    };

    // An ArrayList that stores Game info of created games.
    ArrayList<GameInfo> createGameInfo = new ArrayList<>();

    // An ArrayList that stores Game info of joined games.
    ArrayList<GameInfo> joinGameInfo = new ArrayList<>();

    Gson gson = new Gson();

    @Test
    public void testGameBasic() throws Exception {

        // Create games
        for (GameInfo gi: games) {
            Response response = request("POST", "/hareandhounds/api/games", gi);
            assertEquals("Failed to create a game", 201, response.httpStatus);
            // Transform reply data to GameInfo and store replied game info for other test.
            GameInfo reply = gson.fromJson(response.content, GameInfo.class);
            createGameInfo.add(reply);
        }

        // Get game state
        for (GameInfo gi: createGameInfo) {
            Response response = request("GET", "/hareandhounds/api/games/" + gi.gameId + "/state", null);
            assertEquals("Failed to get game state", 200, response.httpStatus);
            GameInfo reply = gson.fromJson(response.content, GameInfo.class);
            assertEquals("Failed to get game state", "WAITING_FOR_SECOND_PLAYER", reply.state);
        }

        // Test get game state of a non-exist game id.
        Response r = request("GET", "/hareandhounds/api/games/100/state", null);
        assertEquals("Failed to show error message when query a game state of a non-exist game id.", 404, r.httpStatus);

        // Get game board
        for (GameInfo gi: createGameInfo){
            r = request("GET", "/hareandhounds/api/games/" + gi.gameId + "/board", null);
            assertEquals("Failed to get game board", 200, r.httpStatus);
        }

        // Join game
        for (GameInfo gi: createGameInfo){
            r = request("PUT", "/hareandhounds/api/games/" + gi.gameId, null);
            assertEquals("Failed to join a game", 200, r.httpStatus);
            // Transform reply data to GameInfo and store replied game info for other test.
            GameInfo reply = gson.fromJson(r.content, GameInfo.class);
            joinGameInfo.add(reply);
        }

        // Test move
        for (GameInfo gi: createGameInfo){
            Move move = new Move(gi.gameId, gi.playerId, "0", "1", "1", "1");
            r = request("POST", "/hareandhounds/api/games/" + gi.gameId + "/turns", move);
            if (gi.pieceType.equals("HOUND")){
                assertEquals("Failed to move", 200, r.httpStatus);
            } else {
                assertEquals("Failed to prohibit HARE from move first", 422, r.httpStatus);
            }
        }

        // Test move more than one step
        for (GameInfo gi: joinGameInfo){
            if (gi.pieceType.equals("HARE")) {
                Move move = new Move(gi.gameId, gi.playerId, "4", "1", "2", "1");
                r = request("POST", "/hareandhounds/api/games/" + gi.gameId + "/turns", move);
                assertEquals("Failed to prohibit from moving more than one step", 422, r.httpStatus);
            }
        }

        // Test Hound move backward
        GameInfo hound = createGameInfo.get(2); // This is Hound player
        GameInfo hare = joinGameInfo.get(2);    // This is Hare player
        Move move = new Move(hound.gameId, hound.playerId, "0", "1", "1", "1");           //Hound moves forward
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        move = new Move(hare.gameId, hare.playerId, "4", "1", "3", "1");                  //Hare moves
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        move = new Move(hound.gameId, hound.playerId, "1", "1", "0", "1");                //Hound moves backward
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        // Should get error message about Hound's moving backward
        assertEquals("Failed to prohibit Hound from moving backward", 422, r.httpStatus);

    }

    @Test
    public void testHareWin(){
        // Create games
        for (GameInfo gi: games) {
            Response r = request("POST", "/hareandhounds/api/games", gi);
            assertEquals("Failed to create a game", 201, r.httpStatus);
            // Transform reply data to GameInfo and store replied game info for other test.
            GameInfo reply = gson.fromJson(r.content, GameInfo.class);
            createGameInfo.add(reply);
        }

        // Join game
        for (GameInfo gi: createGameInfo){
            Response r = request("PUT", "/hareandhounds/api/games/" + gi.gameId, null);
            assertEquals("Failed to join a game", 200, r.httpStatus);
            // Transform reply data to GameInfo and store replied game info for other test.
            GameInfo reply = gson.fromJson(r.content, GameInfo.class);
            joinGameInfo.add(reply);
        }

        GameInfo hound = createGameInfo.get(0);
        GameInfo hare = joinGameInfo.get(0);
        // Hound moves from (0,1) to (1,1)
        Move move = new Move(hound.gameId, hound.playerId, "0", "1", "1", "1");
        Response r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (4,1) to (3,1)
        move = new Move(hare.gameId, hare.playerId, "4", "1", "3", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (1,0) to (2,0)
        move = new Move(hound.gameId, hound.playerId, "1", "0", "2", "0");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (3,1) to (2,1)
        move = new Move(hare.gameId, hare.playerId, "3", "1", "2", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (2,0) to (3,0)
        move = new Move(hound.gameId, hound.playerId, "2", "0", "3", "0");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (2,1) to (2,0)
        move = new Move(hare.gameId, hare.playerId, "2", "1", "2", "0");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (1,2) to (2,2)
        move = new Move(hound.gameId, hound.playerId, "1", "2", "2", "2");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (2,0) to (1,0) and Hare wins
        move = new Move(hare.gameId, hare.playerId, "2", "0", "1", "0");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Get game state
        r = request("GET", "/hareandhounds/api/games/" + hound.gameId + "/state", null);
        assertEquals("Failed to get game state", 200, r.httpStatus);
        GameInfo reply = gson.fromJson(r.content, GameInfo.class);
        assertEquals("Failed to get game state", "WIN_HARE_BY_ESCAPE", reply.state);

    }

    @Test
    public void testHoundWin(){
        // Create games
        for (GameInfo gi: games) {
            Response r = request("POST", "/hareandhounds/api/games", gi);
            assertEquals("Failed to create a game", 201, r.httpStatus);
            // Transform reply data to GameInfo and store replied game info for other test.
            GameInfo reply = gson.fromJson(r.content, GameInfo.class);
            createGameInfo.add(reply);
        }

        // Join game
        for (GameInfo gi: createGameInfo){
            Response r = request("PUT", "/hareandhounds/api/games/" + gi.gameId, null);
            assertEquals("Failed to join a game", 200, r.httpStatus);
            // Transform reply data to GameInfo and store replied game info for other test.
            GameInfo reply = gson.fromJson(r.content, GameInfo.class);
            joinGameInfo.add(reply);
        }

        GameInfo hound = createGameInfo.get(0);
        GameInfo hare = joinGameInfo.get(0);
        // Hound moves from (0,1) to (1,1)
        Move move = new Move(hound.gameId, hound.playerId, "0", "1", "1", "1");
        Response r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (4,1) to (3,1)
        move = new Move(hare.gameId, hare.playerId, "4", "1", "3", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (1,0) to (2,0)
        move = new Move(hound.gameId, hound.playerId, "1", "0", "2", "0");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (3,1) to (4,1)
        move = new Move(hare.gameId, hare.playerId, "3", "1", "4", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (1,1) to (2,1)
        move = new Move(hound.gameId, hound.playerId, "1", "1", "2", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (4,1) to (3,1)
        move = new Move(hare.gameId, hare.playerId, "4", "1", "3", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (1,2) to (2,2)
        move = new Move(hound.gameId, hound.playerId, "1", "2", "2", "2");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (3,1) to (4,1)
        move = new Move(hare.gameId, hare.playerId, "3", "1", "4", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (2,0) to (3,0)
        move = new Move(hound.gameId, hound.playerId, "2", "0", "3", "0");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (4,1) to (3,1)
        move = new Move(hare.gameId, hare.playerId, "4", "1", "3", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (2,2) to (3,2)
        move = new Move(hound.gameId, hound.playerId, "2", "2", "3", "2");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (3,1) to (4,1)
        move = new Move(hare.gameId, hare.playerId, "3", "1", "4", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (2,1) to (3,1)
        move = new Move(hound.gameId, hound.playerId, "2", "1", "3", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Get game state
        r = request("GET", "/hareandhounds/api/games/" + hound.gameId + "/state", null);
        assertEquals("Failed to get game state", 200, r.httpStatus);
        GameInfo reply = gson.fromJson(r.content, GameInfo.class);
        assertEquals("Failed to get game state", "WIN_HOUND", reply.state);
    }

    @Test
    public void testWinByStalling(){
        // Create games
        for (GameInfo gi: games) {
            Response r = request("POST", "/hareandhounds/api/games", gi);
            assertEquals("Failed to create a game", 201, r.httpStatus);
            // Transform reply data to GameInfo and store replied game info for other test.
            GameInfo reply = gson.fromJson(r.content, GameInfo.class);
            createGameInfo.add(reply);
        }

        // Join game
        for (GameInfo gi: createGameInfo){
            Response r = request("PUT", "/hareandhounds/api/games/" + gi.gameId, null);
            assertEquals("Failed to join a game", 200, r.httpStatus);
            // Transform reply data to GameInfo and store replied game info for other test.
            GameInfo reply = gson.fromJson(r.content, GameInfo.class);
            joinGameInfo.add(reply);
        }

        GameInfo hound = createGameInfo.get(0);
        GameInfo hare = joinGameInfo.get(0);
        // Hound moves from (1,0) to (1,1)
        Move move = new Move(hound.gameId, hound.playerId, "1", "0", "1", "1");
        Response r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (4,1) to (3,1)
        move = new Move(hare.gameId, hare.playerId, "4", "1", "3", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (1,1) to (0,1)
        move = new Move(hound.gameId, hound.playerId, "1", "1", "1", "0");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (3,1) to (4,1)
        move = new Move(hare.gameId, hare.playerId, "3", "1", "4", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (1,0) to (1,1)
        move = new Move(hound.gameId, hound.playerId, "1", "0", "1", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (4,1) to (3,1)
        move = new Move(hare.gameId, hare.playerId, "4", "1", "3", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hound moves from (1,1) to (0,1)
        move = new Move(hound.gameId, hound.playerId, "1", "1", "1", "0");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Hare moves from (3,1) to (4,1)
        move = new Move(hare.gameId, hare.playerId, "3", "1", "4", "1");
        r = request("POST", "/hareandhounds/api/games/" + hound.gameId + "/turns", move);
        assertEquals("Failed to get game state", 200, r.httpStatus);

        // Get game state, now we should get "WIN_HARE_BY_STALLING"
        r = request("GET", "/hareandhounds/api/games/" + hound.gameId + "/state", null);
        assertEquals("Failed to get game state", 200, r.httpStatus);
        GameInfo reply = gson.fromJson(r.content, GameInfo.class);
        assertEquals("Failed to test win by stalling", "WIN_HARE_BY_STALLING", reply.state);
    }


 
    //------------------------------------------------------------------------//
    // Generic Helper Methods and classes
    //------------------------------------------------------------------------//
    
    private Response request(String method, String path, Object content) {
        try {
			URL url = new URL("http", Bootstrap.IP_ADDRESS, Bootstrap.PORT, path);
            System.out.println(url);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);
            http.setDoInput(true);
            if (content != null) {
                String contentAsJson = new Gson().toJson(content);
                http.setDoOutput(true);
                http.setRequestProperty("Content-Type", "application/json");
                OutputStreamWriter output = new OutputStreamWriter(http.getOutputStream());
                output.write(contentAsJson);
                output.flush();
                output.close();
            }
            String responseBody;
            if (http.getResponseCode() < 400) {
                responseBody = IOUtils.toString(http.getInputStream());
            } else {
                responseBody = "";
            }
			return new Response(http.getResponseCode(), responseBody);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Sending request failed: " + e.getMessage());
			return null;
		}
    }

        
    private static class Response {

		public String content;
        
		public int httpStatus;

		public Response(int httpStatus, String content) {
			this.content = content;
            this.httpStatus = httpStatus;
		}

        public <T> T getContentAsObject(Type type) {
            return new Gson().fromJson(content, type);
        }
	}

    //------------------------------------------------------------------------//
    // TodoApp Specific Helper Methods and classes
    //------------------------------------------------------------------------//

    private void clearDB() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:hareandhounds.db");

        Sql2o db = new Sql2o(dataSource);

        try (Connection conn = db.open()) {
            String sql = "DROP TABLE IF EXISTS game" ;
            conn.createQuery(sql).executeUpdate();
        }
    }

}