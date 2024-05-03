package lang24.phase.asmgen;

import java.util.Vector;

import lang24.common.report.Report;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
import lang24.data.asm.AsmMOVE;
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
import lang24.phase.imcgen.ImcGen;

public class AsmGenerator {
    private final LinCodeChunk codeChunk;
    public AsmGenerator(LinCodeChunk codeChunk){
        this.codeChunk = codeChunk;
    }
    public Code generateCode(){
        Vector<AsmInstr> instrs = new Vector<>();
        MemLabel skipToLabel = null; //used to skip all instructions to specific label (for conditional jumps: case if(false) - no need to jump)

        for(ImcStmt s : this.codeChunk.stmts()){ //change statements to instructions
            // System.out.println(s);
            if(skipToLabel != null){
                //skip until we have label
                if(s instanceof ImcLABEL && (((ImcLABEL)s).label == skipToLabel)){ //found it! set skipToLabel to null
                    skipToLabel = null;
                }
                continue;
            }
            TileResolver tr = new TileResolver(this.codeChunk);
            s.accept(tr, instrs);
            if(tr.skipToLabel != null){
                skipToLabel = tr.skipToLabel;
            }
        }

        Code c = new Code(this.codeChunk.frame, this.codeChunk.entryLabel, this.codeChunk.exitLabel, instrs);
        return c;
    }

}

class TileResolver implements ImcVisitor<MemTemp, Vector<AsmInstr>>{
    private final LinCodeChunk codeChunk; //we need that to get function info (for jump: to detect return value)
    public MemLabel skipToLabel = null;
    public MemLabel skipFromLabel = null;
    public static MemTemp callRetVal = new MemTemp(); //register to access return value of calling function

    public TileResolver(LinCodeChunk codeChunk){
        this.codeChunk=codeChunk;
    }

	public MemTemp visit(ImcCJUMP cjump, Vector<AsmInstr> visArg) {
        Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();
		//lahko je: mem, binop(compare)
        if(cjump.cond instanceof ImcBINOP){
            //get op, get T1, T2
            MemTemp T1 = ((ImcBINOP)cjump.cond).fstExpr.accept(this, visArg);
            MemTemp T2 = ((ImcBINOP)cjump.cond).sndExpr.accept(this, visArg);
            uses.add(T1);
            uses.add(T2);

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
            // System.out.printf("je binop: %s, %s, \n", T1, T2);
            
            // System.out.println(" --- We have comparison, get ze temps, do compare, return nothing (null)");
            AsmInstr ai_compare = new AsmOPER("CMP "+T1+","+T1+","+T2, uses, defs, jumps); //compare it
            AsmInstr ai_branch = new AsmOPER(branchAfterCompare+" "+T1+","+cjump.negLabel.name, uses, defs, jumps);
            visArg.add(ai_compare);
            visArg.add(ai_branch);
            this.skipToLabel = cjump.posLabel;
        }
        else if(cjump.cond instanceof ImcCONST){
            //do not jump at all, we have constant (const skip)
            // System.out.println(" --- We have constant ("+cjump.cond+"), do compare, return nothing");
            long imcConst = ((ImcCONST)cjump.cond).value;
           
            if(imcConst == 0){ //do not jump, delete code instead (skipToLabel: skip if)
                // System.out.println("je konstanta, 0");
                jumps.add(cjump.negLabel);
                AsmInstr ai = new AsmOPER("JMP "+cjump.negLabel.name, uses, defs, jumps);
                visArg.add(ai);
                this.skipToLabel = cjump.negLabel;
            }
            else{ //skipFromToLabel: delete (skip) else
                // System.out.println("je konstanta, value: " + imcConst); //TODO
            }
            // jump gone, carry on
        }
        else{
            MemTemp T1 = cjump.cond.accept(this, visArg);
            if(T1 != null){
                uses.add(T1);
                AsmInstr ai_branch = new AsmOPER("BZ "+T1+","+cjump.negLabel.name, uses, defs, jumps); //zero = false, everything else = true
                visArg.add(ai_branch);
                this.skipToLabel = cjump.posLabel; //delete unnecessary label
            }
        }

        return null;
        // if(cjump.cond instanceof ImcMEM){
        //     //got memory access (read from boolean)
        //     // System.out.println(" --- We have boolean expression, do compare, return nothing");
        // }
	}

    //ce visitam to (ImcCONST), potem nalozi konstanto v register, vrni register (temp)
	public MemTemp visit(ImcCONST constant, Vector<AsmInstr> visArg) {
		Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();
        // AsmInstr
        // what is this const?
        long const_val = constant.value;

        long constL = const_val%(1l<<16);
        MemTemp T1 = new MemTemp(); //new register to store value
        // System.out.printf("SETL [%s]\n", constL);
        AsmInstr setL = new AsmOPER("SETL "+T1+","+constL, uses, defs, jumps);
        visArg.add(setL);
        // shift, incML, repeate if needed
        
        for(int n_bits = 16; n_bits < 64; n_bits += 16){
            if(const_val > (1l<<n_bits)){
                long constInc = const_val >>> n_bits;
                constInc = constInc % (1l << 16);
                String op_code = "";
                switch(n_bits){
                    case 16: op_code = "INCML"; break;
                    case 32: op_code = "INCMH"; break;
                    case 48: op_code = "INCH"; break;
                }
                // System.out.printf("%s (%s-%s) [0x%08x]\n",op_code, n_bits, n_bits+16, constInc);
                // System.out.printf("%s [0x%08x]\n",op_code, constInc);
                AsmInstr incML = new AsmOPER(op_code+" "+T1+","+constInc, uses, defs, jumps);
                visArg.add(incML);
            }
        }
        
        return T1;
        // return null;
	}

	

	public MemTemp visit(ImcLABEL label, Vector<AsmInstr> visArg) {
		
        AsmInstr ai = new AsmLABEL(label.label);
        visArg.add(ai);
        return null;
	}

    //helper method to get opCode from binop operator
    private String opCodeFromBINOP(ImcBINOP.Oper oper){
        String op;
        switch (oper) {
            case ImcBINOP.Oper.OR: op="OR"; break;
            case ImcBINOP.Oper.AND: op="AND"; break;
            
            case ImcBINOP.Oper.ADD: op="ADD"; break;
            case ImcBINOP.Oper.SUB: op="SUB"; break;
            case ImcBINOP.Oper.MUL: op="MUL"; break;
            case ImcBINOP.Oper.DIV: op="DIV"; break;
            case ImcBINOP.Oper.MOD: op="MOD"; break;

            default: op = ""; break; //break compiler :(
        }
        return op;
    }

    //TODO: FIX
    //memory access: absolute, relative (relative: has binop) 
	public MemTemp visit(ImcMEM mem, Vector<AsmInstr> visArg) {
		Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();
        MemTemp T1 = new MemTemp(); //can we avoid this new label? maybe fix frame pointer (SL access) (fix imcgen)
        if(mem.addr instanceof ImcBINOP){ //we have relative access, load from memory:
            // String opCode = opCodeFromBINOP(((ImcBINOP)mem.addr).oper);
            MemTemp base = ((ImcBINOP)mem.addr).fstExpr.accept(this, visArg);
            if(((ImcBINOP)mem.addr).sndExpr instanceof ImcCONST){ //imamo relative access, drugace imamo pa array, treba je visitat sndExpr
                Long offset = ((ImcCONST) ((ImcBINOP)mem.addr).sndExpr).value;
                
                defs.add(base);
                defs.add(T1);
                // System.out.printf("%s, %s\n", T1, base);
                // AsmInstr load_relative = new AsmOPER(opCode+" "+T1.temp+","+base.temp+","+offset, uses, defs, jumps);
                AsmInstr load_relative = new AsmOPER("LDO "+T1+","+base+","+offset, uses, defs, jumps);
                visArg.add(load_relative);
            }
            else{
                MemTemp T2 = mem.addr.accept(this, visArg); //imamo drugi izraz
                //add to uses, defs: TODO
                AsmInstr load_relative = new AsmOPER("LDO "+T1+","+base+","+T2, uses, defs, jumps);
                visArg.add(load_relative);
            }
        }
        if(mem.addr instanceof ImcNAME){
            // System.out.printf("imcmem: addr je: %s\n",mem.addr);
            //load from label:
            MemLabel l = ((ImcNAME)(mem.addr)).label;
            AsmInstr load_abs = new AsmOPER("LDO "+T1+","+l.name, uses, defs, jumps);
            visArg.add(load_abs);
        }

        return T1;
	}

    //binop, unop:
    public MemTemp visit(ImcBINOP binOp, Vector<AsmInstr> visArg) { //ce pridemo do sem, izvedi operacijo: visit expr1, expr2, op, return
        Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();
        
        String opCode = opCodeFromBINOP(binOp.oper);
        // if(binOp.sndExpr instanceof ImcCALL) System.out.println("binop: sndexpr: je call");;
        MemTemp T1 = binOp.fstExpr.accept(this, visArg);
        MemTemp T2 = binOp.sndExpr.accept(this, visArg);
        
            
        uses.add(T1);
        uses.add(T2);
        // System.out.printf("--> %s\n%s, %s (e1: %s, e2: %s)<--\n", binOp, T1, T2, binOp.fstExpr, binOp.sndExpr);
        AsmInstr binop_asm = new AsmOPER(opCode+" "+T1+","+T1+","+T2, uses, defs, jumps);
        visArg.add(binop_asm);
        
        //get T1, T2, store to T1, return T1
        // String opCode = opCodeFromBINOP(((ImcBINOP)mem.addr).oper);
        return T1;
	}

    public MemTemp visit(ImcUNOP unOp, Vector<AsmInstr> visArg) {
		Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();
        MemTemp T1 = unOp.subExpr.accept(this, visArg);
        uses.add(T1);
        
        AsmInstr unop_instr;
        switch(unOp.oper){
            case ImcUNOP.Oper.NOT: unop_instr = new AsmOPER("NOR "+T1+","+T1+","+T1, uses, defs, jumps); break; //one's complement
            case ImcUNOP.Oper.NEG: unop_instr = new AsmOPER("NEGU "+T1+",0,"+T1, uses, defs, jumps); break; //two's complement
            default: return T1;
        }
        visArg.add(unop_instr);
        // System.out.println(" ---------------------------------- imamo unop: "+unop_instr);
        return T1;
	}


    //move:
	public MemTemp visit(ImcMOVE move, Vector<AsmInstr> visArg) {
        //get t1, t2, move t2 to t1

        Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();
        
        MemTemp T1 = move.dst.accept(this, visArg); //imamo lokacijo, shrani t2 na lokacijo, kamor kaze t1
        MemTemp T2 = move.src.accept(this, visArg);
        // System.out.printf("%s, %s, src: %s\n", T1, T2, move.src);
        uses.add(T1);
        uses.add(T2);

        AsmInstr move_asm;
        if(move.dst instanceof ImcMEM){ //store
            move_asm = new AsmOPER("STO "+T1+","+T2+",0", uses, defs, jumps);
            visArg.add(move_asm);
        }
        if(move.dst instanceof ImcTEMP){ //save to register
            move_asm = new AsmOPER("SET "+T1+","+T2, uses, defs, jumps); //AsmMOVE?
            visArg.add(move_asm);
        }
        return null; //nothing to return
	}


    //temp, name:
	public MemTemp visit(ImcNAME name, Vector<AsmInstr> visArg) {
		
        Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();
        
        MemTemp T1 = new MemTemp();
        defs.add(T1);
        //load address to register using LDA:
        AsmInstr load_abs_addr = new AsmOPER("LDA "+T1+","+name.label.name, uses,defs,jumps);
        visArg.add(load_abs_addr);
        return T1;
	}

    public MemTemp visit(ImcTEMP temp, Vector<AsmInstr> visArg) {
        return temp.temp;
    }

    //jump:
    public MemTemp visit(ImcJUMP jump, Vector<AsmInstr> visArg) {
        Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();
    
        MemLabel jumpLabel = jump.label;
        if(jumpLabel == this.codeChunk.exitLabel){ //it is return jump, use POP
            // System.out.println("jump outta function to label: "+jump.label.name);
            AsmInstr pop_instr = new AsmOPER("POP X, YZ", uses, defs, jumps);
            visArg.add(pop_instr);
        }
        else{ //regular jump (just jump to label)
            AsmInstr jump_instr = new AsmOPER("JMP "+jump.label.name, uses, defs, jumps);
            visArg.add(jump_instr);
        }
        return null;
    }

    // call:
	public MemTemp visit(ImcCALL call, Vector<AsmInstr> visArg) {
        // System.out.println("klic funkcije");
        Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();


        AsmInstr push_instr = new AsmOPER("PUSHJ $X,"+this.codeChunk.entryLabel.name, uses, defs, jumps); //TODO
        visArg.add(push_instr);
        
        return callRetVal;
	}
    
    // estmt: (we ignore it, because it is only used in void function calls (return value is discarded, we do not need it))
    public MemTemp visit(ImcESTMT eStmt, Vector<AsmInstr> visArg) {
        return null;
    }
}
