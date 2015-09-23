/**
 * @author: Li-Yi Lin
 */
package com.oose2015.llin34.hareandhounds;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * A class that represents a hare-and-hounds game. 
 */
// implements Serializable
public class Game {

	// Game states mapping.
	private static final HashMap<Integer, String> STATES;
	static
	{
		STATES = new HashMap<Integer, String>();
		STATES.put(0, "TURN_HOUND");
		STATES.put(1, "TURN_HARE");
		STATES.put(2, "WAITING_FOR_SECOND_PLAYER");
		STATES.put(3, "WIN_HARE_BY_ESCAPE");
		STATES.put(4, "WIN_HARE_BY_STALLING");
		STATES.put(5, "WIN_HOUND");
	}


	private int gameId;
	private int gameState;
	private int hareId;
	private int houndId;

	// Pieces' information
	private List<Piece> pieces;
	private boolean[][] board;

	// Storing the stalling state.
	private Map<String, Integer> stalling;


	/**
	 * Construct a new game. Assign a new game id to this game and
	 * player id to the first player.
	 * @param gameId
	 * @param pieceType
	 * @param playerId
	 */
	public Game(int gameId, String pieceType, int playerId) {
		// Assign game id.
		this.gameId = gameId;

		// Assign player id.
		if (pieceType.equals("HARE")){
			this.hareId = playerId;
			this.houndId = -1;
		} else if (pieceType.equals("HOUND")) {
			this.houndId = playerId;
			this.hareId = -1;
		}

		gameState = 2; // WAITING_FOR_SECOND_PLAYER

		// Initialize pieces' position
		this.pieces = new ArrayList<>();
		Piece hare = new Piece("HARE", 4, 1);
		Piece hound1 = new Piece("HOUND", 0, 1);
		Piece hound2 = new Piece("HOUND", 1, 0);
		Piece hound3 = new Piece("HOUND", 1, 2);
		this.pieces.add(hare);
		this.pieces.add(hound1);
		this.pieces.add(hound2);
		this.pieces.add(hound3);

		// Initialize the board.
		this.board = new boolean[5][3];
		this.board[0][1] = true;
		this.board[1][0] = true;
		this.board[1][2] = true;
		this.board[4][1] = true;
		this.stalling = new HashMap<>();

		// Add the initialize board status (pieces' position) into the stalling.
		updateStalling();
	}

	/**
	 * The second player joins this game.
	 * @param playerId
	 * @return the piece type of the second player; null if
	 *         the second player already joined.
	 */
	public String joinGame(int playerId) {
		if (houndId < 0) {
			this.houndId = playerId;
			this.gameState = 0; // Hound moves first.
			return "HOUND";
		} else if (hareId < 0) {
			this.hareId = playerId;
			this.gameState = 0; // Hound moves first.
			return "HARE";
		} else {
			// Second player already joined.
			return null;
		}
	}

	public int getGameId() { return this.gameId; }

	public int getHareId() { return this.hareId; }

	public int getHoundId() { return this.houndId; }
	
	public String getState() { return STATES.get(this.gameState); }

	public int getStateInt() { return this.gameState; }

	public List<Piece> getPieces() { return pieces; }

	/**
	 * Find the piece type of the given player id.
	 * @param playerId
	 * @return piece type; null if not found.
	 */
	public String getPieceType(int playerId) {
		if (playerId == this.houndId) {
			return "HOUND";
		} else if (playerId == this.hareId) {
			return "HARE";
		} else {
			return null;
		}
	}

	/**
	 * Check whether the turn is for the given playerId.
	 * @param playerId
	 * @return check result
	 */
	public String checkTurns(int playerId){
		if (playerId == this.houndId){
			if (this.gameState != 0){ return "INCORRECT_TURN"; }
		} else if (playerId == this.hareId){
			if (this.gameState != 1){ return "INCORRECT_TURN"; }
		} else {
			return "INVALID_PLAYER_ID";
		}

		return "OK";
	}


	/**
	 * Rules:
	 * - The game is turn based with the hound moving first.
	 * - The player with the turn can only move one of his pieces
	 *   per turn. A piece can only move one step at a time and
	 *   it can only move to an empty location connected to its
	 *   current location.
	 * - Hounds cannot move backwards while the hare has no such
	 *   restriction.
	 *
	 * @param move
	 * @return true if move succeed; false otherwise.
	 */
	public boolean movePiece(Move move){
		String pieceType = this.getPieceType(move.getPlayerId());

		// Hounds cannot move backwards.
		if (pieceType.equals("HOUND") && move.getFromX() > move.getToX()){
			return false;
		}

		// Check move to an empty position.
		if (!this.board[move.getToX()][move.getToY()]){
			// Check move only one step.
			if(checkMoveOneStep(move)){
				for(Piece piece: this.pieces){
					// Move the piece.
					if (piece.getX() == move.getFromX() && piece.getY() == move.getFromY()){
						piece.setX(move.getToX());
						piece.setY(move.getToY());
						this.board[move.getFromX()][move.getFromY()] = false;
						this.board[move.getToX()][move.getToY()] = true;
						this.gameState = (this.gameState + 1) % 2;
						updateStalling();
						checkWin(pieceType);
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Update the stalling state of this game.
	 */
	public void updateStalling(){
		ArrayList<Integer> pState = new ArrayList<>();

		// Get the three position of hounds.
		for (int i = 1; i <= 3; i++){
			Piece piece = this.pieces.get(i);
			// Use (x * 10 + y) as the key of each position.
			pState.add(piece.getX() * 10 + piece.getY());
		}

		Collections.sort(pState);
		// Add hare's position to the last of the pState list.
		Piece hare = pieces.get(0);
		pState.add(hare.getX() * 10 + hare.getY());

		// Convert list of integers to a string in order to prevent error when
		// deserializing object form json.
		String stateStr = "";
		for (Integer i: pState){
			stateStr += Integer.toString(i) + "_";
		}

		// Update Stalling state.
		int times = 1;
		if (this.stalling.containsKey(stateStr)){
			times = this.stalling.get(stateStr) + 1;
		}
		this.stalling.put(stateStr, times);
	}


	/**
	 * The turns continue until one of the following (win) conditions occur:
	 * 1.The hare manages to sneak past the hounds. i.e. it moves to a square
	 *   such that there are no hounds to left of it.
	 *   In this case the hare wins.
	 * 2.The hare is trapped such that it has no valid move.
	 *   The hounds win in this case.
	 * 3.The same board position occurs three times over the course of the
	 *   game. In this case the hounds are considered to be stalling and the
	 *   hare wins.
	 *
	 * @param pieceType
	 */
	public void checkWin(String pieceType){
		Piece hare = this.pieces.get(0);

		// 1.Check whether hare is trapped. Only check when hound moves.
		// Hare can only be trapped at (2,0), (2,2) or (4,1) since each of
		// these points has only three outer routes.
		if (pieceType.equals("HOUND")) {
			if (hare.getX() == 4 && hare.getY() == 1) {
				if (this.board[3][0] && this.board[3][1] && this.board[3][2]) {
					this.gameState = 5; // WIN_HOUND
				}
			} else if (hare.getX() == 2 && hare.getY() == 0) {
				if (this.board[1][0] && this.board[2][1] && this.board[3][0]) {
					this.gameState = 5; // WIN_HOUND
				}
			} else if (hare.getX() == 2 && hare.getY() == 2) {
				if (this.board[1][2] && this.board[2][1] && this.board[3][2]) {
					this.gameState = 5; // WIN_HOUND
				}
			}
		}

		// 2.Check whether there is no hound to left of the hare.
		boolean noLeft = true;
		for (int i = 1; i <= 3; i++){
			if (this.pieces.get(i).getX() < hare.getX()){
				noLeft = false;
				break;
			}
		}
		if (noLeft) {
			this.gameState = 3; // WIN_HARE_BY_ESCAPE
		}

		// 3.Check stalling.
		if (this.stalling.containsValue(3)){
			this.gameState = 4; // WIN_HARE_BY_STALLING
		}
	}

	/**
	 * Check whether a move is only one step.
	 * If the sum of current x and y positions equals to a multiple of 2,
	 * it can only move up, down, right, and left (no diagonal move).
	 * @param move
	 * @return true if only move one step; false otherwise.
	 */
	public boolean checkMoveOneStep(Move move){
		// Find the step of move in x and y axes.
		int moveX = Math.abs(move.getFromX() - move.getToX());
		int moveY = Math.abs(move.getFromY() - move.getToY());

		if (moveX > 1){return false;}
		if (moveY > 1){return false;}

		int step = moveX + moveY;
		if ((move.getFromX() + move.getFromY()) % 2 == 0) {
			if (step > 1){ return false; }
		} else {
			if (step > 2){ return false; }
		}

		// pass all check.
		return true;
	}
}

