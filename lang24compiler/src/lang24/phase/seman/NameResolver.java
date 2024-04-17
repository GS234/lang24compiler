package lang24.phase.seman;

import java.util.*;
import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.visitor.*;
import lang24.data.ast.attribute.*;

/**
 * Name resolver.
 * 
 * The name resolver connects each node of a abstract syntax tree where a name
 * is used with the node where it is defined. The only exceptions are struct and
 * union component names which are connected with their definitions by type
 * resolver. The results of the name resolver are stored in
 * {@link lang24.phase.seman.SemAn#definedAt}.
 */
//string: pass name to children (mainly for record types)
public class NameResolver implements AstFullVisitor<Object, Object> {
	/** The symbol table. */
	private SymbTable symbTable = new SymbTable();


	/** Constructs a new name resolver. */
	public NameResolver() {
	}

	@Override
	public Object visit(AstNodes<? extends AstNode> nodes, Object arg) {
		//1. obhod: dodaj definicije globalnega scope-a notr
		// System.out.println("[global scope definitions]{");
			// nodes.accept(new ScopeDefnAdder(this.symbTable), null); //najprej dodaj definicije
		nodes.accept(new ScopeDefnAdder(this.symbTable), null); //najprej dodaj definicije
		// System.out.println("} [end global scope definitions]");
		//2. obhod: preveri, ce obstajajo uporabljene definicije
		for (final AstNode node : nodes)
			node.accept(new NameChecker(this.symbTable), null);
		return null;
	}
}

