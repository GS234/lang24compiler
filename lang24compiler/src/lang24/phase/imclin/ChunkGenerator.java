package lang24.phase.imclin;
import java.util.List;
import java.util.Vector;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.expr.AstCallExpr;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.*;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.lin.LinCodeChunk;
import lang24.data.mem.*;
import lang24.phase.imcgen.ImcGen;
import lang24.phase.memory.Memory;

// data chunks are added already in memory phase

public class ChunkGenerator implements AstFullVisitor<Object, Object> {
    // function definition: pass argument down the tree, nodes that have stmts will fill it
    @Override
	public Object visit(AstFunDefn funDefn, Object arg) {
		Vector<ImcStmt> code = new Vector<>();
        // if (funDefn.pars != null)
		// 	funDefn.pars.accept(this, arg);
        
        // //this is where (possible) data lies:
        if (funDefn.defns != null)
            funDefn.defns.accept(this, arg);
        
        
            //this is where code lies:
		if (funDefn.stmt != null){
            ImcStmt funStmt = ImcGen.stmtImc.get(funDefn.stmt);
            if(funStmt instanceof ImcSTMTS){
                funStmt.accept(new StmtExtractor(), code);
                // for(ImcStmt s : code){ //debug
                //     System.out.println(s);
                // }
            }
            else{
                // System.out.println(funStmt); //debug
                code.add(funStmt);
            }

            
            // we have everything to build code chunk for this function. So, build it!
            MemFrame mf = Memory.frames.get(funDefn); // get ze frame:
            MemLabel entryLabel = ImcGen.entryLabel.get(funDefn);
            MemLabel exitLabel = ImcGen.exitLabel.get(funDefn);

            //add labels to code:
            code.addFirst(new ImcLABEL(entryLabel));
            // code.add(new ImcLABEL(exitLabel));

            // if at this point code does not contain return statement or jump to exit label, add one:
            ImcStmt lastStmt = code.getLast();
            if(
                !(lastStmt instanceof ImcJUMP) ||
                ((lastStmt instanceof ImcJUMP) && ((ImcJUMP) lastStmt).label != exitLabel)
            )
            {
                ImcExpr expr_dist = new ImcTEMP(mf.RV);
                ImcStmt stmt_move = new ImcMOVE(expr_dist, new ImcCONST(0l));
                ImcStmt stmt_jump = new ImcJUMP(exitLabel); 
                code.add(stmt_move);
                code.add(stmt_jump);   
            }

            LinCodeChunk c = new LinCodeChunk(mf, code, entryLabel,exitLabel);
            ImcLin.addCodeChunk(c);
        }
		return null;
	}

    // call: save arguments in temporary variables, add that to stmts
    @Override
	public Object visit(AstCallExpr callExpr, Object arg) {
		if (callExpr.args != null)
			callExpr.args.accept(this, arg);
		return null;
	}

}

// imc visitor: visits ImcSTMTS (possibly nested), on it's way it fills one single list of statements
class StmtExtractor implements ImcVisitor<ImcStmt, List<ImcStmt>>{
    private final boolean throwErrorOnError = false; // debug stuff
    private void errorMessage(String message){
		System.out.println(message);
		if(this.throwErrorOnError) throw new Report.Error("imclin ) " + message);
	}
    
    public ImcStmt visit(ImcSTMTS stmts, List<ImcStmt> visArg) {
		if(visArg == null){
            this.errorMessage("[!] (StmtExtractor - stmts) error: (argument) list is null");
            return null;
        }
        if(!(visArg instanceof List<ImcStmt>)){
            errorMessage("[!] (StmtExtractor - stmts) error: argument list is of incorrect type: "+visArg.getClass().getName());
            return null;
        }
        
        // if all ok, then:
        for(ImcStmt stmt : stmts.stmts){
            if(stmt instanceof ImcSTMTS){
                stmt.accept(this, visArg); //nothing is returned, accept only
            }
            else{
                // stmt.accept(this, visArg); //visit it (morebitni call expression-i, ki jih je treba popravit)
                // visArg.add(stmt);
                ImcStmt novi = stmt.accept(this, visArg);
                visArg.add(novi);
            }
        }
        return null;
	}

    //statements: we add them here:
    public ImcStmt visit(ImcMOVE move, List<ImcStmt> visArg) { //assign stmt (root) -> pass
        ImcExpr dst_expr = move.dst.accept(new ExprTreeChanger(), visArg);
        ImcExpr src_expr = move.src.accept(new ExprTreeChanger(), visArg);

        //add it:
        ImcStmt move_stmt = new ImcMOVE(dst_expr, src_expr);
        return move_stmt;
    }

    public ImcStmt visit(ImcCJUMP cjump, List<ImcStmt> visArg) {
        ImcExpr condition_expr = cjump.cond.accept(new ExprTreeChanger(), visArg);
        ImcStmt cjump_stmt = new ImcCJUMP(condition_expr, cjump.posLabel, cjump.negLabel);
        return cjump_stmt;
    }
    
    
    public ImcStmt visit(ImcESTMT eStmt, List<ImcStmt> visArg) { //expression statement (root) -> pass
        ImcExpr estmt_expr = eStmt.expr.accept(new ExprTreeChanger(), visArg);
        ImcStmt estmt_stmt = new ImcESTMT(estmt_expr);
        return estmt_stmt;
    }

    public ImcStmt visit(ImcJUMP jump, List<ImcStmt> visArg) {
        return jump;
	}
    public ImcStmt visit(ImcLABEL label, List<ImcStmt> visArg) {
        return label;
    }
}


class ExprTreeChanger implements ImcVisitor<ImcExpr, List<ImcStmt>>{
    //save nested calls into temps, return changed tree; save result, return temp, not call!
    public ImcExpr visit(ImcCALL call, List<ImcStmt> visArg){
        Vector<ImcExpr> newArgs = new Vector<>();
        
        int i = 0;
        for(ImcExpr e : call.args){
            if(i == 0){ //leave static link intact
                newArgs.add(e);
                i = i +1;
                continue;
            }
            ImcExpr e_expr = e.accept(this, visArg); //accept args, save them to temps:
            // konstant ne bi bilo treba - TODO
            // save expression value to temp:
            MemTemp t = new MemTemp();
            ImcExpr temp = new ImcTEMP(t); //create temporary variable to store call values
            // ImcExpr mem_temp = new ImcMEM(temp);
            
            ImcStmt move = new ImcMOVE(temp, e_expr);
            // ImcStmt move = new ImcMOVE(mem_temp, e_expr);
            
            //add 'em
            visArg.add(move);
            // newArgs.add(mem_temp);
            newArgs.add(temp);
        }
        //store result in temp, return temp
        ImcExpr new_call = new ImcCALL(call.label, call.offs, newArgs);

        MemTemp lresult = new MemTemp();
        ImcExpr result = new ImcTEMP(lresult);
        ImcStmt move_result = new ImcMOVE(result, new_call);
        visArg.add(move_result);
        //return no call, new temp:
        return result;
    }

    //override other visits, save nested calls into temps
	// public ImcExpr visit(ImcCALL call, List<ImcStmt> visArg) { //do stuff: move arguments outta call (visit first: nested calls)
    //     ImcExpr call_changed = cha
    //     for(ImcExpr e : call.args){
    //         e.accept(this, visArg);
    //     }
    //     return null;
	// }

    public ImcExpr visit(ImcBINOP binOp, List<ImcStmt> visArg) { //expression (arithmetic) -> pass
		ImcExpr fst = binOp.fstExpr.accept(this, visArg);
		ImcExpr snd = binOp.sndExpr.accept(this, visArg);

        //make new binop, return it
        ImcExpr bin_op = new ImcBINOP(binOp.oper, fst, snd);
        return bin_op;
	}

    public ImcExpr visit(ImcUNOP unOp, List<ImcStmt> visArg) { //expression (arithmetic) -> pass
        ImcExpr sub = unOp.subExpr.accept(this, visArg);
        //make new unop, return it
        ImcExpr un_op = new ImcUNOP(unOp.oper, sub);
		return un_op;
	}

    

    public ImcExpr visit(ImcMEM mem, List<ImcStmt> visArg) { //memory (middle node) -> pass
		ImcExpr mem_expr = mem.addr.accept(this, visArg);
        //make new, return it
        ImcExpr mem_e = new ImcMEM(mem_expr);
        return mem_e;
	}

    
    
    // podatkii
    public ImcExpr visit(ImcCONST constant, List<ImcStmt> visArg) {
        return constant;
    }
    
    public ImcExpr visit(ImcNAME name, List<ImcStmt> visArg) {
        return name;
    }
    
    public ImcExpr visit(ImcTEMP temp, List<ImcStmt> visArg) {
        return temp;
    }
    
}