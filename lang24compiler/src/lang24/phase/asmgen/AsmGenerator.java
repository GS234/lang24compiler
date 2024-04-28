package lang24.phase.asmgen;

import java.util.Vector;

import lang24.common.report.Report;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
import lang24.data.asm.AsmOPER;
import lang24.data.asm.Code;
import lang24.data.imc.code.expr.ImcBINOP;
import lang24.data.imc.code.expr.ImcCALL;
import lang24.data.imc.code.expr.ImcCONST;
import lang24.data.imc.code.expr.ImcMEM;
import lang24.data.imc.code.expr.ImcNAME;
import lang24.data.imc.code.expr.ImcSEXPR;
import lang24.data.imc.code.expr.ImcTEMP;
import lang24.data.imc.code.expr.ImcUNOP;
import lang24.data.imc.code.stmt.ImcCJUMP;
import lang24.data.imc.code.stmt.ImcESTMT;
import lang24.data.imc.code.stmt.ImcJUMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcMOVE;
import lang24.data.imc.code.stmt.ImcSTMTS;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.lin.LinCodeChunk;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemTemp;

//TODO TODO TODO TODO TODO TODO TODO TODO TODO

public class AsmGenerator {
    private final LinCodeChunk codeChunk;
    public AsmGenerator(LinCodeChunk codeChunk){
        this.codeChunk = codeChunk;
    }
    public Code generateCode(){
        Vector<AsmInstr> instrs = new Vector<>();
        MemLabel skipToLabel = null; //used to skip all instructions to specific label (for conditional jumps: case if(false) - no need to jump)

        for(ImcStmt s : this.codeChunk.stmts()){ //change statements to instructions
            System.out.println(s);
            if(skipToLabel != null){
                //skip until we have label
                if(s instanceof ImcLABEL && (((ImcLABEL)s).label == skipToLabel)){ //found it! set skipToLabel to null
                    skipToLabel = null;
                }
                continue;
            }
            TileResolver tr = new TileResolver();
            s.accept(tr, instrs);
            if(tr.skipToLabel != null){
                skipToLabel = tr.skipToLabel;
            }
        }

        Code c = new Code(this.codeChunk.frame, this.codeChunk.entryLabel, this.codeChunk.exitLabel, instrs);
        return c;
    }

}

/* TODO */
//finite automate?
class TileResolver implements ImcVisitor<MemTemp, Vector<AsmInstr>>{
    private int state = 0;
    public MemLabel skipToLabel = null;

    public MemTemp visit(ImcBINOP binOp, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcCALL call, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

    //TODO
	public MemTemp visit(ImcCJUMP cjump, Vector<AsmInstr> visArg) {
        Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();
		//lahko je: mem, binop(compare)
        if(cjump.cond instanceof ImcBINOP){
            //get op, get T1, T2
            MemTemp T1 = ((ImcBINOP)cjump.cond).fstExpr.accept(this, visArg);
            MemTemp T2 = ((ImcBINOP)cjump.cond).sndExpr.accept(this, visArg);

            String branchAfterCompare = ""; //must be opposite, as we only jump on false
            switch (((ImcBINOP)cjump.cond).oper) {
                // case  ImcBINOP.Oper.OR: case ImcBINOP.Oper.AND: op="BZ"; compare=false; break; //ce je 1, potem ne skoci, drugace skoci (zero = else)
                case ImcBINOP.Oper.EQU: branchAfterCompare="BNZ"; break;
                case ImcBINOP.Oper.NEQ: branchAfterCompare="BZ"; break;
                case ImcBINOP.Oper.LTH: branchAfterCompare="BNN"; break;
                case ImcBINOP.Oper.GTH: branchAfterCompare="BNP"; break;
                case ImcBINOP.Oper.LEQ: branchAfterCompare="BP"; break;
                case ImcBINOP.Oper.GEQ: branchAfterCompare="BN"; break;
            
                default:
                    break; //break compiler :(
            }
            
            
            System.out.println(" --- We have comparison, get ze temps, do compare, return nothing (null)");
            AsmInstr ai_compare = new AsmOPER("CMP "+T1+", "+T1+", "+T2, uses, defs, jumps); //compare it
            AsmInstr ai_branch = new AsmOPER(branchAfterCompare+" "+cjump.negLabel.name, uses, defs, jumps);
            visArg.add(ai_compare);
            visArg.add(ai_branch);
            this.skipToLabel = cjump.posLabel;


        }
        if(cjump.cond instanceof ImcMEM){
            //got memory access (read from boolean)
            System.out.println(" --- We have boolean expression, do compare, return nothing");
        }
        if(cjump.cond instanceof ImcCONST){
            //do not jump at all, we have constant
            System.out.println(" --- We have constant ("+cjump.cond+"), do compare, return nothing");
            long imcConst = ((ImcCONST)cjump.cond).value;
           
            if(imcConst == 0){ //do not jump, delete code instead
                jumps.add(cjump.negLabel);
                // AsmInstr ai = new AsmOPER("JUMP "+cjump.negLabel.name, uses, defs, jumps);
                // visArg.add(ai);
                this.skipToLabel = cjump.negLabel;
            }
            // jump gone, carry on
        }
        

        //throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcCONST constant, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcESTMT eStmt, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcJUMP jump, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcLABEL label, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        AsmInstr ai = new AsmLABEL(label.label);
        visArg.add(ai);
        return null;
	}

	public MemTemp visit(ImcMEM mem, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcMOVE move, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcNAME name, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcSEXPR sExpr, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcSTMTS stmts, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcTEMP temp, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}

	public MemTemp visit(ImcUNOP unOp, Vector<AsmInstr> visArg) {
		//throw new Report.InternalError();
        return null;
	}
    
}
