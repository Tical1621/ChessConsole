package org.example;

import org.example.board.Board;
import org.example.board.BoardConsoleRenderer;
import org.example.board.BoardFactory;
import org.example.game.Game;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) {

//        Board board = new Board();
//        board.setupDefaultPiecePositions();
        Board board = (new BoardFactory()).fromFEN("k2R4/8/8/8/2P1P3/2PKP3/2PPP3/8 w - - 0 1");
        BoardConsoleRenderer renderer = new BoardConsoleRenderer();
//        renderer.render(board);
//
//        Piece piece = board.getPiece(new Coordinates(File.G,8));
//        Set<Coordinates> moves = piece.getAvailableMoveSquares(board);
//        int x = 123;

        Game game = new Game(board);
        game.gameLoop();

    }
}
