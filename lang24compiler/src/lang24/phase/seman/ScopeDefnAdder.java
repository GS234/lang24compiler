package lang24.phase.seman;

import java.util.*;
import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.visitor.*;

/**
 * Scope definiton adder.
 * 
 * Adds all definitions in the current scope to symbol table
 */
//string: pass name to children (mainly for record types)
public class ScopeDefnAdder implements AstVisitor<Object, Object> {

	
	protected SymbTable symbTable;
	public ScopeDefnAdder(SymbTable symbTable) {
        this.symbTable = symbTable;
    }

	protected void ins(String name, AstDefn defn) throws Report.Error{
		try{
			this.symbTable.ins(name, defn);
		}
		catch(SymbTable.CannotInsNameException e){
			// System.out.printf("Napaka: ne morem dodati %s, ker ze obstaja v tren. scope-u.\n", name);
			throw new Report.Error("seman ) ["+defn.location()+"]: Duplicate name found in scope: '"+name+"'.");
		}
	}

	@Override
	public Object visit(AstNodes<? extends AstNode> nodes, Object arg) {
		// System.out.println("[definitions]{");
		for (final AstNode node : nodes)
			node.accept(this, arg);
		// System.out.println("} [end definitions]");
		return null;
	}

	
	@Override
	public Object visit(AstTypDefn typDefn, Object arg) {
		String name = typDefn.name;
		this.ins(name, typDefn);
		// System.out.printf("[ScopeDefnAdder][+][TD] %s (%s)\n", name, typDefn.location());
		return null;
	}

	@Override
	public Object visit(AstVarDefn varDefn, Object arg) {
		String name = varDefn.name;
		this.ins(name, varDefn);
		// System.out.printf("[ScopeDefnAdder][+][VD] %s (%s)\n", name, varDefn.location());
		return null;
	}

	@Override
	public Object visit(AstFunDefn funDefn, Object arg) {
		String name = funDefn.name;
		this.ins(name, funDefn);
		// System.out.printf("[ScopeDefnAdder][+][FD] %s (%s)\n", name, funDefn.location());
		return null;
	}
}



class RecDefnAdder extends ScopeDefnAdder{

	private SymbTable localSymbTable; //lokalna simbolna tabela (string -> node)
	public RecDefnAdder(SymbTable symbTable){
		super(symbTable);
		this.localSymbTable = new SymbTable();
	}

	public RecDefnAdder(SymbTable symbTable, SymbTable localSymbTable){
		super(symbTable);
		this.localSymbTable = localSymbTable;
	}

	public SymbTable getLocalSymbTable(){
		return this.localSymbTable;
	}

	@Override
	protected void ins(String name, AstDefn defn) throws Report.Error{
		try{
			// System.out.printf("[recdefnadder] [ins] dodajam %s\n", name);
			this.symbTable.ins(name, defn);
			this.localSymbTable.ins(name, defn);
		}
		catch(SymbTable.CannotInsNameException e){
			throw new Report.Error("seman ) ["+defn.location()+"]: Duplicate name found in scope: '"+name+"'.");
		}
	}




	@Override
	public Object visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
		String name = cmpDefn.name;
		this.ins(name, cmpDefn);
		return null;
	}
}


