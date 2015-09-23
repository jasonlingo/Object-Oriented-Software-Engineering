/**
 * @author: Li-Yi Lin
 */

package com.oose2015.llin34.hareandhounds;

/**
 * A class that represents a move.
 */
public class Move {
    private int gameId;
    private int playerId;
    private int fromX;
    private int fromY;
    private int toX;
    private int toY;

    public Move(int gameId, int playerId, String fromX, String fromY, String toX, String toY){
        this.gameId = gameId;
        this.playerId = playerId;
        this.fromX = Integer.parseInt(fromX);
        this.fromY = Integer.parseInt(fromY);
        this.toX = Integer.parseInt(toX);
        this.toY = Integer.parseInt(toY);
    }

    // Return the gameId that this move belongs to.
    public int getGameId(){ return this.gameId; }

    // Return the playerId that this move belongs to.
    public int getPlayerId(){ return this.playerId; }

    // Return the integer value of "fromX" position.
    public int getFromX(){ return this.fromX; }

    // Return the integer value of "fromY" position.
    public int getFromY(){ return this.fromY; }

    // Return the integer value of "toX" position.
    public int getToX(){ return this.toX; }

    // Return the integer value of "toY" position.
    public int getToY(){ return this.toY; }
}
