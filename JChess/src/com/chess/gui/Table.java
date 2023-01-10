package com.chess.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.MoveTransition;
import com.google.common.collect.Lists;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

public class Table {
    private  final JFrame gameFrame;
    private final GameHistoryPanel gameHistoryPanel;
    private final TakenPiecesPanel takenPiecesPanel;
    private final BoardPanel boardPanel;
    private final MoveLog moveLog;
    private Board chessBoard;

    private Tile sourceTile;
    private Tile destinationTile;
    private Piece humanMovedPiece;
    private BoardDirection boardDirection;

    private boolean highlightLegalMoves;
    private final static Dimension OUTER_FRAME_DIMENSION = new Dimension(1280,1024);
    private static final String defaultPieceImagesPath = "obrazki/figury/";
    private final Color lightTileColor = Color.decode("#FFFFFF");
    private final Color darkTileColor = Color.decode("#BFBFBF");

    public Table(){
        gameFrame = new JFrame("Ready to chess");
        gameFrame.setLayout(new BorderLayout());
        gameFrame.setResizable(false);
        gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.chessBoard = Board.createStandardBoard();
        this.gameHistoryPanel = new GameHistoryPanel();
        this.takenPiecesPanel = new TakenPiecesPanel();
        this.boardPanel = new BoardPanel();
        this.moveLog = new MoveLog();
        this.boardDirection = BoardDirection.NORMAL;
        this.highlightLegalMoves = true;
        gameFrame.add(this.takenPiecesPanel, BorderLayout.WEST);
        gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        gameFrame.add(this.gameHistoryPanel, BorderLayout.EAST);
        createSettingsPanel();
        gameFrame.setVisible(true);
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void newGame() {
        moveLog.clear();
        if(chessBoard.currentPlayer().getAlliance().isBlack()){
            boardDirection = boardDirection.opposite();
        }
        chessBoard = Board.createStandardBoard();
        gameHistoryPanel.redo(chessBoard,moveLog);
        takenPiecesPanel.redo(moveLog);
        boardPanel.drawBoard(chessBoard);
        gameFrame.repaint();
    }

    private void flipBoard(){
        boardDirection = boardDirection.opposite();
        boardPanel.drawBoard(chessBoard);
    }

    private void createSettingsPanel(){
        final JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BorderLayout());
        gameHistoryPanel.add(settingsPanel, BorderLayout.SOUTH);
        final JButton newGameButton = new JButton("Nowa Gra");
        newGameButton.addActionListener(e -> newGame());
        settingsPanel.add(newGameButton, BorderLayout.NORTH);
        final JButton flipBoardButton = new JButton("Obróć szachownicę");
        flipBoardButton.addActionListener(e -> flipBoard());
        settingsPanel.add(flipBoardButton, BorderLayout.SOUTH);
        final JCheckBox legalMoveHighlighterCheckbox = new JCheckBox("Podświetl możliwe ruchy",true);
        legalMoveHighlighterCheckbox.addActionListener(e -> highlightLegalMoves = legalMoveHighlighterCheckbox.isSelected());
        settingsPanel.add(legalMoveHighlighterCheckbox,BorderLayout.CENTER);
    }
    public enum BoardDirection{
        NORMAL{
            @Override
            List<TilePanel> traverse(List<TilePanel> boardTiles) {
                return boardTiles;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED{
            @Override
            List<TilePanel> traverse(List<TilePanel> boardTiles) {
                return Lists.reverse(boardTiles);//guava
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };
        abstract List<TilePanel> traverse(final List<TilePanel> boardTiles);
        abstract BoardDirection opposite();
    }

    private class BoardPanel extends JPanel{
        final List<TilePanel> boardTiles;
        BoardPanel(){
            super(new GridLayout(8,8));
            this.boardTiles = new ArrayList<>();
            for(int i=0;i< BoardUtils.NUM_TILES; i++){
                final TilePanel tilePanel = new TilePanel(this,i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
        }
        public void drawBoard(final Board board){
            removeAll();
            for(final TilePanel tilePanel : boardDirection.traverse(boardTiles)){
                tilePanel.drawTile(board);
                add(tilePanel);
            }
            validate();
            repaint();
        }
    }

    public static class MoveLog {
        private final List<Move> moves;

        MoveLog(){
            this.moves = new ArrayList<>();
        }
        public List<Move> getMoves(){
            return this.moves;
        }
        public void addMove(final Move move){
            this.moves.add(move);
        }
        public int size(){
            return this.moves.size();
        }
        public void clear(){
            this.moves.clear();
        }
    }

    private class TilePanel extends JPanel{
        private final int tileId;
        TilePanel(final BoardPanel boardPanel,final int tileId){
            super(new GridBagLayout());
            this.tileId = tileId;
            assignTileColor();
            assignTilePieceIcon(chessBoard);
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    if(isRightMouseButton(e)){
                        sourceTile = null;
                        destinationTile = null;
                        humanMovedPiece = null;
                    }else if(isLeftMouseButton(e)){
                        if(sourceTile == null){
                            sourceTile = chessBoard.getTile(tileId);
                            humanMovedPiece = sourceTile.getPiece();
                            if(humanMovedPiece == null){
                                sourceTile=null;
                            }
                        }else{
                            destinationTile = chessBoard.getTile(tileId);
                            final Move move = Move.MoveFactory.createMove(chessBoard, sourceTile.getTileCoordinate(), destinationTile.getTileCoordinate());
                            final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
                            if(transition.getMoveStatus().isDone()){
                                chessBoard = transition.getTransisitonBoard();
                                moveLog.addMove(move);
                                flipBoard();
                            }
                            sourceTile=null;
                            destinationTile=null;
                            humanMovedPiece=null;
                        }
                        SwingUtilities.invokeLater(() -> {
                            gameHistoryPanel.redo(chessBoard,moveLog);
                            takenPiecesPanel.redo(moveLog);
                            boardPanel.drawBoard(chessBoard);
                        });
                    }
                }

                @Override
                public void mousePressed(final MouseEvent e) {

                }

                @Override
                public void mouseReleased(final MouseEvent e) {

                }

                @Override
                public void mouseEntered(final MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }
            });
            validate();
        }
        public void drawTile(final Board board){
            assignTileColor();
            assignTilePieceIcon(board);
            highlightLegals(board);
            validate();
            repaint();
        }
        private void assignTilePieceIcon(final Board board){
            this.removeAll();
            if(board.getTile(this.tileId).isTileOccupied()){
                try {
                    final BufferedImage image =
                            ImageIO.read(new File(defaultPieceImagesPath + board.getTile(this.tileId).getPiece().getPieceAlliance().toString().charAt(0)+
                                    board.getTile(this.tileId).getPiece().toString()+ ".gif"));
                    add(new JLabel(new ImageIcon(image)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        private void highlightLegals(final Board board){
            if(highlightLegalMoves) {
                for (final Move move : pieceLegalMoves(board)) {
                    Board transitionBoard = move.execute();
                    if((!transitionBoard.currentPlayer().getOpponent().isInCheck() && move.getDestinationCoordinate() == this.tileId)||
                            (board.currentPlayer().getAlliance().isWhite()&&board.currentPlayer().isEnableKingSideCastle(board.currentPlayer().getLegalMoves(), board.currentPlayer().getOpponent().getLegalMoves())&&humanMovedPiece.getPieceType().isKing()&&this.tileId == 62)||
                            (board.currentPlayer().getAlliance().isWhite()&&board.currentPlayer().isEnableQueenSideCastle(board.currentPlayer().getLegalMoves(), board.currentPlayer().getOpponent().getLegalMoves())&&humanMovedPiece.getPieceType().isKing()&&this.tileId == 58)||
                            (board.currentPlayer().getAlliance().isBlack()&&board.currentPlayer().isEnableKingSideCastle(board.currentPlayer().getLegalMoves(), board.currentPlayer().getOpponent().getLegalMoves())&&humanMovedPiece.getPieceType().isKing()&&this.tileId == 6)||
                            (board.currentPlayer().getAlliance().isBlack()&&board.currentPlayer().isEnableQueenSideCastle(board.currentPlayer().getLegalMoves(), board.currentPlayer().getOpponent().getLegalMoves())&&humanMovedPiece.getPieceType().isKing()&&this.tileId == 2)){
                        try {
                            add(new JLabel(new ImageIcon(ImageIO.read(new File("obrazki/inne/kropka.png")))));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        private Collection<Move> pieceLegalMoves(final Board board){
            if(humanMovedPiece != null && humanMovedPiece.getPieceAlliance() == board.currentPlayer().getAlliance()){
                return humanMovedPiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }
        private void assignTileColor() {
            if(BoardUtils.EIGHTH_RANK[this.tileId] || BoardUtils.SIXTH_RANK[this.tileId] ||
                    BoardUtils.FOURTH_RANK[this.tileId] || BoardUtils.SECOND_RANK[this.tileId]){
                setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
            }else if(BoardUtils.SEVENTH_RANK[this.tileId] || BoardUtils.FIFTH_RANK[this.tileId] ||
                    BoardUtils.THIRD_RANK[this.tileId] || BoardUtils.FIRST_RANK[this.tileId]){
                setBackground(this.tileId % 2 != 0 ? lightTileColor : darkTileColor);
            }
        }
    }
}
