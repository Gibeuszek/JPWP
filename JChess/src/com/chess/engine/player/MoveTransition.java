package com.chess.engine.player;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;

public class MoveTransition {
    private final Board transisitonBoard;
    private final Move move;
    private final MoveStatus moveStatus;

    public MoveTransition(final Board transisitonBoard, final Move move, final MoveStatus movestatus){
        this.transisitonBoard=transisitonBoard;
        this.move=move;
        this.moveStatus=movestatus;
    }
    public MoveStatus getMoveStatus(){
        return this.moveStatus;
    }
    public Board getTransisitonBoard(){
        return this.transisitonBoard;
    }
}
