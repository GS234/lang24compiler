package lang24.phase.seman;

import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.type.AstNameType;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.visitor.*;

/**
 * Lvalue resolver.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class LValResolver implements AstFullVisitor<Object, Object> {

	/** Constructs a new lvalue resolver. */
	public LValResolver() {
	}

	// @Override
	// public Object visit(AstVarDefn varDefn, Object arg) {
	// 	SemAn.isLVal.put(varDefn, true);
	// 	varDefn.type.accept(this, arg);
	// 	return null;
	// }

	// @Override
	// public Object visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
	// 	SemAn.isLVal.put(refParDefn, true);
	// 	refParDefn.type.accept(this, arg);
	// 	return null;
	// }

	// @Override
	// public Object visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
	// 	SemAn.isLVal.put(valParDefn, true);
	// 	valParDefn.type.accept(this, arg);
	// 	return null;
	// }
	
	@Override
	public Object visit(AstNameExpr nameExpr, Object arg) {
		// System.out.println("imam ime: " + nameExpr.name);
		AstDefn d = SemAn.definedAt.get(nameExpr);//SemAn.definedAt.get(nameExpr);
		if(!(d instanceof AstTypDefn)) 
			SemAn.isLVal.put(nameExpr, true); //put only if not typedefn
		return null;
	}




	@Override
	public Object visit(AstCmpExpr cmpExpr, Object arg) {
		cmpExpr.expr.accept(this, arg); //najprej pejt naprej pogledat, pol poglej, ce je lval, potem dodaj
		Boolean b = SemAn.isLVal.get(cmpExpr.expr);
		if(b != null && b){
			SemAn.isLVal.put(cmpExpr, true);
		}
		return null;
	}

	@Override
	public Object visit(AstArrExpr arrExpr, Object arg) {
		arrExpr.arr.accept(this, arg);
		arrExpr.idx.accept(this, arg);
		Boolean b = SemAn.isLVal.get(arrExpr.arr);
		// System.out.printf("is lval? : %s\n", b);
		if(b != null && b){
			SemAn.isLVal.put(arrExpr, true);
		}
		return null;
	}

	//TODO: is cast lvalue?
	@Override
	public Object visit(AstCastExpr castExpr, Object arg) {
		castExpr.type.accept(this, arg);
		castExpr.expr.accept(this, arg);
		Boolean b = SemAn.isLVal.get(castExpr.expr);
		if(b != null && b){
			SemAn.isLVal.put(castExpr, true);
		}
		return null;
	}

	//pazi! tuki se ne vemo, ce je lahko to lval, preverjanje se zgodi v TypeChecker-ju (ali je sploh pointer, ker takrat sele vemo)
	@Override
	public Object visit(AstSfxExpr sfxExpr, Object arg){
		// Boolean b = SemAn.isLVal.get(sfxExpr.expr);
		//tip se preveri v typechecker-ju
		// if(b != null && b){
		SemAn.isLVal.put(sfxExpr, true); //samo da ni morebitnih tezav kasneje
		// }
		return null;
	}
	

}

/*
// lang24.data.ast.tree:

	@Override
	public default Result visit(AstNodes<? extends AstNode> nodes, Argument arg) {
		for (final AstNode node : nodes)
			node.accept(this, arg);
		return null;
	}

	// lang24.data.ast.tree.defn:

	@Override
	public default Result visit(AstTypDefn typDefn, Argument arg) {
		typDefn.type.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstVarDefn varDefn, Argument arg) {
		varDefn.type.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstFunDefn funDefn, Argument arg) {
		if (funDefn.pars != null)
			funDefn.pars.accept(this, arg);
		if (funDefn.stmt != null)
			funDefn.stmt.accept(this, arg);
		if (funDefn.defns != null)
			funDefn.defns.accept(this, arg);
		funDefn.type.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstFunDefn.AstRefParDefn refParDefn, Argument arg) {
		refParDefn.type.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstFunDefn.AstValParDefn valParDefn, Argument arg) {
		valParDefn.type.accept(this, arg);
		return null;
	}

	// lang24.data.ast.tree.expr:

	@Override
	public default Result visit(AstArrExpr arrExpr, Argument arg) {
		arrExpr.arr.accept(this, arg);
		arrExpr.idx.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstAtomExpr atomExpr, Argument arg) {
		return null;
	}

	@Override
	public default Result visit(AstBinExpr binExpr, Argument arg) {
		binExpr.fstExpr.accept(this, arg);
		binExpr.sndExpr.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstCallExpr callExpr, Argument arg) {
		if (callExpr.args != null)
			callExpr.args.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstCastExpr castExpr, Argument arg) {
		castExpr.type.accept(this, arg);
		castExpr.expr.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstCmpExpr cmpExpr, Argument arg) {
		cmpExpr.expr.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstNameExpr nameExpr, Argument arg) {
		return null;
	}

	@Override
	public default Result visit(AstPfxExpr pfxExpr, Argument arg) {
		pfxExpr.expr.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstSfxExpr sfxExpr, Argument arg) {
		sfxExpr.expr.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstSizeofExpr sizeofExpr, Argument arg) {
		sizeofExpr.type.accept(this, arg);
		return null;
	}

	// lang24.data.ast.tree.stmt:

	@Override
	public default Result visit(AstAssignStmt assignStmt, Argument arg) {
		assignStmt.dst.accept(this, arg);
		assignStmt.src.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstBlockStmt blockStmt, Argument arg) {
		if (blockStmt.stmts != null)
			blockStmt.stmts.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstExprStmt exprStmt, Argument arg) {
		exprStmt.expr.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstIfStmt ifStmt, Argument arg) {
		ifStmt.cond.accept(this, arg);
		ifStmt.thenStmt.accept(this, arg);
		if (ifStmt.elseStmt != null)
			ifStmt.elseStmt.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstReturnStmt retStmt, Argument arg) {
		retStmt.expr.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstWhileStmt whileStmt, Argument arg) {
		whileStmt.cond.accept(this, arg);
		whileStmt.stmt.accept(this, arg);
		return null;
	}

	// lang24.data.ast.tree.type:

	@Override
	public default Result visit(AstArrType arrType, Argument arg) {
		arrType.elemType.accept(this, arg);
		arrType.size.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstAtomType atomType, Argument arg) {
		return null;
	}

	@Override
	public default Result visit(AstNameType nameType, Argument arg) {
		return null;
	}

	@Override
	public default Result visit(AstPtrType ptrType, Argument arg) {
		ptrType.baseType.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstStrType strType, Argument arg) {
		strType.cmps.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstUniType uniType, Argument arg) {
		uniType.cmps.accept(this, arg);
		return null;
	}

	@Override
	public default Result visit(AstRecType.AstCmpDefn cmpDefn, Argument arg) {
		cmpDefn.type.accept(this, arg);
		return null;
	}
*/
