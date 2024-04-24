package lang24.phase.imcgen;
import java.util.List;
import java.util.Vector;

import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.defn.AstFunDefn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;
import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.*;
import lang24.data.mem.*;
import lang24.data.type.*;
import lang24.phase.memory.*;
import lang24.phase.seman.SemAn;

public class ImcGenerator implements AstFullVisitor<Object, Object> {
    private List<AstFunDefn> defnStack = new Vector<>(); //stack: v kateri funkciji se trenutno nahajamo (da ni treba preko parametrov)
    //addr: //TODO - finish/fix (might not work for all cases)
    //a2,a3,a4,a5
    //a1 - strings:
    private ImcExpr getAddr(AstExpr expr){
        //ce je to string, potem vrni njegov label
        if(expr instanceof AstAtomExpr){
            if(((AstAtomExpr)expr).type  == AstAtomExpr.Type.STR){ //imamo string, return string label
                MemAccess l = Memory.strings.get((AstAtomExpr)expr);
                return new ImcNAME(((MemAbsAccess)l).label);
            }
        }
        else if(expr instanceof AstNameExpr){ //variable (local, global), function, ...
            //imamo identifier, samo treba je ugotovit, katerega tipa je:
            AstDefn d = SemAn.definedAt.get(expr); //definicijo mamo. Zdj pa:
            //ce je to: vardefn
            if(d instanceof AstVarDefn){
                MemAccess m = Memory.varAccesses.get((AstVarDefn)d);
                //ce je: local (offset), global (label)
                if(m instanceof MemAbsAccess){ //global: vrni label
                    ImcNAME n = new ImcNAME(((MemAbsAccess)m).label);
                    return n;
                }
                else if(m instanceof MemRelAccess){ //local: vrni offset
                    long depth = ((MemRelAccess)m).depth;
                    ImcCONST c = new ImcCONST(((MemRelAccess)m).offset);
                    // System.out.println(defnStack.get((int)depth));
                    AstFunDefn funDefn = defnStack.get((int)depth);
                    if(funDefn != null){
                        MemFrame mf = Memory.frames.get(funDefn);
                        if(mf != null){
                            MemTemp fp = mf.FP;
                            ImcExpr expr_temp = new ImcTEMP(fp);
                            ImcExpr expr_add = new ImcBINOP(ImcBINOP.Oper.ADD, expr_temp, c);
                            return expr_add;
                        }
                    }
                    return c;
                }

            }
            //ce je to: pardefn (parameter)
            else if(d instanceof AstParDefn){
                // System.out.println("parameter access");
                MemAccess m = Memory.parAccesses.get((AstParDefn)d);
                long depth = ((MemRelAccess)m).depth;
                ImcCONST c = new ImcCONST(((MemRelAccess)m).offset);

                AstFunDefn funDefn = defnStack.get((int)depth);
                if(funDefn != null){
                    MemFrame mf = Memory.frames.get(funDefn);
                    if(mf != null){
                        MemTemp fp = mf.FP;
                        ImcExpr expr_temp = new ImcTEMP(fp);
                        ImcExpr expr_add = new ImcBINOP(ImcBINOP.Oper.ADD, expr_temp, c);
                        return expr_add;
                    }
                }
                return c;
            }
            //ce je to cmpdefn:
            //ce je to fundefn:
        }
        else if(expr instanceof AstArrExpr){ //je array
            ImcExpr e1 = this.getAddr(((AstArrExpr)expr).arr); //pridobi array addr
            ImcExpr e2 = ImcGen.exprImc.get(((AstArrExpr)expr).idx); //possible M' (?)
            SemType t = SemAn.ofType.get(expr); //tip, ki ga ima array
            if(t != null){
                long typeSize = MemEvaluator.type2size(t);
                ImcExpr sizeConst = new ImcCONST(typeSize);
                ImcExpr bin_mul = new ImcBINOP(ImcBINOP.Oper.MUL, e2, sizeConst);
                ImcExpr bin_add = new ImcBINOP(ImcBINOP.Oper.ADD, e1, bin_mul);
                return bin_add;
            }

        }
        else if(expr instanceof AstCmpExpr){ //component access:
            ImcExpr baseAddr = this.getAddr(((AstCmpExpr)expr).expr); //base addr
            AstDefn d = SemAn.definedAt.get(expr); //get component definition: za offset
            MemAccess acc = Memory.cmpAccesses.get((AstRecType.AstCmpDefn)d);
            if(d != null && acc != null && baseAddr != null){
                long offset = ((MemRelAccess)acc).offset;
                if(offset == 0){ // no need to add offset, because it is 0 (tiny optimization)
                    return baseAddr;
                }
                else{ // if not 0, add it
                    ImcExpr off = new ImcCONST(offset);
                    ImcExpr add = new ImcBINOP(ImcBINOP.Oper.ADD, baseAddr, off);
                    return add;
                }
            }
        }
        else if(expr instanceof AstCallExpr){ //kaj ce je klic funkcije? vrni ret. value
            // System.out.printf("je function call\n");
            AstFunDefn funDefn = (AstFunDefn)SemAn.definedAt.get(expr); //get function definition
            MemFrame mf = Memory.frames.get(funDefn); //get frame
            if(funDefn != null && mf != null){
                ImcExpr mem_temp = new ImcTEMP(mf.RV); //return value od frame-a; sam kok je pa offset?
                return mem_temp;
            }
        }
        else if(expr instanceof AstSfxExpr){ //return mem of that address
            // System.out.println("imcgen: sfxexpr");
            ImcExpr addr = this.getAddr(((AstSfxExpr)expr).expr);
            ImcExpr mem = new ImcMEM(addr);
            return mem;
        }
        return null;
    }


    //ex6.2 //PREVERI
    @Override
    public Object visit(AstSfxExpr sfxExpr, Object arg){
        sfxExpr.expr.accept(this, arg);
        // System.out.println("abc");
        ImcExpr e1 = ImcGen.exprImc.get(sfxExpr.expr);
        // System.out.printf("sfx: %s\n",e1);
        if(e1 != null){
            //mem access:
            ImcExpr mem = new ImcMEM(e1);
            ImcGen.exprImc.put(sfxExpr, mem);
        }
        return null;
    }


    //expr:
    //helper methods:
    //oper enum translator methods (ast -> imc)
    private ImcBINOP.Oper operTranslator(AstBinExpr.Oper oper){
        switch(oper) {
            case OR : return ImcBINOP.Oper.OR;
            case AND: return ImcBINOP.Oper.AND;
            case EQU: return ImcBINOP.Oper.EQU;
            case NEQ: return ImcBINOP.Oper.NEQ;
            case LTH: return ImcBINOP.Oper.LTH;
            case GTH: return ImcBINOP.Oper.GTH;
            case LEQ: return ImcBINOP.Oper.LEQ;
            case GEQ: return ImcBINOP.Oper.GEQ;
            case ADD: return ImcBINOP.Oper.ADD;
            case SUB: return ImcBINOP.Oper.SUB;
            case MUL: return ImcBINOP.Oper.MUL;
            case DIV: return ImcBINOP.Oper.DIV;
            case MOD: return ImcBINOP.Oper.MOD;
            default: return null; //v ostalih primerih (ki jih ni) vrne null
        }
    }
    
    private char parseChar(String c){
        String value = c.replaceAll("'", "");
        if(value.length() == 1){ //normal char
            return value.charAt(0);
        }
        else if(value.length() == 2){ //escape sequence
            switch(value.charAt(1)){
                case 'n': return '\n';
                case 'r': return '\r';
                case 't': return '\t';
            }
        }
        else{
            int n = Integer.parseInt(value.substring(1), 16); //convert to int in base 16
            // System.out.printf("%s, %s, %d\n",value, value.substring(1), n);
            n = n % 256;
            return (char)n;
        }
        return '\0';
    }
    //---------------
    
    //sizeof:
    @Override
	public Object visit(AstSizeofExpr sizeofExpr, Object arg) {
		// sizeofExpr.type.accept(this, arg);
        SemType t = SemAn.isType.get(sizeofExpr.type);
        Long size = MemEvaluator.type2size(t);
        ImcExpr constExpr = new ImcCONST(size);
        ImcGen.exprImc.put(sizeofExpr, constExpr);
		return null;
	}

    //ex1, ex2, ex3:
    @Override
	public Object visit(AstAtomExpr atomExpr, Object arg) {
		SemType t = SemAn.ofType.get(atomExpr).actualType();
        if(t instanceof SemPointerType){ //pointer type
            if(((SemPointerType)t).baseType instanceof SemVoidType){
                ImcGen.exprImc.put(atomExpr, new ImcCONST(0)); //nil = 0
            }
            if(((SemPointerType)t).baseType instanceof SemCharType){ //to je string
                MemAbsAccess mem_str = Memory.strings.get(atomExpr);
                if(mem_str != null){
                    ImcGen.exprImc.put(atomExpr, new ImcMEM(new ImcNAME(mem_str.label)));
                }
            }
        }
        if(t instanceof SemVoidType){ //none (void)
            //none: leave undefined
        }
        if(t instanceof SemBoolType){ //boolean
            if(atomExpr.value.equals("true")) ImcGen.exprImc.put(atomExpr, new ImcCONST(1)); //true = 1
            else ImcGen.exprImc.put(atomExpr, new ImcCONST(0)); //false = 0
        }
        if(t instanceof SemCharType){
            String value = atomExpr.value;
            char c = this.parseChar(value);
            // System.out.println(c);
            // System.out.printf("%s, %s\n", value ,this.parseChar(value));
            ImcGen.exprImc.put(atomExpr, new ImcCONST(c));
        }
        if(t instanceof SemIntType){
            Long n = Long.parseLong(atomExpr.value);
            ImcGen.exprImc.put(atomExpr, new ImcCONST(n));
        }
        return null;
	}


    //ex4 - binop
    @Override
	public Object visit(AstBinExpr binExpr, Object arg) {
        // System.out.printf("binexpr: fst: %s, snd: %s\n", binExpr.fstExpr, binExpr.sndExpr);
        binExpr.fstExpr.accept(this, arg);
		binExpr.sndExpr.accept(this, arg);

        //kle lahko dobim:
        //fstImc, sndImc
        //oper mam ze

        ImcExpr e1 = ImcGen.exprImc.get(binExpr.fstExpr); //possible M'
        ImcExpr e2 = ImcGen.exprImc.get(binExpr.sndExpr); //possible M''
        // System.out.printf("binop: e1: %s, e2: %s\n", e1, e2);
        if(e1 != null && e2 != null){
            // System.out.println(e1);
            ImcExpr b = new ImcBINOP(this.operTranslator(binExpr.oper), e1, e2);
            ImcGen.exprImc.put(binExpr, b); //it is M''
        }
		return null;
	}

    //ex5 - unop
    @Override
    public Object visit(AstPfxExpr pfxExpr, Object arg){
        pfxExpr.expr.accept(this, arg);
        if(pfxExpr.oper == AstPfxExpr.Oper.PTR){ //ex6.1
            ImcExpr ptrAddr = this.getAddr(pfxExpr.expr);
            if(ptrAddr != null){
                ImcGen.exprImc.put(pfxExpr, ptrAddr);
            }
        }
        else{
            ImcExpr e = ImcGen.exprImc.get(pfxExpr.expr);
            // System.out.printf("%s, %s\n", pfxExpr, e);
            if(e != null){
                if(pfxExpr.oper == AstPfxExpr.Oper.ADD){ //leave out, does not change the value
                    ImcGen.exprImc.put(pfxExpr, e); //passthrough
                }
                else if(pfxExpr.oper == AstPfxExpr.Oper.SUB){
                    ImcExpr neg_e = new ImcUNOP(ImcUNOP.Oper.NEG, e);
                    ImcGen.exprImc.put(pfxExpr, neg_e);

                }
                else if(pfxExpr.oper == AstPfxExpr.Oper.NOT){
                    ImcExpr not_e = new ImcUNOP(ImcUNOP.Oper.NOT, e);
                    ImcGen.exprImc.put(pfxExpr, not_e);
                }
            }
        }
        return null;
    } 
    
    
    //ex8
    @Override
	public Object visit(AstArrExpr arrExpr, Object arg) {
        arrExpr.arr.accept(this, arg);
		arrExpr.idx.accept(this, arg);
        
        
        
        ImcExpr e = this.getAddr(arrExpr); //pridobi address expression-a
        // ImcExpr e = this.getAddr(arrExpr.arr); //pridobi address expression-a
        // System.out.printf("arrexpr: fst: %s, snd: %s, imcexpr: %s\n", arrExpr.arr, arrExpr.idx, e);
        
        // System.out.printf("%s, %s\n", arrExpr, e);
        if(e != null){
            ImcExpr mem = new ImcMEM(e);
            ImcGen.exprImc.put(arrExpr, mem);
        }

		return null;
	}
    

    //ex11: oklepaji reseni ze v seman-u (?)
    //ex12, ex13 (typecast)
    @Override
	public Object visit(AstCastExpr castExpr, Object arg) {
		// castExpr.type.accept(this, arg); //kle se s tipi ne ukvarjamo, samo s kodo
		castExpr.expr.accept(this, arg);

        SemType t = SemAn.isType.get(castExpr.type).actualType();
        ImcExpr e = ImcGen.exprImc.get(castExpr.expr);
        if(e != null){
            if(t instanceof SemCharType){ //ex12
                //subtree: e <-- binop(mod) --> const(256)
                ImcExpr const256 = new ImcCONST(256l);
                ImcExpr mod_op = new ImcBINOP(ImcBINOP.Oper.MOD, e, const256);
                ImcGen.exprImc.put(castExpr, mod_op);
            }
            else{ //ex13
                ImcGen.exprImc.put(castExpr, e);
            }
        }

		return null;
	}

    //ex7:
    @Override
	public Object visit(AstNameExpr nameExpr, Object arg) {
		//get possible outer definition:
        if(arg != null && (arg instanceof AstDefn)){
            // AstDefn d = (AstDefn)arg;
            ImcExpr e = this.getAddr(nameExpr); //get identifier address
            if(e != null){
                if(e instanceof ImcCONST){ //relative access
                    //actions depend on definition type:
                    // if(d instanceof AstFunDefn){
                    //     //get expr relative to fp
                    //     MemFrame f = Memory.frames.get((AstFunDefn)d);
                    //     ImcExpr temp = new ImcTEMP(f.FP);
                    //     ImcExpr bin = new ImcBINOP(ImcBINOP.Oper.ADD, temp, e);
                    //     ImcExpr mem = new ImcMEM(bin);
                    //     ImcGen.exprImc.put(nameExpr, mem);
                    // }
                    ImcExpr mem = new ImcMEM(e);
                    ImcGen.exprImc.put(nameExpr, mem);
                }
                else{
                    //absolute access: return label:
                    ImcExpr mem = new ImcMEM(e);
                    ImcGen.exprImc.put(nameExpr, mem);
                }
            }
        }
        return null;
	}

    //ex9 - component access
    @Override
	public Object visit(AstCmpExpr cmpExpr, Object arg) {
		cmpExpr.expr.accept(this, arg);

        //cmpExpr = baseAddr + offset
        ImcExpr baseAddr = this.getAddr(cmpExpr.expr);
        AstDefn d = SemAn.definedAt.get(cmpExpr); //get component definition: za offset
        MemAccess acc = Memory.cmpAccesses.get((AstRecType.AstCmpDefn)d);
        if(d != null && acc != null && baseAddr != null){
            long offset = ((MemRelAccess)acc).offset;
            ImcExpr mem;
            if(offset == 0){ //no need for extra instruction, skip add (x + 0 = x)
                mem = new ImcMEM(baseAddr);
                ImcGen.exprImc.put(cmpExpr, mem);
            }
            else{
                ImcExpr off = new ImcCONST(offset);
                ImcExpr add = new ImcBINOP(ImcBINOP.Oper.ADD, baseAddr, off);
                mem = new ImcMEM(add);
                ImcGen.exprImc.put(cmpExpr, mem);
            }
        }
		return null;
	}
    
    //helper function for AstCallExpr visit: get static link
    private ImcExpr getSL(AstFunDefn current, AstFunDefn calling){
        MemFrame mf1 = Memory.frames.get(current);
        MemFrame mf2 = Memory.frames.get(calling);
        
        long nMem = (mf1.depth - mf2.depth) +1; //depth difference +1 (if we call inner function, then SL is equal to FP of the caller function)
        ImcExpr SL = new ImcTEMP(mf1.FP);
        for(long i = 0; i < nMem; i++){
            SL = new ImcMEM(SL);
        }
        return SL;
    }
    
    //ex10 - function call
    @Override
	public Object visit(AstCallExpr callExpr, Object arg) {
        AstDefn fdef = SemAn.definedAt.get(callExpr); //get function definition (for frame)
        MemFrame frame = Memory.frames.get((AstFunDefn)fdef);

        MemLabel flabel = frame.label;
        Vector<Long> offsets = new Vector<>();
        Vector<ImcExpr> arguments = new Vector<>();

        
        //calculate static link:
        // ImcExpr sl_expr = new ImcCONST(0l);
        ImcExpr sl_expr = getSL(defnStack.getLast(), (AstFunDefn)fdef);

        //add static link to arguments
        arguments.add(sl_expr);
        offsets.add(0l);
        
        if (callExpr.args != null){
            // callExpr.args.accept(this, arg);
            for(AstExpr e : callExpr.args){
                e.accept(this, arg);
                // get expression, add it to arguments
                ImcExpr arg_e = ImcGen.exprImc.get(e);
                if(arg_e != null){
                    arguments.add(arg_e);
                    offsets.add(0l); //kok je offset?
                }
                
            }
        }
        
        //? a nj vrnem tudi mem?
        ImcExpr e_call = new ImcCALL(flabel, offsets, arguments);
        ImcGen.exprImc.put(callExpr, e_call);
        // System.out.printf("callexpr: %s\n",e_call);
		return null;
	}


    //function definition: pass function definition through parameters
    @Override
	public Object visit(AstFunDefn funDefn, Object arg) {
        arg = funDefn;//Memory.frames.get(funDefn); //tree now has current function's definition (for frame)
        defnStack.add(funDefn); //add funtion to stack for addr resolver function

        //add entry/exit label
        MemLabel fn_entry = new MemLabel();
        MemLabel fn_exit = new MemLabel();
        ImcGen.entryLabel.put(funDefn, fn_entry);
        ImcGen.exitLabel.put(funDefn, fn_exit);


		if (funDefn.pars != null)
			funDefn.pars.accept(this, arg);
		if (funDefn.stmt != null)
            funDefn.stmt.accept(this, arg);
		if (funDefn.defns != null)
			funDefn.defns.accept(this, arg);
		funDefn.type.accept(this, arg);
        defnStack.remove(funDefn); //remove from stack
		return null;
	}

    //statements:

    //st1 - expr. statement
    @Override
	public Object visit(AstExprStmt exprStmt, Object arg) {
		exprStmt.expr.accept(this, arg);

        ImcExpr e = ImcGen.exprImc.get(exprStmt.expr);
        if(e != null){
            ImcStmt estmt = new ImcESTMT(e);
            ImcGen.stmtImc.put(exprStmt, estmt);
        }

		return null;
	}

    
    //st2 - assign statement
    @Override
	public Object visit(AstAssignStmt assignStmt, Object arg) {
		assignStmt.dst.accept(this, arg);
		assignStmt.src.accept(this, arg);

        // ImcExpr e_dst = ImcGen.exprImc.get(assignStmt.dst);
        ImcExpr e_dst = this.getAddr(assignStmt.dst); //mogoce bi bilo fino to narest tkole (ampak ne bo slo kar tako, treba je popravit relative access) //TODO
        ImcExpr e_src = ImcGen.exprImc.get(assignStmt.src);
        if(e_dst != null && e_src != null){
            ImcExpr e_mem = new ImcMEM(e_dst); //save!
            ImcStmt s_assign = new ImcMOVE(e_mem, e_src);
            // ImcStmt s_assign = new ImcMOVE(e_dst, e_src);
            ImcGen.stmtImc.put(assignStmt, s_assign);
        }
		return null;
	}



    //st3,st4 - if then
    //st5,st6 - if then else

    @Override
	public Object visit(AstIfStmt ifStmt, Object arg) {
        Vector<ImcStmt> stmts = new Vector<>(); //vector statement-ov

		ifStmt.cond.accept(this, arg);
		ifStmt.thenStmt.accept(this, arg);

        //if -> condition
        //label(true) -> label true
        // stmt1
        //label(false) -> label false
        // ? stmt2
        
        ImcExpr condition = ImcGen.exprImc.get(ifStmt.cond); //get condition
        if(condition == null) return null;
        MemLabel l1 = new MemLabel();
        MemLabel l2 = new MemLabel();
        ImcStmt jump = new ImcCJUMP(condition, l1, l2);
        
        ImcStmt l1stmt = new ImcLABEL(l1);
        ImcStmt stmt1 = ImcGen.stmtImc.get(ifStmt.thenStmt);
        //manjka jump!
        ImcStmt l2stmt = new ImcLABEL(l2);
        ImcStmt stmt2 = null;


		if (ifStmt.elseStmt != null){
			ifStmt.elseStmt.accept(this, arg);
            stmt2 = ImcGen.stmtImc.get(ifStmt.elseStmt);
        }

        if(jump != null && stmt1 != null){
            stmts.add(jump);
            stmts.add(l1stmt);
            stmts.add(stmt1);
            if(stmt2 != null){ //imamo else, treba dodati se, da ga if preskoci
                MemLabel after = new MemLabel(); //so that if can jump over else part
                ImcStmt lafter = new ImcLABEL(after);
                ImcStmt jump_over_else = new ImcJUMP(after);
                stmts.add(jump_over_else);
                stmts.add(l2stmt);
                stmts.add(stmt2);
                stmts.add(lafter);
            }
            else{
                stmts.add(l2stmt);
            }
            ImcStmt stmt_if = new ImcSTMTS(stmts);
            ImcGen.stmtImc.put(ifStmt, stmt_if);
        }
		return null;
	}


    
    //st7,st8 - while
    @Override
	public Object visit(AstWhileStmt whileStmt, Object arg) {
        Vector<ImcStmt> stmts = new Vector<>(); //vector statement-ov
		
        whileStmt.cond.accept(this, arg);
		whileStmt.stmt.accept(this, arg);
        
        
        MemLabel l0 = new MemLabel();
        ImcStmt l0stmt = new ImcLABEL(l0);
        ImcExpr condition = ImcGen.exprImc.get(whileStmt.cond); //get condition
        if(condition == null) return null;
        
        MemLabel l1 = new MemLabel();
        MemLabel l2 = new MemLabel();
        
        ImcStmt cjump = new ImcCJUMP(condition, l1, l2);
        ImcStmt l1stmt = new ImcLABEL(l1);
        
        ImcStmt stmt = ImcGen.stmtImc.get(whileStmt.stmt);
        ImcStmt l2stmt = new ImcLABEL(l2);
        ImcStmt jump0 = new ImcJUMP(l0); //jump to start

        if(stmt != null){
            stmts.add(l0stmt);
            stmts.add(cjump);
            stmts.add(l1stmt);
            stmts.add(stmt);
            stmts.add(jump0);
            stmts.add(l2stmt);
            ImcStmt stmt_while = new ImcSTMTS(stmts);
            ImcGen.stmtImc.put(whileStmt, stmt_while);
        }
		return null;
	}

    //st9 - block
    @Override
	public Object visit(AstBlockStmt blockStmt, Object arg) {
		Vector<ImcStmt> stmts = new Vector<>(); //vector statement-ov
        
        if (blockStmt.stmts != null){
            for(AstStmt s : blockStmt.stmts){
                s.accept(this, arg);

                ImcStmt stmt = ImcGen.stmtImc.get(s);
                if(stmt != null){
                    stmts.add(stmt);
                }
            }
            ImcStmt stmt_block = new ImcSTMTS(stmts);
            ImcGen.stmtImc.put(blockStmt, stmt_block);
        }
		return null;
	}

    //st10 - return
    @Override
	public Object visit(AstReturnStmt retStmt, Object arg) {
		retStmt.expr.accept(this, arg);
        Vector<ImcStmt> stmts = new Vector<>(); //vector statement-ov
		ImcExpr expr_ret = ImcGen.exprImc.get(retStmt.expr);
        
        if(SemAn.ofType.get(retStmt.expr) == SemVoidType.type){
            //System.out.println("je void");
            expr_ret = new ImcCONST(0l); //ce je void, vseeno vrni 0
        }
        //move + jump
        if(expr_ret != null && arg != null && (arg instanceof AstFunDefn)){
            MemFrame funcFrame = Memory.frames.get((AstFunDefn)arg);
            // ImcTEMP t = new ImcTEMP(funcFrame.RV);
            // ImcExpr expr_dst = new ImcMEM(new ImcTEMP(funcFrame.RV));
            ImcExpr expr_dst = new ImcTEMP(funcFrame.RV);
            ImcStmt stmt_move = new ImcMOVE(expr_dst, expr_ret);
            MemLabel l_exit = new MemLabel();
            if(arg != null && (arg instanceof AstFunDefn)){
                l_exit = ImcGen.exitLabel.get((AstFunDefn)arg); //dobimo exit label iz imcgen-a
            }
            ImcStmt stmt_jump = new ImcJUMP(l_exit); 
            stmts.add(stmt_move);
            stmts.add(stmt_jump);
            ImcStmt stmt_ret = new ImcSTMTS(stmts);
            ImcGen.stmtImc.put(retStmt, stmt_ret);
        }
        return null;
	}

}
