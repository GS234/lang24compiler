package lang24.phase.memory;

import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstCallExpr;
import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.type.AstRecType;
import lang24.data.ast.tree.type.AstStrType;
import lang24.data.ast.tree.type.AstUniType;
import lang24.data.ast.visitor.*;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.*;
import lang24.data.type.*;
import lang24.phase.imclin.ImcLin;
import lang24.phase.seman.SemAn;

/**
 * Computing memory layout: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemEvaluator implements AstFullVisitor<Object, Object> {
    public static final long ptrSize = 8l;
    public static final long intSize = 8l;
    public static final long boolSize = 8l; //1l
    public static final long charSize = 8l; //1l

    public static final long alignN = 8l;


    public static int depth = 0; //static depth: najprej smo na 0, v funkcijah pa dodajamo oz. odvzemamo
    public static int localLevel = 0; //control localness: 0-global, >0: local

    public static long align_n(long input,long n){
        long pad = (n - input%n)%n;
        input = input+pad;
        return input;
    }

    // LINEARIZATION PHASE
    private void LinAddDataChunk(MemAbsAccess a){
        LinDataChunk dataChunk = new LinDataChunk(a);
        ImcLin.addDataChunk(dataChunk);
    }
    // ------------------


    //funkcije: dodaj klicni zapis
    @Override
	public Object visit(AstFunDefn funDefn, Object arg) {
        // System.out.println("FunDefn: " + funDefn.name);
		// Long paramOffset = 0l;
        Long defnOffset = 0l;
        Long frameSize = 0l; //local defns + [function call frames + params] +  old fp + ret val
        Long SLSize = 8l; // static link
        long argSize = 0l; //velikost argumentov (maksimalna velikost argumentov klicanih funkcij)
        Long rTypeSize = 0l; //velikost return tipa

        if (funDefn.pars != null){
            Long paramOffset = 0l; //za trenutno funkcijo (za offset parametrov)
            // AstFunDefn.AstRefParDefn
            for(AstFunDefn.AstParDefn param : funDefn.pars){
                //which type?
                Long size = (Long)param.accept(this, paramOffset);
                
                //dodajamo +offset:
                paramOffset = paramOffset + size; //align? treba je!
                paramOffset = align_n(paramOffset, MemEvaluator.alignN);
            }
        }


        if (funDefn.defns != null){
		    // funDefn.defns.accept(this, arg);
            MemEvaluator.localLevel++; //povecaj local level
            for(AstDefn defn : funDefn.defns){
                // System.out.println("---defn inside fun: " + defn.name);
                //which type?

                //ce funkcija, povecaj depth:
                if(defn instanceof AstFunDefn){
                    MemEvaluator.depth++;
                    defn.accept(this, arg);
                    --MemEvaluator.depth;
                }
                else if(defn instanceof AstVarDefn){
                    Long size = (Long)defn.accept(this, defnOffset);
                    // System.out.println(defn.name + ", " + size);
                    //dodajamo +offset:
                    defnOffset = defnOffset + size; //align?
                    defnOffset = align_n(defnOffset, alignN);
                }
            }
            --MemEvaluator.localLevel; //zmanjsaj            
        }
        
        MemLabel ml;
        // if it has body (code)
		if (funDefn.stmt != null){
            ml = (MemEvaluator.localLevel == 0)? new MemLabel(funDefn.name): new MemLabel();
            //poisci call expression-e; vzemi max od vseh
            

            BSize size = new BSize(); //poslij po drevesu, spreminja ga samo callExpr:
			// funDefn.stmt.accept(this, arg);
            funDefn.stmt.accept(this, size);
            // System.out.println("velikost: " +  size.getSize());
            argSize = size.getSize();
        }
        else{ //function has external linkage, give it global label
            ml = new MemLabel(funDefn.name);
        }
		
		funDefn.type.accept(this, arg); //a rabmo it po tipu? DA!
        rTypeSize = type2size(SemAn.isType.get(funDefn.type));
        rTypeSize = align_n(rTypeSize, alignN); //align it!
        
        
        // frameSize = //max: rTypeSize, (SLSize + argSize)
        Long blockSize = SLSize+argSize;
        frameSize = defnOffset + ((blockSize > rTypeSize)?blockSize:rTypeSize);
        
        
        MemFrame mf = new MemFrame(
            ml,
            MemEvaluator.depth,
            defnOffset,
            blockSize,
            frameSize
        );
        Memory.frames.put(funDefn, mf);
		
        return frameSize;
	}

    //call expr:
    @Override
	public Object visit(AstCallExpr callExpr, Object arg) {
        // callExpr.args.accept(this, arg);
        // System.out.println(arg + ", " + arg.getClass().getName());
        Long argSize = 0l;
		if (callExpr.args != null){
            
            for(AstExpr argument : callExpr.args){
                Long size = 0l;
                SemType argType = SemAn.ofType.get(argument);
                if(argType != null) size = type2size(argType);
                //dodajamo +offset:
                argSize = argSize + size; //align? treba je!
                argSize = align_n(argSize, alignN);
            }
        }
        // System.out.println(callExpr.name + ": " + argSize);
        if(arg instanceof BSize){
            if(argSize > ((BSize) arg).getSize()){
                ((BSize)arg).setSize(argSize); //nastavi size samo ce je vecji, kot je ze prejsni
            }
        }
		return null;
	}


    //defns:
    // @Override
	public Object visit(AstTypDefn typDefn, Object arg) {
		// System.out.println(typDefn.name);
        typDefn.type.accept(this, arg);
		return null;
	}

	@Override
	public Object visit(AstVarDefn varDefn, Object arg) {
		varDefn.type.accept(this, arg);
        SemType t = SemAn.ofType.get(varDefn);
        Long i = type2size(t);

        // System.out.println(i);
        
        //global level: this is MemAbsAccess
        if(MemEvaluator.localLevel == 0){
            MemAbsAccess a = new MemAbsAccess(i, new MemLabel(varDefn.name));
            this.LinAddDataChunk(a); //LINEARIZATION PHASE
            Memory.varAccesses.put(varDefn,a);
        }
        else{ //is relative access
            //offset dobi iz arg
            Long offset = 0l;
            if(arg != null) offset = (Long)arg;
            offset = offset+i;
            //align 8?
            offset = align_n(offset, alignN);
            //--
            MemRelAccess a = new MemRelAccess(i, -offset, MemEvaluator.depth);
            Memory.varAccesses.put(varDefn,a);
        }
		return i;
	}

	@Override
	public Object visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
		// refParDefn.type.accept(this, arg);
        SemType t = SemAn.ofType.get(refParDefn);
        Long i = type2size(t);

        Long offset = 0l;
        if(arg != null) offset = (Long)arg;
        offset = offset+i;
        //parametri so padded!
        offset = align_n(offset, alignN);
        
        MemRelAccess a = new MemRelAccess(i, offset, MemEvaluator.depth);
        Memory.parAccesses.put(refParDefn,a);
		return 8l; //ima referenco, je pointer, je 8bajtov
	}

	@Override
	public Object visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
		// valParDefn.type.accept(this, arg);
        SemType t = SemAn.ofType.get(valParDefn);
        Long i = type2size(t);
        Long offset = 0l;
        if(arg != null) offset = (Long)arg;
        offset = offset+i;

        //parametri so padded!
        offset = align_n(offset, alignN);

        MemRelAccess a = new MemRelAccess(i, offset, MemEvaluator.depth);
        Memory.parAccesses.put(valParDefn,a);
		return i;
	}
    
    //struct, union (iterate over defns, ):
    @Override
	public Object visit(AstStrType strType, Object arg) {
		// strType.cmps.accept(this, arg);
        Long offset = 0l;
        for(AstRecType.AstCmpDefn cmp : strType.cmps){
            Long size = (Long)cmp.accept(this, offset);
            offset = offset + size; //align? treba je!
            offset = align_n(offset, alignN);
        }
		return offset;
	}

	@Override
	public Object visit(AstUniType uniType, Object arg) {
		// uniType.cmps.accept(this, arg);
        Long offset = 0l;
        for(AstRecType.AstCmpDefn cmp : uniType.cmps){
            Long size = (Long)cmp.accept(this, null); //arg? ali rabimo offset?
            // System.out.println(cmp.name+", "+size);
            if(size >= offset){
                offset = size;
            }
        }
        //a morjo bit padded?
        offset = align_n(offset, alignN);
		return offset;
	}

	@Override
	public Object visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
		cmpDefn.type.accept(this, arg); //lahko je novi struct, zato pejt po njem

        SemType t = SemAn.ofType.get(cmpDefn);
        Long i = type2size(t);
        Long offset = 0l;
        if(arg != null) offset = (Long)arg;
        MemRelAccess a = new MemRelAccess(i, offset, -1); //record componente majo depth -1
        Memory.cmpAccesses.put(cmpDefn,a);
		return i;
	}

    //strings:
    @Override
	public Object visit(AstAtomExpr atomExpr, Object arg) {
        SemType et = SemAn.ofType.get(atomExpr);
        if(et instanceof SemPointerType && ((SemPointerType)et).baseType instanceof SemCharType){
            //get value
            String value = atomExpr.value;
            value = value.substring(1, value.length()-1); //get value inside ""
            long len = (long) value.length() + 1l; //null terminated? if so, then +1
            MemAbsAccess a = new MemAbsAccess(charSize*len, new MemLabel(), atomExpr.value);
            this.LinAddDataChunk(a); // LINEARIZATION PHASE
            Memory.strings.put(atomExpr, a);
        }
        return null;
	}



    //vrne velikost tipa v bajtih
    public static long type2size(SemType type){
        if(type instanceof SemIntType) return MemEvaluator.intSize;
        else if(type instanceof SemPointerType) return MemEvaluator.ptrSize;
        else if(type instanceof SemBoolType) return MemEvaluator.boolSize;
        else if(type instanceof SemCharType) return MemEvaluator.charSize;
        else if(type instanceof SemVoidType) return 0l;
        else if (type instanceof SemArrayType){
            SemArrayType at = (SemArrayType)type;
            long elemSize = align_n( type2size(at.elemType), alignN); //align array elements (everything is aligned now)

            // return type2size( at.elemType  ) * at.size; //return size of element type multiplied by number of elements
            return elemSize * at.size;
        }
        
        //struct:
        else if (type instanceof SemStructType){
            //je align 8
            // System.out.println("je struct: elementi:");
            SemStructType st = (SemStructType)type;
            long size = 0;
            for(SemType t : st.cmpTypes){
                
                long size_i = type2size(t);
                
                if(size_i >= 0){
                    size = (size + size_i);
                    size = align_n(size, alignN); //add pad if needed
                    // System.out.printf("%d, %d, %d\n", size, size_i, pad);
                }
            }
            // System.out.printf("--> koncna velikost struct-a: %d\n", size);
            return size;
        }
        //union:
        else if (type instanceof SemUnionType){
            SemUnionType st = (SemUnionType)type;
            long size = 0;
            for(SemType t : st.cmpTypes){
                
                long size_i = type2size(t);
                if(size_i >= 0){
                    if(size_i >= size){ //za array type vecinoma
                        size = size_i;
                        // long pad = (8l - size%8l)%8l; //if not align8, pad //pad union?
                        // size = size+pad; //add pad if needed
                    }
                }
            }
            // System.out.printf("--> koncna velikost struct-a: %d\n", size);
            return size;
        }

        else if (type instanceof SemNameType){
            SemNameType sn = (SemNameType) type;
            // System.out.println(sn.actualType());
            return type2size(sn.actualType());
        }
        else return 0l;
    }
}


//object to pass size around (used in call expression)
class BSize{
    long size = 0l;

    public void setSize(long size){
        this.size = size;
    }
    public long getSize(){
        return this.size;
    }

}

