/**
 * This class structure is based on TodoController.java.
 * @author Li-Yi Lin
 */

package com.oose2015.llin34.hareandhounds;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static spark.Spark.*;

public class GameController {

    private static final String API_CONTEXT = "/hareandhounds/api/games";

    private final GameService gameService;

    private final Logger logger = LoggerFactory.getLogger(GameController.class);

    public GameController(GameService gameService) {
        this.gameService = gameService;
        setupEndpoints();
    }

    private void setupEndpoints() {
        // Create new game
        post(API_CONTEXT, "application/json", (request, response) -> {
            try {
                if (request.body().contains("HOUND") ||
                    request.body().contains("HARE")     ){
                    // The pieceType should be "HOUND" or "HARE."
                    GameInfo gameInfo = gameService.createNewGame(request.body());
                    if (gameInfo != null){
                        response.status(201);
                        return gameInfo;
                    }
                }
                response.status(400);
            } catch (GameService.GameServiceException ex) {
                logger.error("Failed to create new game!");
                response.status(500);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        // Get game board
        get(API_CONTEXT + "/:gameId" + "/board", "application/json", (request, response) -> {
            try {
                List<Piece> pieces = gameService.getGameBoard(request.params(":gameId"));
                if (pieces != null) {
                    response.status(200);
                    return pieces;
                } else {
                    response.status(404);
                }
            } catch (GameService.GameServiceException ex) {
                logger.error(String.format("Failed to find the game's board with gameId: %s", request.params(":gameId")));
                response.status(500);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        // Get game state
        get(API_CONTEXT + "/:gameId" + "/state", "application/json", (request, response) -> {
            try {
                GameInfo gameInfo = gameService.getGameState(request.params(":gameId"));
                if (gameInfo != null) {
                    response.status(200);
                    return gameInfo;
                } else {
                    response.status(404);
                }
            } catch (GameService.GameServiceException ex) {
                logger.error(String.format("Failed to find the game with gameId: %s", request.params(":gameId")));
                response.status(500);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        // Join a game
        put(API_CONTEXT + "/:gameId", "application/json", (request, response) -> {
            try {
                GameInfo gameInfo = gameService.joinGame(request.params(":gameId"));
                if (gameInfo.state.equals("404")) {
                    response.status(404);
                } else if (gameInfo.state.equals("410")) {
                    response.status(410);
                } else {
                    response.status(200);
                    return gameInfo;
                }
                return Collections.EMPTY_MAP;
            } catch (GameService.GameServiceException ex) {
                logger.error("Failed to fetch the list of games!");
                response.status(500);
                return Collections.EMPTY_MAP;
            }
        }, new JsonTransformer());

        // Play game
        post(API_CONTEXT + "/:gameId" + "/turns", "application/json", (request, response) -> {
            try {
                // create a move object from json.
                Move move = new Gson().fromJson(request.body(), Move.class);
                String moveResult = gameService.playGame(move);
                switch (moveResult){
                    case "MOVE_OK":
                        response.status(200);
                        break;
                    case "INVALID_GAME_ID":
                        //response.status(404);
                        //break;
                    case "INVALID_PLAYER_ID":
                        response.status(404);
                        break;
                    case "INCORRECT_TURN":
                        response.status(422);
                        break;
                    case "ILLEGAL_MOVE":
                        response.status(422);
                        break;
                }
                return moveResult;

            } catch (GameService.GameServiceException ex) {
                logger.error("Failed to create new game!");
                response.status(500);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());
    }
}
