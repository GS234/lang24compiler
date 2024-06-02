// Generated from ../synan/Lang24Parser.g4 by ANTLR 4.13.1

	package lang24.phase.synan;
	import java.util.*;
	import lang24.common.report.*;
	import lang24.data.token.*;
	import lang24.data.ast.tree.*;
	import lang24.data.ast.tree.type.*;
	import lang24.data.ast.tree.expr.*;
	import lang24.data.ast.tree.defn.*;
	import lang24.data.ast.tree.stmt.*;
	import lang24.phase.lexan.*;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link Lang24Parser}.
 */
public interface Lang24ParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#source}.
	 * @param ctx the parse tree
	 */
	void enterSource(Lang24Parser.SourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#source}.
	 * @param ctx the parse tree
	 */
	void exitSource(Lang24Parser.SourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#def_begin}.
	 * @param ctx the parse tree
	 */
	void enterDef_begin(Lang24Parser.Def_beginContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#def_begin}.
	 * @param ctx the parse tree
	 */
	void exitDef_begin(Lang24Parser.Def_beginContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#def_end}.
	 * @param ctx the parse tree
	 */
	void enterDef_end(Lang24Parser.Def_endContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#def_end}.
	 * @param ctx the parse tree
	 */
	void exitDef_end(Lang24Parser.Def_endContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#def_next}.
	 * @param ctx the parse tree
	 */
	void enterDef_next(Lang24Parser.Def_nextContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#def_next}.
	 * @param ctx the parse tree
	 */
	void exitDef_next(Lang24Parser.Def_nextContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#params}.
	 * @param ctx the parse tree
	 */
	void enterParams(Lang24Parser.ParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#params}.
	 * @param ctx the parse tree
	 */
	void exitParams(Lang24Parser.ParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#params_next}.
	 * @param ctx the parse tree
	 */
	void enterParams_next(Lang24Parser.Params_nextContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#params_next}.
	 * @param ctx the parse tree
	 */
	void exitParams_next(Lang24Parser.Params_nextContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(Lang24Parser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(Lang24Parser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#expression2}.
	 * @param ctx the parse tree
	 */
	void enterExpression2(Lang24Parser.Expression2Context ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#expression2}.
	 * @param ctx the parse tree
	 */
	void exitExpression2(Lang24Parser.Expression2Context ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#stmt2}.
	 * @param ctx the parse tree
	 */
	void enterStmt2(Lang24Parser.Stmt2Context ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#stmt2}.
	 * @param ctx the parse tree
	 */
	void exitStmt2(Lang24Parser.Stmt2Context ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#block_begin}.
	 * @param ctx the parse tree
	 */
	void enterBlock_begin(Lang24Parser.Block_beginContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#block_begin}.
	 * @param ctx the parse tree
	 */
	void exitBlock_begin(Lang24Parser.Block_beginContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#block_end}.
	 * @param ctx the parse tree
	 */
	void enterBlock_end(Lang24Parser.Block_endContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#block_end}.
	 * @param ctx the parse tree
	 */
	void exitBlock_end(Lang24Parser.Block_endContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(Lang24Parser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(Lang24Parser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#components}.
	 * @param ctx the parse tree
	 */
	void enterComponents(Lang24Parser.ComponentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#components}.
	 * @param ctx the parse tree
	 */
	void exitComponents(Lang24Parser.ComponentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#components_next}.
	 * @param ctx the parse tree
	 */
	void enterComponents_next(Lang24Parser.Components_nextContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#components_next}.
	 * @param ctx the parse tree
	 */
	void exitComponents_next(Lang24Parser.Components_nextContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(Lang24Parser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(Lang24Parser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#a}.
	 * @param ctx the parse tree
	 */
	void enterA(Lang24Parser.AContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#a}.
	 * @param ctx the parse tree
	 */
	void exitA(Lang24Parser.AContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#b}.
	 * @param ctx the parse tree
	 */
	void enterB(Lang24Parser.BContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#b}.
	 * @param ctx the parse tree
	 */
	void exitB(Lang24Parser.BContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#c}.
	 * @param ctx the parse tree
	 */
	void enterC(Lang24Parser.CContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#c}.
	 * @param ctx the parse tree
	 */
	void exitC(Lang24Parser.CContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#d}.
	 * @param ctx the parse tree
	 */
	void enterD(Lang24Parser.DContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#d}.
	 * @param ctx the parse tree
	 */
	void exitD(Lang24Parser.DContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#e}.
	 * @param ctx the parse tree
	 */
	void enterE(Lang24Parser.EContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#e}.
	 * @param ctx the parse tree
	 */
	void exitE(Lang24Parser.EContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#f}.
	 * @param ctx the parse tree
	 */
	void enterF(Lang24Parser.FContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#f}.
	 * @param ctx the parse tree
	 */
	void exitF(Lang24Parser.FContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#g}.
	 * @param ctx the parse tree
	 */
	void enterG(Lang24Parser.GContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#g}.
	 * @param ctx the parse tree
	 */
	void exitG(Lang24Parser.GContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#h}.
	 * @param ctx the parse tree
	 */
	void enterH(Lang24Parser.HContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#h}.
	 * @param ctx the parse tree
	 */
	void exitH(Lang24Parser.HContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#args}.
	 * @param ctx the parse tree
	 */
	void enterArgs(Lang24Parser.ArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#args}.
	 * @param ctx the parse tree
	 */
	void exitArgs(Lang24Parser.ArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#args_next}.
	 * @param ctx the parse tree
	 */
	void enterArgs_next(Lang24Parser.Args_nextContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#args_next}.
	 * @param ctx the parse tree
	 */
	void exitArgs_next(Lang24Parser.Args_nextContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#voidconst}.
	 * @param ctx the parse tree
	 */
	void enterVoidconst(Lang24Parser.VoidconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#voidconst}.
	 * @param ctx the parse tree
	 */
	void exitVoidconst(Lang24Parser.VoidconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#boolconst}.
	 * @param ctx the parse tree
	 */
	void enterBoolconst(Lang24Parser.BoolconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#boolconst}.
	 * @param ctx the parse tree
	 */
	void exitBoolconst(Lang24Parser.BoolconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#charconst}.
	 * @param ctx the parse tree
	 */
	void enterCharconst(Lang24Parser.CharconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#charconst}.
	 * @param ctx the parse tree
	 */
	void exitCharconst(Lang24Parser.CharconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#intconst}.
	 * @param ctx the parse tree
	 */
	void enterIntconst(Lang24Parser.IntconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#intconst}.
	 * @param ctx the parse tree
	 */
	void exitIntconst(Lang24Parser.IntconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#strconst}.
	 * @param ctx the parse tree
	 */
	void enterStrconst(Lang24Parser.StrconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#strconst}.
	 * @param ctx the parse tree
	 */
	void exitStrconst(Lang24Parser.StrconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#ptrconst}.
	 * @param ctx the parse tree
	 */
	void enterPtrconst(Lang24Parser.PtrconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#ptrconst}.
	 * @param ctx the parse tree
	 */
	void exitPtrconst(Lang24Parser.PtrconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#postfix_op}.
	 * @param ctx the parse tree
	 */
	void enterPostfix_op(Lang24Parser.Postfix_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#postfix_op}.
	 * @param ctx the parse tree
	 */
	void exitPostfix_op(Lang24Parser.Postfix_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#prefix_op}.
	 * @param ctx the parse tree
	 */
	void enterPrefix_op(Lang24Parser.Prefix_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#prefix_op}.
	 * @param ctx the parse tree
	 */
	void exitPrefix_op(Lang24Parser.Prefix_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#mul_op}.
	 * @param ctx the parse tree
	 */
	void enterMul_op(Lang24Parser.Mul_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#mul_op}.
	 * @param ctx the parse tree
	 */
	void exitMul_op(Lang24Parser.Mul_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#add_op}.
	 * @param ctx the parse tree
	 */
	void enterAdd_op(Lang24Parser.Add_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#add_op}.
	 * @param ctx the parse tree
	 */
	void exitAdd_op(Lang24Parser.Add_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#rel_op}.
	 * @param ctx the parse tree
	 */
	void enterRel_op(Lang24Parser.Rel_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#rel_op}.
	 * @param ctx the parse tree
	 */
	void exitRel_op(Lang24Parser.Rel_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#conj}.
	 * @param ctx the parse tree
	 */
	void enterConj(Lang24Parser.ConjContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#conj}.
	 * @param ctx the parse tree
	 */
	void exitConj(Lang24Parser.ConjContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#disj}.
	 * @param ctx the parse tree
	 */
	void enterDisj(Lang24Parser.DisjContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#disj}.
	 * @param ctx the parse tree
	 */
	void exitDisj(Lang24Parser.DisjContext ctx);
}