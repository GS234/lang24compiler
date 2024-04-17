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
 * Scope definiton adder.
 * 
 * Adds all definitions in the current scope to symbol table
 */
//string: pass name to children (mainly for record types)
public class NameChecker implements AstFullVisitor<Object, String> {

	
	protected SymbTable symbTable;
	public NameChecker(SymbTable symbTable) {
        this.symbTable = symbTable;
    }

	protected void ins(String name, AstDefn defn) throws Report.Error{
		try{
			symbTable.ins(name, defn);
		}
		catch(SymbTable.CannotInsNameException e){
			// System.out.printf("Napaka: ne morem dodati %s, ker ze obstaja v tren. scope-u.\n", name);
			throw new Report.Error("seman ) ["+defn.location()+"]: Duplicate name found in function parameters: '"+name+"'.");
		}
	}

	protected AstDefn fnd(String name, Location l) throws Report.Error{
		try{
			return symbTable.fnd(name);
		}
		catch(SymbTable.CannotFndNameException e){
			throw new Report.Error("seman ) ["+l+"]: Name '"+name+"' does not exist.");
		}
	}

	//find in specified namespace (symbol table) - for record types
	protected AstDefn fnd(String name, Location l, SymbTable symbTable) {//throws Report.Error{
		try{
			return symbTable.fnd(name);
		}
		catch(SymbTable.CannotFndNameException e){
			//throw new Report.Error("seman ) ["+l+"]: Name '"+name+"' does not exist.");
			System.out.println("seman ) ["+l+"]: Name '"+name+"' does not exist.");
		}
		return null;
	}


	//name type: preveri, ali obstaja
	@Override
	public Object visit(AstNameType nameType, String arg) {
		String name = nameType.name;
		// System.out.printf("[?] %s\n", name);
		
		AstDefn defnNode = this.fnd(name, nameType.location());
		if(defnNode != null) SemAn.definedAt.put(nameType, defnNode);
		
		return null;
	}

	//name expression
	@Override
	public Object visit(AstNameExpr nameExpr, String arg) {
		String name = nameExpr.name;
		// System.out.printf("[?] %s\n", name);
		
		AstDefn defnNode = this.fnd(name, nameExpr.location());
		if(defnNode != null) SemAn.definedAt.put(nameExpr, defnNode);
		
		return null;
	}

	//function checker:
	@Override
	public Object visit(AstFunDefn funDefn, String arg) {
		String name = funDefn.name;
		// System.out.printf("[params scope begin]{ \n");
		this.symbTable.newScope();
		if (funDefn.pars != null)
			//preveri tipe parametrov (ali so v scopu)
			//get: tipi parametrov: se preverijo, ali obstajajo v trenutnem scope-u (to je brez veze, itak je vidno na ven, lahko preveri kukr hoce)	
			//put za imena, get za tipe (to je avtomaticno narejeno s strukturo)
			funDefn.pars.accept(this, arg);
		// System.out.printf("[function scope begin]{ \n");
		this.symbTable.newScope();
		if (funDefn.defns != null)
			//dodaj definicije v scope
			funDefn.defns.accept(new ScopeDefnAdder(this.symbTable), arg);
			//preveri imena!
			funDefn.defns.accept(this, arg);
		if (funDefn.stmt != null)
			funDefn.stmt.accept(this, arg);
		// System.out.printf("} [function scope end]\n");
		this.symbTable.oldScope();
		// System.out.printf("} [params scope end]\n");
		this.symbTable.oldScope();
		funDefn.type.accept(this, arg);
		return null;
	}

	@Override
	public Object visit(AstFunDefn.AstRefParDefn refParDefn, String arg) {
		String name = refParDefn.name;
		this.ins(name, refParDefn);
		// System.out.printf("[+][RefPD] %s (%s)\n", name, refParDefn.location());
		refParDefn.type.accept(this, arg);
		return null;
	}

	@Override
	public Object visit(AstFunDefn.AstValParDefn valParDefn, String arg) {
		String name = valParDefn.name;
		this.ins(name, valParDefn);
		// System.out.printf("[+][ValPD] %s (%s)\n", name, valParDefn.location());
		valParDefn.type.accept(this, arg);
		return null;
	}

	@Override
	public Object visit(AstCallExpr callExpr, String arg) {
		String name = callExpr.name;
		// System.out.printf("[?] %s\n", name);
		
		AstDefn defnNode = this.fnd(name, callExpr.location());
		if(defnNode != null) SemAn.definedAt.put(callExpr, defnNode);
		
		if (callExpr.args != null)
			callExpr.args.accept(this, arg);
		return null;
	}

	//block: (knjigovodstvo: treba je ustrezno povecati/zmanjsati current block v tabeli) (new scope)
	@Override
	public Object visit(AstBlockStmt blockStmt, String arg) {
		// System.out.printf("[new block scope] {\n");
		this.symbTable.newScope();
		if (blockStmt.stmts != null){
			blockStmt.stmts.accept(this, arg);
		}
		// System.out.printf("} [end block scope]\n");
		this.symbTable.oldScope();
		return null;
	}

	// struct/union visit:
	@Override
	public Object visit(AstStrType strType, String arg) {
		RecDefnAdder recDefnVisitor = new RecDefnAdder(this.symbTable); //rec definition visitor
		// System.out.println("[!] [AstStrType] dodajam namespace v record type (struct)");
		this.symbTable.newScope();
		strType.cmps.accept(recDefnVisitor, arg);
		
		RecNameChecker recVisitor = new RecNameChecker(this.symbTable, recDefnVisitor.getLocalSymbTable()); //rec visitor
		strType.cmps.accept(recVisitor, arg);

		this.symbTable.oldScope();
		strType.symbTable = recVisitor.getLocalSymbTable(); //nastavi namespace v record type-u
		return null;
	}

	@Override
	public Object visit(AstUniType uniType, String arg) {
		// System.out.println("[!] [AstStrType] dodajam namespace v record type (struct)");
		RecDefnAdder recDefnVisitor = new RecDefnAdder(this.symbTable); //rec definition visitor
		this.symbTable.newScope();
		uniType.cmps.accept(recDefnVisitor, arg);
		
		RecNameChecker recVisitor = new RecNameChecker(this.symbTable, recDefnVisitor.getLocalSymbTable()); //rec visitor
		uniType.cmps.accept(recVisitor, arg);
		
		this.symbTable.oldScope();
		uniType.symbTable = recVisitor.getLocalSymbTable(); //nastavi namespace v record type-u
		return null;
	}
	//component access
	
	//check if component exists: (samo za test tuki, v tej fazi to se ni vredu preverjat, treba je pol) !!!!
	
	
	// //poisci ime komponente v novem namespace-u (izraza component)
	// @Override
	// public Object visit(AstCmpExpr cmpExpr, String arg) {
	// 	//go to previous level, get it's namespace
	// 	// System.out.printf("[AstCmpExpr] entered %s\n", cmpExpr.name);
	// 	SymbTable namespace = (SymbTable) cmpExpr.expr.accept(this, arg);
	// 	// System.out.printf("[AstCmpExpr] returned from expr\n");
	// 	//get name from namespace:
	// 	if(namespace == null){
	// 		namespace = this.symbTable; //use global if null
	// 	}
	// 	//change namespace
	// 	//ce imamo opravka z imeni
	// 	AstExpr prev = cmpExpr.expr;
	// 	// System.out.printf("     [!] prejsni node je: %s\n", prev);

	// 	String name = null;
	// 	if(prev instanceof AstNameExpr) name = ((AstNameExpr)prev).name;
	// 	if(prev instanceof AstCmpExpr) name = ((AstCmpExpr) prev).name;
	// 	// System.out.println(name);
	// 	// System.out.println(this.fnd(name, new Location(1,1), namespace));
	// 	if(name != null){
	// 		AstDefn astDefn = this.fnd(name, cmpExpr.expr.location(), namespace);
	// 		if(astDefn instanceof AstRecType.AstCmpDefn){
	// 			//poisci typedef v trenutnem namespace-u:
	// 			AstDefn typeDefn = SemAn.definedAt.get(astDefn.type);
	// 			if(typeDefn == null){ //ni definicije, tip je ze kar tuki
	// 				AstRecType typ = (AstRecType) ((AstRecType.AstCmpDefn) astDefn).type;
	// 				namespace = typ.symbTable;
	// 				AstDefn d_name = this.fnd(cmpExpr.name, cmpExpr.location(), namespace);
	// 				SemAn.definedAt.put(cmpExpr, d_name);
	// 				return namespace;
	// 			}
	// 			// System.out.println(typeDefn);
	// 			astDefn = typeDefn;
	// 		}
	// 		// System.out.printf("[AstCmpExpr][astDefn] %s\n",astDefn);
	// 		if(astDefn instanceof AstTypDefn){
	// 			AstType typ = ((AstTypDefn) astDefn).type;
	// 			// System.out.printf("[AstCmpExpr][typ] %s\n",typ);
	// 			if(typ instanceof AstRecType){ //ima novi namespace, vrni ga
	// 				namespace = ((AstRecType)typ).symbTable;
	// 				// System.out.printf("namespace od tipa: %s, isci: %s\n", astDefn.name, cmpExpr.name);
	// 				//v tem namespace-u poisci ime:
	// 				AstDefn d_name = this.fnd(cmpExpr.name, cmpExpr.location(), namespace);
	// 				// System.out.printf("Dodaj %s v defn %s\n", cmpExpr.name, d_name);
	// 				SemAn.definedAt.put(cmpExpr, d_name);

	// 				return namespace;
	// 			}

	// 			// System.out.printf("[!] astdefn je: %s, %s, %s\n", astDefn, astDefn.name,astDefn.type);
	// 		}
	// 		// System.out.println(cmpExpr.expr.name);
	// 		// astDefn = this.fnd(cmpExpr.name, cmpExpr.expr.location(), namespace);
	// 		SemAn.definedAt.put(cmpExpr, astDefn);
	// 	}
	// 	return namespace;
	// }

}

//razred, ki poleg svoje simbolne tabele gradi se eno (za record type)
class RecNameChecker extends NameChecker{
	private SymbTable localSymbTable;
	public RecNameChecker(SymbTable symbTable){
		super(symbTable);
		this.localSymbTable = new SymbTable();
	}

	public RecNameChecker(SymbTable symbTable, SymbTable localSymbTable){
		super(symbTable);
		this.localSymbTable = localSymbTable;
	}

	public SymbTable getLocalSymbTable(){
		return this.localSymbTable;
	}

	@Override
	protected void ins(String name, AstDefn defn) throws Report.Error{
		try{
			this.symbTable.ins(name, defn);
			this.localSymbTable.ins(name, defn);
			// System.out.printf("[RecNameChecker - ins] dodajam: %s\n", name);
		}
		catch(SymbTable.CannotInsNameException e){
			throw new Report.Error("seman ) ["+defn.location()+"]: Duplicate name found in function parameters: '"+name+"'.");
		}
	}

	@Override
	protected AstDefn fnd(String name, Location l) throws Report.Error{
		try{
			return symbTable.fnd(name);
		}
		catch(SymbTable.CannotFndNameException e){
			throw new Report.Error("seman ) ["+l+"]: Name '"+name+"' does not exist.");
		}
	}

	//struct, union namespaces:
	// @Override
	// public Object visit(AstStrType strType, String arg) {		
	// 	//BEFORE
	// 	this.symbTable.newScope();
	// 	System.out.println("novi scope");
	// 	strType.cmps.accept(this, arg); //check if everything is ok
	// 	//AFTER
	// 	//recover current namespace definedAt, symbTable:
	// 	System.out.println("stari scope");
	// 	this.symbTable.oldScope();
		
	// 	return null;
	// }

	// @Override
	// public Object visit(AstUniType uniType, String arg) {
	// 	//save current namespace definedAt, symbTable:
	// 	//BEFORE
	// 	// this.symbTable = new SymbTable(); //new SymbTable for new namespace
	// 	this.symbTable.newScope();
	// 	System.out.println("novi scope");
	// 	uniType.cmps.accept(this, arg);
	// 	//AFTER
	// 	//recover current namespace definedAt, symbTable:
	// 	System.out.println("stari scope");
	// 	this.symbTable.oldScope();
	// 	return null;
	// }

	//ni dovoljeno:
	//funkcije:
	@Override
	public Object visit(AstFunDefn funDefn, String arg) {
		return null;
	}

	//block: (to je ubistvu nov struct/union)
	@Override
	public Object visit(AstBlockStmt blockStmt, String arg) {
		return null;
	}
}

