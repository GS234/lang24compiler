package lang24.phase.seman;

import java.util.*;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.tree.type.AstRecType.AstCmpDefn;
import lang24.data.ast.visitor.*;
import lang24.data.type.*;

/**
 * @author bostjan.slivnik@fri.uni-lj.si
 */


//data classes modifications:
//SemNameType: type needs to be public (check if exists)


public class TypeResolver implements AstFullVisitor<SemType, Object> {	
	@Override
	public SemType visit(AstNodes<? extends AstNode> nodes, Object arg) {
		nodes.accept(new TypeDefnAdder(), arg); //first loop: resolve name defns
		nodes.accept(new TypeChecker(), arg); //second loop: everything else
		return null;
	}
}

//name type resolver + adder
class TypeDefnAdder implements AstFullVisitor<SemType, Object>{
	private List<AstType> unresolvedTypes = new Vector<>(); //name types that could not be resolved in first pass, because of incomplete definitions
	public static final Map<SemRecordType, SymbTable> rec2namespace = new HashMap<>(); // type resolver NT

	@Override
	public SemType visit(AstNodes<? extends AstNode> nodes, Object arg) {
		//1st pass
		for(AstNode node : nodes){
			node.accept(this, new Vector<AstType>()); //arg is used to detect cycles
		}

		//2nd pass: add all those types, which could not be resolved on first pass
		// System.out.println(unresolvedTypes.size()); //arg is used to detect cycles
		for(AstType type : unresolvedTypes){
			type.accept(this, new Vector<AstType>());
		}
		return null;
	}

	// !!! this method is complex (but not really so complex, basically it does this):
	//recursively resolves name types:
	//	if type of the definition is nametype: resolve it
	//	else if type is null, it has not yet been added, add it to 
	//		list for second pass
	//	else: we know the type, add it to SemAn.isType.
	//	it also has logic to detect cycles (part that deals with arg)
	// @Override
	public SemType visit(AstNameType nameType, Object arg) {
		AstDefn nameTypeDefn = SemAn.definedAt.get(nameType); //get type definition
		if(arg != null && (arg instanceof List)){
			List<AstType> argl = (List<AstType>)arg;
			if(argl.contains(nameType) || (nameTypeDefn != null && argl.contains(nameTypeDefn.type))){
				throw new Report.Error(nameType.location(), "seman ) tip '"+nameType.name+"' je ciklicen! ("+nameType.location()+")"); //cycle detected: this is error
			}
			argl.add(nameType);
		}
		
		if(!(nameTypeDefn instanceof AstTypDefn)){
			throw new Report.Error(nameType.location(), "seman ) ne mores nastaviti za tip, ker '"+nameTypeDefn.name+"' ni definicija tipa! ("+nameTypeDefn.location()+")");
		}
		
		SemNameType n = (SemNameType)SemAn.isType.get(nameType); //if current node was already defined (in 1st pass), update its type, otherwise add new
		SemType type = SemAn.isType.get(nameTypeDefn.type); //check the type of nametype definition
		if(type instanceof SemNameType){ //if it is another nametype, go resolve it
			type = nameTypeDefn.type.accept(this, arg);
			// if(unresolvedTypes.contains(nameType)) unresolvedTypes.remove(nameType); //we resolved type type, remove from unresolvedTypes
		}
		else if(type == null && n == null){ //add to list for second pass if n not null (je mogoce definiran naprej)
			// System.out.println("[+] adding " + nameType.name + " to unresolved types for second pass");
			unresolvedTypes.add(nameType);
		}
		// else; //do nothing, we already know which type this is, set it!
		
		if(n == null){ //first encounter, make new and insert into SemAn.isType
			n = new SemNameType(nameType.name); //dodaj se symbol table?
			// System.out.println("[!] adding "+nameType.name+" to SemAn.isType");
			SemAn.isType.put(nameType, n);
			n.define(type);
		}
		
		try{
			// System.out.println("[!] updating "+nameType.name+" type to " + type); //update type
			if(n.type == null) n.define(type); //type needs to be public in SemNameType!!
		}
		catch (Error e){
			System.out.println("[!] tried to update "+n.type()+" to "+type);
		}
		return n;
	}

	//t1
	@Override
	public SemType visit(AstAtomType atomType, Object arg) {
		switch(atomType.type){
			case INT:  {
				SemAn.isType.put(atomType,SemIntType.type);
				return SemIntType.type;
				}
			case BOOL: {
				SemAn.isType.put(atomType,SemBoolType.type);
				return SemBoolType.type;
				}
			case CHAR: {
				SemAn.isType.put(atomType,SemCharType.type);
				return SemCharType.type;
				}
			case VOID: {
				SemAn.isType.put(atomType,SemVoidType.type);
				return SemVoidType.type;
				}
		}
		return null;
	}

	//t3
	@Override
	public SemType visit(AstStrType strType, Object arg) {
		//check for cycles
		if(arg != null && (arg instanceof List)){
			List<AstType> argl = (List<AstType>)arg;
			if(argl.contains(strType)) throw new Report.Error(strType.location(), "seman ) tip '"+strType+"' je ciklicen! ("+strType.location()+")"); //cikel: to je error
			argl.add(strType);
		}
		// ----
		List<SemType> types = new Vector<>();
		for (final AstNode node : strType.cmps){ 
			SemType t_i = ((AstRecType.AstCmpDefn)node).accept(this, arg); //get component type
			if(! (t_i instanceof SemVoidType)) types.add(t_i);
			else System.out.println("[!] tip je void, ni vredu!");
		}
		SemStructType st = new SemStructType(types);
		SemAn.isType.put(strType, st);
		rec2namespace.put(st, strType.symbTable); //add struct's namespace to map
		return st;
	}

	//t4
	@Override
	public SemType visit(AstUniType uniType, Object arg) {
		//check for cycles
		if(arg != null && (arg instanceof List)){
			List<AstType> argl = (List<AstType>)arg;
			if(argl.contains(uniType)) throw new Report.Error(uniType.location(), "seman ) tip '"+uniType+"' je ciklicen! ("+uniType.location()+")"); //cikel: to je error
			argl.add(uniType);
		}
		// ----
		List<SemType> types = new Vector<>();
		for (final AstNode node : uniType.cmps){
			AstCmpDefn cmpDefn = (AstCmpDefn)node;
			SemType t_i = cmpDefn.accept(this, arg); //get component type
			if(! (t_i instanceof SemVoidType)) types.add(t_i);
			else System.out.println("[!] tip je void, ni vredu!");
		}
		SemUnionType ut = new SemUnionType(types);
		SemAn.isType.put(uniType, ut);
		rec2namespace.put(ut, uniType.symbTable); //add union's namespace to map
		return ut;
	}

	//t5
	@Override
	public SemType visit(AstPtrType ptrType, Object arg) {
		SemType bt = ptrType.baseType.accept(this, new Vector<AstType>());
		SemPointerType pt = new SemPointerType(bt);
		SemAn.isType.put(ptrType, pt);
		return pt;
	}

	//pointer na array?
	@Override
	public SemType visit(AstArrType arrType, Object arg) {
		SemType et = arrType.elemType.accept(this, arg);
		
		SemType size = arrType.size.accept(new TypeChecker(), arg);
		if(size.actualType() instanceof SemIntType){
			long value = Long.parseLong(((AstAtomExpr)arrType.size).value);
			if(value > 0 && value <=  ((1l<<63)-1)){
				SemArrayType at = new SemArrayType(et, value);
				SemAn.isType.put(arrType, at);
				return at;
			}
		}
		return null;
	}

	//component definitions:
	@Override
	public SemType visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
		SemType t = cmpDefn.type.accept(this, arg);
		SemAn.ofType.put(cmpDefn, t);
		return t;
	}

}


//main thing happens here
class TypeChecker implements AstFullVisitor<SemType, Object> {
	private final boolean throwErrorOnError = true; //flag: za debug je fino, ce je false (treba bo se popravljat seman verjetno, verjetno tudi imcgen)
	protected AstDefn fnd(String name, Location l, SymbTable namespace) throws Report.Error{
		try{
			return namespace.fnd(name);
		}
		catch(SymbTable.CannotFndNameException e){
			throw new Report.Error("seman ) ["+l+"]: Name '"+name+"' does not exist.");
		}
	}

	private void errorMessage(String message){
		System.out.println(message);
		if(throwErrorOnError) throw new Report.Error("seman ) " + message);
	}

	/**
	 * Structural equivalence of types.
	 * 
	 * @param type1 The first type.
	 * @param type2 The second type.
	 * @return {@code true} if the types are structurally equivalent, {@code false}
	 *         otherwise.
	 */
	private boolean equiv(SemType type1, SemType type2) {
		return equiv(type1, type2, new HashMap<SemType, HashSet<SemType>>());
	}

	/**
	 * Structural equivalence of types.
	 * 
	 * @param type1  The first type.
	 * @param type2  The second type.
	 * @param equivs Type synonyms assumed structurally equivalent.
	 * @return {@code true} if the types are structurally equivalent, {@code false}
	 *         otherwise.
	 */
	private boolean equiv(SemType type1, SemType type2, HashMap<SemType, HashSet<SemType>> equivs) {

		if ((type1 instanceof SemNameType) && (type2 instanceof SemNameType)) {
			if (equivs == null)
				equivs = new HashMap<SemType, HashSet<SemType>>();

			if (equivs.get(type1) == null)
				equivs.put(type1, new HashSet<SemType>());
			if (equivs.get(type2) == null)
				equivs.put(type2, new HashSet<SemType>());
			if (equivs.get(type1).contains(type2) && equivs.get(type2).contains(type1))
				return true;
			else {
				HashSet<SemType> types;

				types = equivs.get(type1);
				types.add(type2);
				equivs.put(type1, types);

				types = equivs.get(type2);
				types.add(type1);
				equivs.put(type2, types);
			}
		}

		type1 = type1.actualType();
		type2 = type2.actualType();

		if (type1 instanceof SemVoidType)
			return (type2 instanceof SemVoidType);
		if (type1 instanceof SemBoolType)
			return (type2 instanceof SemBoolType);
		if (type1 instanceof SemCharType)
			return (type2 instanceof SemCharType);
		if (type1 instanceof SemIntType)
			return (type2 instanceof SemIntType);

		if (type1 instanceof SemArrayType) {
			if (!(type2 instanceof SemArrayType))
				return false;
			final SemArrayType arr1 = (SemArrayType) type1;
			final SemArrayType arr2 = (SemArrayType) type2;
			if (arr1.size != arr2.size)
				return false;
			return equiv(arr1.elemType, arr2.elemType, equivs);
		}

		if (type1 instanceof SemPointerType) {
			if (!(type2 instanceof SemPointerType))
				return false;
			final SemPointerType ptr1 = (SemPointerType) type1;
			final SemPointerType ptr2 = (SemPointerType) type2;
			if ((ptr1.baseType.actualType() instanceof SemVoidType)
					|| (ptr2.baseType.actualType() instanceof SemVoidType))
				return true;
			return equiv(ptr1.baseType, ptr2.baseType, equivs);
		}

		if (type1 instanceof SemStructType) {
			if (!(type2 instanceof SemStructType))
				return false;
			final SemStructType str1 = (SemStructType) type1;
			final SemStructType str2 = (SemStructType) type2;
			if (str1.cmpTypes.size() != str2.cmpTypes.size())
				return false;
			for (int c = 0; c < str1.cmpTypes.size(); c++)
				if (!(equiv(str1.cmpTypes.get(c), str2.cmpTypes.get(c), equivs)))
					return false;
			return true;
		}
		if (type1 instanceof SemUnionType) {
			if (!(type2 instanceof SemUnionType))
				return false;
			final SemUnionType uni1 = (SemUnionType) type1;
			final SemUnionType uni2 = (SemUnionType) type2;
			if (uni1.cmpTypes.size() != uni2.cmpTypes.size())
				return false;
			for (int c = 0; c < uni1.cmpTypes.size(); c++)
				if (!(equiv(uni1.cmpTypes.get(c), uni2.cmpTypes.get(c), equivs)))
					return false;
			return true;
		}

		throw new Report.InternalError();
	}

	//t1
	@Override
	public SemType visit(AstAtomType atomType, Object arg) {
		switch(atomType.type){
			case INT:  {
				SemAn.isType.put(atomType,SemIntType.type);
				return SemIntType.type;
				}
			case BOOL: {
				SemAn.isType.put(atomType,SemBoolType.type);
				return SemBoolType.type;
				}
			case CHAR: {
				SemAn.isType.put(atomType,SemCharType.type);
				return SemCharType.type;
				}
			case VOID: {
				SemAn.isType.put(atomType,SemVoidType.type);
				return SemVoidType.type;
				}
		}
		return null;
	}

	//type:
	//array type: t2
	@Override
	public SemType visit(AstArrType arrType, Object arg) {
		SemType et = arrType.elemType.accept(this, arg);
		if(et.actualType() instanceof SemVoidType) this.errorMessage("[!] (arrType) "+arrType.location()+" tip je void"); //ampak, ce je name type, je lahko tudi void, al ne? pol pa ni vredu, treba je rekurzivno pogledat tip
		SemType at = SemAn.isType.get(arrType); //je ze dodan
		if(at != null) return at;
		// SemType size = arrType.size.accept(new TypeChecker(), arg);
		// if(size.actualType() instanceof SemIntType){
		// 	long value = Long.parseLong(((AstAtomExpr)arrType.size).value);
		// 	if(value > 0 && value <=  ((1l<<63)-1)){
		// 		SemArrayType at = new SemArrayType(et, value);
		// 		SemAn.isType.put(arrType, at);
		// 		return at;
		// 	}
		// }
		return null;
	}


	//t3
	@Override
	public SemType visit(AstStrType strType, Object arg) {
		//loop preko komponent, preveri njihov tip:
		//komponente:
		List<SemType> types = new Vector<>();
		for (final AstNode node : strType.cmps){
			SemType t_i = ((AstRecType.AstCmpDefn)node).accept(this, arg); //pridobi tip komponente
			if(! (t_i instanceof SemVoidType)) types.add(t_i);
			else this.errorMessage("[!] (strType) "+strType.location()+" tip je void, ni vredu!");
		}

		SemStructType st = new SemStructType(types);
		SemAn.isType.put(strType, st);
		return st;
	}

	//t4
	@Override
	public SemType visit(AstUniType uniType, Object arg) {
		List<SemType> types = new Vector<>();
		for (final AstNode node : uniType.cmps){
			SemType t_i = ((AstRecType.AstCmpDefn)node).accept(this, arg); //pridobi tip komponente
			if(! (t_i instanceof SemVoidType)) types.add(t_i);
			else this.errorMessage("[!] (uniType) "+uniType.location()+" tip je void, ni vredu!"); //ni vredu, error!
		}

		SemUnionType ut = new SemUnionType(types);
		SemAn.isType.put(uniType, ut);
		return ut;
	}

	//t5
	@Override
	public SemType visit(AstPtrType ptrType, Object arg) {
		SemType bt = ptrType.baseType.accept(this, arg);
		SemPointerType pt = new SemPointerType(bt);
		SemAn.isType.put(ptrType, pt);
		return pt;
	}

	//name type:
	public SemType visit(AstNameType nameType, Object arg) {
		return SemAn.isType.get(nameType); //is defined AND has type! (if TypeDefnAdder visited tree)
	}
	//type
	
	//expression (v1, v2):
	@Override
	public SemType visit(AstAtomExpr atomExpr, Object arg) {
		switch(atomExpr.type){
			case INT: {
				SemAn.ofType.put(atomExpr,SemIntType.type);
				return SemIntType.type;
				}
			case BOOL: {
				SemAn.ofType.put(atomExpr,SemBoolType.type);
				return SemBoolType.type;
				}
			case CHAR: {
				SemAn.ofType.put(atomExpr,SemCharType.type);
				return SemCharType.type;
				}
			case VOID:  {
				SemAn.ofType.put(atomExpr,SemVoidType.type);
				return SemVoidType.type;
				}
			case STR: {
				SemPointerType ptrType = new SemPointerType(SemCharType.type);
				SemAn.ofType.put(atomExpr,ptrType);
				return ptrType;
				}
			case PTR: {
				SemAn.ofType.put(atomExpr,SemPointerType.type);
				// return SemVoidType.type;
				return SemPointerType.type;
				}
		}
		return null;
	}

	//v3
	@Override
	public SemType visit(AstPfxExpr pfxExpr, Object arg) {
		SemType et = pfxExpr.expr.accept(this, arg);
		switch(pfxExpr.oper){
			case ADD: case SUB:{
				if(et == SemIntType.type){
					SemAn.ofType.put(pfxExpr, SemIntType.type);
					return SemIntType.type;
				}
				break;
			}
			case NOT:{
				if(et == SemBoolType.type){
					SemAn.ofType.put(pfxExpr, SemBoolType.type);
					return SemBoolType.type;
				}
				break;			
			}
			case PTR:{
				//v8.1
				
				Boolean b = SemAn.isLVal.get(pfxExpr.expr);
				b = true;
				if(b != null && b){
					SemPointerType pt = new SemPointerType(et);
					SemAn.ofType.put(pfxExpr, pt);
					return pt;
				}
				else this.errorMessage("[!] (pfxExpr) "+pfxExpr.location()+" ni lval, napaka");
				break;
			}
		}
		this.errorMessage("[!] (pfxExpr) "+pfxExpr.location()+" napaka: napacen tip");
		return null;
	}

	
	@Override
	public SemType visit(AstBinExpr binExpr, Object arg) {
		//v4, v5, v6, v7 (binExpr)
		SemType et1 = binExpr.fstExpr.accept(this, arg); //SemType fstType = binExpr.fstExpr.accept(this, arg);
		SemType et2 = binExpr.sndExpr.accept(this, arg); //SemType sndType = binExpr.sndExpr.accept(this, arg);
		SemType returnType;

		//v4:
		if(
			binExpr.oper == AstBinExpr.Oper.AND ||
			binExpr.oper == AstBinExpr.Oper.OR
		){
			//preveri tipa podizrazov
			if(et1 == SemBoolType.type && et2 == SemBoolType.type){
				returnType = SemBoolType.type;
				SemAn.ofType.put(binExpr, returnType);
				return returnType;
			}
			else this.errorMessage("[!] (binExpr) "+binExpr.location()+" napaka: izrazi niso pravih tipov");
		}

		//v5
		if( 
			binExpr.oper == AstBinExpr.Oper.ADD ||
			binExpr.oper == AstBinExpr.Oper.SUB ||
			binExpr.oper == AstBinExpr.Oper.MUL ||
			binExpr.oper == AstBinExpr.Oper.DIV ||
			binExpr.oper == AstBinExpr.Oper.MOD
		){
			//if(SemAn.ofType.get(binExpr.fstExpr) == SemIntType.type && SemAn.ofType.get(binExpr.sndExpr) == SemIntType.type){
			if(et1 == SemIntType.type && et2 == SemIntType.type){
				returnType = SemIntType.type;
				SemAn.ofType.put(binExpr, returnType);
				return returnType;
			}
			else this.errorMessage("[!] (binExpr) "+binExpr.location()+" napaka: izrazi niso pravih tipov");
		}
		//v6
		if(
			binExpr.oper == AstBinExpr.Oper.EQU ||
			binExpr.oper == AstBinExpr.Oper.NEQ
		){
			//tip je lahko: 
			//bool, char, int oz. ptr(t) [t je datatype]
			if(this.isEqNeComparable(et1) && this.isEqNeComparable(et2)){
				returnType = SemBoolType.type;
				SemAn.ofType.put(binExpr, returnType);
				return returnType;
			}
			else this.errorMessage("[!] (binExpr) "+binExpr.location()+" napaka: izrazi niso pravih tipov");
		}

		//v7
		if(
			binExpr.oper == AstBinExpr.Oper.LTH ||
			binExpr.oper == AstBinExpr.Oper.GTH ||
			binExpr.oper == AstBinExpr.Oper.LEQ ||
			binExpr.oper == AstBinExpr.Oper.GEQ
		){
			//tip je lahko: 
			//char, int oz. ptr(t) [t je datatype]
			if(this.isNumComparable(et1) && this.isNumComparable(et2)){
				returnType = SemBoolType.type;
				SemAn.ofType.put(binExpr, returnType);
				return returnType;
			}
			else this.errorMessage("[!] (binExpr) "+binExpr.location()+" napaka: izrazi niso pravih tipov");
		}
		
		return null;
	}
	//pomozne metode za AstBinExpr visit:
	//za v6:
	private boolean isEqNeComparable(SemType t){
		return (
			t == SemIntType.type ||
			t == SemCharType.type || 
			t == SemBoolType.type || 
			t instanceof SemPointerType
			);
	}

	//za v7:
	private boolean isNumComparable(SemType t){
		return (
			t == SemIntType.type ||
			t == SemCharType.type || 
			t instanceof SemPointerType
			);
	}
	
	//za v12
	private boolean isPrimitiveType(SemType t){
		return (
			t == SemIntType.type ||
			t == SemCharType.type || 
			t == SemBoolType.type || 
			t == SemVoidType.type || 
			t instanceof SemPointerType
			);
	}
	//----

	//v8.2, (v8.1 -> v3 (pfxexpr))
	//dereference:
	@Override
	public SemType visit(AstSfxExpr sfxExpr, Object arg) { // TODO - fix
		SemType set = sfxExpr.expr.accept(this, arg);
		if(set instanceof SemPointerType){
			set = ((SemPointerType)set).baseType;
		}
		else this.errorMessage("[!] (sfxExpr) "+sfxExpr.location()+" napaka: ni pointer");
		SemAn.ofType.put(sfxExpr, set);
		SemAn.isLVal.put(sfxExpr, true); //lvalue add
		return set;
	}
	
	
	//v9
	@Override
	public SemType visit(AstArrExpr arrExpr, Object arg) {
		SemType tar = arrExpr.arr.accept(this, arg);
		SemType tid = arrExpr.idx.accept(this, arg);

		Boolean b = SemAn.isLVal.get(arrExpr.arr);
		if(tar instanceof SemArrayType && tid instanceof SemIntType && (b != null && b)){
			SemType t = ((SemArrayType)tar).elemType;
			SemAn.ofType.put(arrExpr, t);
			return t;
		}

		return null;
	}

	//v12: dol

	//v13:
	@Override
	public SemType visit(AstCastExpr castExpr, Object arg) {
		SemType t = castExpr.type.accept(this, arg); //type
		SemType et = castExpr.expr.accept(this, arg); //expression type

		if(this.isNumComparable(SemAn.isType.get(castExpr.type)) && this.isNumComparable(SemAn.ofType.get(castExpr.expr))){
			SemAn.ofType.put(castExpr, t);
			return t;
		}
		else this.errorMessage("[!] (castExpr) "+castExpr.location()+" napaka: napacen tip");
		return null;
	}
	
	//v14 (sizeof, (expr)):
	@Override
	public SemType visit(AstSizeofExpr sizeofExpr, Object arg) {
		SemType t = sizeofExpr.type.accept(this, arg);
		if(SemAn.isType.get(sizeofExpr.type) != null){
			SemAn.ofType.put(sizeofExpr, SemIntType.type);
			return SemIntType.type;
		}
		else this.errorMessage("[!] (sizeofExpr) "+sizeofExpr.location()+" napaka: ni tip");
		return null;
	}

	//statements: 
	//s1 -> dela ko urca
	@Override
	public SemType visit(AstAssignStmt assignStmt, Object arg) {
		SemType t1_real = assignStmt.dst.accept(this, arg);
		SemType t2_real = assignStmt.src.accept(this, arg);
		
		Boolean b = SemAn.isLVal.get(assignStmt.dst);
		if(t1_real != null && t2_real != null){
			SemType t1 = t1_real.actualType();
			SemType t2 = t1_real.actualType();

			// System.out.printf("("+assignStmt.location()+") real: %s, %s, actual: %s, %s; equiv: %s;;", t1_real, t2_real, t1, t2, this.equiv(t1, t2));
			// System.out.printf("%s, %s\n", this.isEqNeComparable(t1),this.isEqNeComparable(t2));

			if(this.isEqNeComparable(t1) && this.isEqNeComparable(t2) && this.equiv(t1, t2)  && (b != null && b)){
				SemAn.ofType.put(assignStmt, SemVoidType.type);
				return SemVoidType.type;
			}
			else this.errorMessage("[!] (assignStmt) "+assignStmt.location()+" napaka: ni pravi tip/lvalue!");
		}
		else this.errorMessage("[!] (assignStmt) "+assignStmt.location()+" napaka: podtip(a) je(sta) null!");
		return null;
	}

	//s2
	@Override
	public SemType visit(AstExprStmt exprStmt, Object arg) {
		SemType et = exprStmt.expr.accept(this, arg);
		if(et instanceof SemVoidType){
			SemAn.ofType.put(exprStmt, SemVoidType.type);
			return SemVoidType.type;
		}
		return null;
	}

	//s3, s4
	@Override
	public SemType visit(AstIfStmt ifStmt, Object arg) {
		SemType tcon = ifStmt.cond.accept(this, arg);
		SemType tstm = ifStmt.thenStmt.accept(this, arg);
		SemType test = null;
		if (ifStmt.elseStmt != null)
			test = ifStmt.elseStmt.accept(this, arg);
		
		if(tcon != null && (tcon instanceof SemBoolType) && tstm != null){
			if(ifStmt.elseStmt != null && test == null) this.errorMessage("[!] (ifstmt - else) "+ifStmt.location()+" napaka: manjka else tip");
			
			SemAn.ofType.put(ifStmt, SemVoidType.type);
			return SemVoidType.type;
		}
		else this.errorMessage("[!] (ifstmt) "+ifStmt.location()+" napaka!");
		return null;
	}
	
	
	//s5
	@Override
	public SemType visit(AstWhileStmt whileStmt, Object arg) {
		SemType tcon = whileStmt.cond.accept(this, arg);
		SemType twst = whileStmt.stmt.accept(this, arg);

		if(tcon != null && tcon instanceof SemBoolType && twst != null){
			SemAn.ofType.put(whileStmt, SemVoidType.type);
			return SemVoidType.type;
		}
		else this.errorMessage("[!] (whilestmt) "+whileStmt.location()+" napaka!");
		return null;
	}
	
	//s6 
	@Override
	public SemType visit(AstReturnStmt retStmt, Object arg) {
		SemType t = retStmt.expr.accept(this, arg);
		if(t != null){
			SemAn.ofType.put(retStmt, SemVoidType.type);
			return SemVoidType.type;
		}
		return null;
	}
	
	//s7
	@Override
	public SemType visit(AstBlockStmt blockStmt, Object arg) {
		if (blockStmt.stmts != null){
			for(AstNode node : blockStmt.stmts){
				SemType t = node.accept(this, arg);
				if(t == null){
					this.errorMessage("[!] (blockStmt) "+blockStmt.location()+" napaka: tip node-a ("+node.location()+") ni znan");
					// return null;
				}
			}
			SemAn.ofType.put(blockStmt, SemVoidType.type);
			return SemVoidType.type;
		}
		return null;
	}


	//v10, 11

	//helper methods
	private AstType nameTypeFinder(AstType type){
		if(type instanceof AstNameType){
			AstDefn defnType = SemAn.definedAt.get(type);
			return nameTypeFinder(defnType.type);
		}
		else return type;
	}
	private SymbTable getNamespace(AstType t){
		if(t instanceof AstRecType) return ((AstRecType)t).symbTable;
		else return new SymbTable();
	}
	
	private SymbTable cmpResolver(AstCmpExpr expr, Object arg){
		//component; pridobi tip:
		AstExpr next = expr.expr;
		if(next instanceof AstCmpExpr){ //ok, gremo dalje
			SymbTable namespace = cmpResolver((AstCmpExpr)next, arg);
			
			//REPEATED CODE
			AstDefn cmpDefn = this.fnd(expr.name, expr.location(), namespace);
			SemAn.definedAt.put(expr, cmpDefn);
			SemType cmpSemType = SemAn.ofType.get(cmpDefn);
			SemAn.ofType.put(expr, cmpSemType);
			
			//possible next namespace
			AstType nextType = nameTypeFinder(cmpDefn.type);
			SymbTable nextNamespace = getNamespace(nextType);
			return nextNamespace;
			//REPEATED CODE END
		}
		else if(next instanceof AstNameExpr){
			//visit it, to determine nameType (!)
			next.accept(this, arg);

			AstDefn nameDefn = SemAn.definedAt.get(next);
			AstType defnType = nameTypeFinder(nameDefn.type);
			if(defnType instanceof AstRecType){
				SymbTable namespace = ((AstRecType)defnType).symbTable;
				
				//REPEATED CODE
				AstDefn cmpDefn = this.fnd(expr.name, expr.location(), namespace); //this defn -> bind to definition:
				SemAn.definedAt.put(expr, cmpDefn); //link component to its definition
				SemType cmpSemType = SemAn.ofType.get(cmpDefn);
				SemAn.ofType.put(expr, cmpSemType);

				//possible next namespace
				AstType nextType = nameTypeFinder(cmpDefn.type);
				SymbTable nextNamespace = getNamespace(nextType); //get next namespace, if exists
				//REPEATED CODE END

				return nextNamespace; //rekurzija stopping criteria
			}
			else {
				//ni vredu, treba je vrst error, ker drugi tipi nimajo namespace-a
				this.errorMessage("[!] (cmpResolver) "+expr.location()+" napaka: tip nima namespace-a.");
				return new SymbTable(); //just so that it is not null, vem FUJ
			}
		}
		else{ //zadnja sansa: ce je record type, ima namespace, drugace nima
			next.accept(this, arg); //resolve it's type, get namespace from type
			
			//type resolver NT (v2)
			SemType t_real = SemAn.ofType.get(next);
			if(t_real != null && (t_real.actualType() instanceof SemRecordType)){
				SemRecordType t_actual = (SemRecordType)(t_real.actualType());
				SymbTable namespace = TypeDefnAdder.rec2namespace.get(t_actual);
				if(namespace != null){
					
					//REPEATED CODE
					AstDefn cmpDefn = this.fnd(expr.name, expr.location(), namespace);
					SemAn.definedAt.put(expr, cmpDefn);
					SemType cmpSemType = SemAn.ofType.get(cmpDefn);
					SemAn.ofType.put(expr, cmpSemType);

					//possible next namespace
					AstType nextType = nameTypeFinder(cmpDefn.type);
					SymbTable nextNamespace = getNamespace(nextType);
					return nextNamespace;
					//REPEATED CODE END
				}
				this.errorMessage("[!] (cmpResolver) "+expr.location()+" napaka: record nima namespace-a.");
				return new SymbTable();
				
			}
			else{
				this.errorMessage("[!] (cmpResolver) "+expr.location()+" napaka: invalidna struktura (cmpAccess stack).");
				return new SymbTable();
			}
		}
	}
	//-----

	//lahko bi pa tut drugac naredu (pri obhodu navzdol, bi lahko nastavljal tudi definedAt
	//in mi ne bi bilo treba sabo nest namespace-a)
	@Override
	public SemType visit(AstCmpExpr cmpExpr, Object arg) {
		//pojdi po tipu navzdol
		this.cmpResolver(cmpExpr, arg);
		return SemAn.ofType.get(cmpExpr);
	}

	

	//declarations:
	//d1, d2

	@Override
	public SemType visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
		SemType t = cmpDefn.type.accept(this, arg);
		SemAn.ofType.put(cmpDefn, t);
		return t;
	}

	@Override
	public SemType visit(AstVarDefn varDefn, Object arg) {
		SemType t = varDefn.type.accept(this, arg);
		SemAn.ofType.put(varDefn, t);
		return t;
	}
	
	@Override
	public SemType visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
		SemType t = refParDefn.type.accept(this, arg);
		SemAn.ofType.put(refParDefn, t);
		return t;
	}

	@Override
	public SemType visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
		SemType t = valParDefn.type.accept(this, arg);
		SemAn.ofType.put(valParDefn, t);
		return t;
	}


	@Override
	public SemType visit(AstNameExpr nameExpr, Object arg) {
		//System.out.println("nameexpr");
		AstDefn d = SemAn.definedAt.get(nameExpr);
		// System.out.println(d+" "+d.name);
		if(d instanceof AstTypDefn){ //type definition/declaration (d1)
			SemType tdef = SemAn.isType.get(d.type); //.actualType();
			// System.out.println(tdef);
			SemAn.isType.put(nameExpr, tdef);
			return tdef;
		}
		if(d instanceof AstVarDefn){ //variable definition/declaration (d2)
			SemType vdef = SemAn.isType.get(d.type); //.actualType();
			if(!(vdef instanceof SemVoidType)){
				// System.out.println(vdef);
				SemAn.ofType.put(nameExpr, vdef);
				return vdef;
			}
			else this.errorMessage("[!] (nameExpr) "+nameExpr.location()+" napaka: imenski tip je void");
		}
		if(d instanceof AstFunDefn.AstValParDefn){ //parametri
			SemType pvdef = SemAn.isType.get(d.type); //.actualType();
			
			if(!(pvdef instanceof SemVoidType)){
				SemAn.ofType.put(nameExpr, pvdef);
				return pvdef;
			}
			else this.errorMessage("[!] (nameExpr) "+nameExpr.location()+" napaka: imenski tip je void");
		}
		if(d instanceof AstFunDefn.AstRefParDefn){ //parametri (ref)
			SemType prdef = SemAn.isType.get(d.type);
			
			if(!(prdef instanceof SemVoidType)){
				SemAn.ofType.put(nameExpr, prdef);
				return prdef;
			}
			else this.errorMessage("[!] (nameExpr) "+nameExpr.location()+" napaka: imenski tip je void");
		}
		return null;
	}

	
	//d3, d4: (function definitions)
	
	@Override
	public SemType visit(AstFunDefn funDefn, Object arg) {
		SemType t = SemAn.ofType.get(funDefn);
		if(t != null) return t; //smo ze obiskali to
		t = funDefn.type.accept(this, arg);
		if(isPrimitiveType(t)){
			SemAn.ofType.put(funDefn, t);
		}
		else this.errorMessage("[!] (funDefn) "+funDefn.location()+" napaka: funkcijski tip ni primitivni");


		if (funDefn.pars != null)
			for(AstDefn param : funDefn.pars){
				SemType paramType = param.accept(this, arg);
				if(!this.isEqNeComparable(paramType)){
					this.errorMessage("[!] (funDefn) "+funDefn.location()+" napaka: (parametri) parameter ("+param.name+": "+param.location()+") ni pravega tipa.");
					return null;
				}
			}
		if (funDefn.defns != null)
			funDefn.defns.accept(this, arg);
		if (funDefn.stmt != null){ //d4
				SemType stmtType = funDefn.stmt.accept(this, arg);
				if(!(stmtType instanceof SemVoidType)){
					this.errorMessage("[!] (funDefn) "+funDefn.location()+" napaka: stmt ("+funDefn.stmt.location()+") ni void type");
					return null;
				}
			}
		return t;
	}


	//v12:
	@Override
	public SemType visit(AstCallExpr callExpr, Object arg) {
		//get function definition:
		AstFunDefn fn = (AstFunDefn)(SemAn.definedAt.get(callExpr));//SemAn.ofType.get(callExpr.type);
		SemType tfn = SemAn.ofType.get(fn);//fn.type;
		// System.out.printf("%s\n",tfn);
		if(tfn == null){
			tfn = fn.accept(this, arg);
		}
		
		Iterator<AstFunDefn.AstParDefn> parDefIterator = null;
		if(fn.pars != null) parDefIterator = fn.pars.iterator();
		if (callExpr.args != null){

			//napaka: imamo argumente, ampak funkcija jih ne sprejema
			if(parDefIterator == null){
				this.errorMessage("[!] (callExpr) "+callExpr.location()+" napaka: prevec argumentov");
				return null;
			}
			else{
				callExpr.args.accept(this, arg); //argument types
				for(AstExpr fnArg : callExpr.args){
					if(!parDefIterator.hasNext()){
						this.errorMessage("[!] (callExpr) "+callExpr.location()+" napaka: prevec argumentov");
						return null;
					}
					AstDefn parDefn = parDefIterator.next(); //first element
					//check if types are the same:
					SemType t1 = SemAn.ofType.get(parDefn);
					SemType t2 = SemAn.ofType.get(fnArg);
					if(!this.equiv(t1, t2)){
						this.errorMessage("[!] (callExpr) "+callExpr.location()+" napaka: tipa parametra in argumenta ("+parDefn.name+") se ne ujemata!");
						return null;
					}
					if(parDefn instanceof AstFunDefn.AstRefParDefn){ //ce je ta ref par defn, potem mora biti expr lvalue
						Boolean b = SemAn.isLVal.get(fnArg);
						if(b != null && b); //ok
						else{
							this.errorMessage("[!] (callExpr) "+callExpr.location()+" napaka: argument ni lval (parameter je AstRefParDefn)");
							return null;
						}
					}
				}
			}
		}
		if(parDefIterator != null && parDefIterator.hasNext()){ //ce funkcija pricakuje se argumentov, potem je to napaka
			this.errorMessage("[!] (callExpr) "+callExpr.location()+" napaka: premalo argumentov");
			return null;
		}
		else{ //ce je vse ok, potem:
			// System.out.printf("callExpr: je vse ok: %s, %s\n", fn, tfn);
			SemAn.ofType.put(callExpr, tfn);
			return tfn;
		}
	}
	
}

//other visitors:
class ActualTypePrinter implements AstFullVisitor<Object, Object>{
	@Override
	public Object visit(AstNameType nameType, Object arg) {
		System.out.printf("%s, %s\n", nameType.name, SemAn.isType.get(nameType).actualType());
		return null;
	}
}

