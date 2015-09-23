package com.oose2015.llin34.hareandhounds;

/**
 * Created by Li-Yi Lin on 9/6/15.
 */

/**
 * A class that stores the information about a game for returning to the
 * front end.
 */
public class GameInfo {
    public int gameId;
    public int playerId;
    public String pieceType;
    public String state;


    /**
     * Construct a GameInfo.
     * @param gameId
     * @param playerId
     * @param pieceType
     * @param state
     */
    public GameInfo(int gameId, int playerId, String pieceType, String state){
        this.gameId = gameId;
        this.playerId = playerId;
        this.pieceType = pieceType;
        this.state = state;
    }
}