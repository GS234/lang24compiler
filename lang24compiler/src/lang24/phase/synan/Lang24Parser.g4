parser grammar Lang24Parser;

@header {
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
}

@members {

	private Location loc(Token tok) { return new Location((LocLogToken)tok); }
	private Location loc(Token     tok1, Token     tok2) { return new Location((LocLogToken)tok1, (LocLogToken)tok2); }
	private Location loc(Token     tok1, Locatable loc2) { return new Location((LocLogToken)tok1, loc2); }
	private Location loc(Locatable loc1, Token     tok2) { return new Location(loc1, (LocLogToken)tok2); }
	private Location loc(Locatable loc1, Locatable loc2) { return new Location(loc1, loc2); }

}

options{
    tokenVocab=Lang24Lexer;
}

//source
source returns [AstNode ast] locals [List<AstDefn> definitions = new Vector<AstDefn>()]	
	: def_begin[$definitions] {$ast = new AstNodes($definitions);}
	;

//definition rules
def_begin[List<AstDefn> definitions]
	: IDENT def_end[$definitions, $IDENT]
	;
def_end [List<AstDefn> definitions, Token ident] locals [List<AstDefn> possibleFunctionDefinitions = new Vector<AstDefn>()]
	: ASSIGN_OP type def_next[$definitions] {$definitions.add(0,new AstTypDefn(loc($ident, $type._type.location()), $ident.getText(), $type._type));}
	| COL type def_next[$definitions] {$definitions.add(0,new AstVarDefn(loc($ident, $type._type.location()), $ident.getText(), $type._type));}
	| LB params? RB COL type ( ASSIGN_OP stmt ( LCB def_begin[$possibleFunctionDefinitions] RCB )? )? def_next[$definitions] //function definition
		{
			Location l = loc($ident, $type._type); //set location based on what is present
			AstNodes<AstFunDefn.AstParDefn> parameters = null;
			AstStmt statement = null;
			if(_localctx.params != null) parameters = $params._params; //function parameters are present, use them
			if(_localctx.stmt != null){
				statement = $stmt._stmt; //statement is also there, use it
				l = statement.location();
			}
			if(_localctx.RCB != null) l = loc($ident, $RCB);
			$definitions.add(0,new AstFunDefn(l, $ident.getText(), parameters , $type._type, statement, new AstNodes($possibleFunctionDefinitions)));
		}
	;

def_next[List<AstDefn> definitions]
	: 
	| def_begin[$definitions]
	;

//function parameters (could be call-by-reference (^) or call-by-value)
params returns [AstNodes<AstFunDefn.AstParDefn> _params] locals [List<AstFunDefn.AstParDefn> parameters = new Vector<AstFunDefn.AstParDefn>()]	
	: POW? IDENT COL type params_next[$parameters] //ce je pow, potem je to AstFunDefn.astRefParDefn (call-by-reference), ce pa ni, je pa AstFunDefn.astValParDefn (call-by-value)
		{
			AstFunDefn.AstParDefn p;
			if($POW != null) p = new AstFunDefn.AstRefParDefn(loc($POW, $type._type), $IDENT.getText(), $type._type);
			else p = new AstFunDefn.AstValParDefn(loc($IDENT, $type._type), $IDENT.getText(), $type._type);
			$parameters.add(0, p);
			$_params = new AstNodes($parameters);
		}
	;
params_next[List<AstFunDefn.AstParDefn> parameters]
	:
	| COM POW? IDENT COL type params_next[$parameters]
		{
			AstFunDefn.AstParDefn p;
			if($POW != null) p = new AstFunDefn.AstRefParDefn(loc($POW, $type._type), $IDENT.getText(), $type._type);
			else p = new AstFunDefn.AstValParDefn(loc($IDENT, $type._type), $IDENT.getText(), $type._type);
			$parameters.add(0, p);
		}
	;

//statement (stmt) rules:
stmt returns [AstStmt _stmt]
	: expression SCOL {$_stmt = new AstExprStmt(loc($expression._expression, $SCOL), $expression._expression);} //! locatable
 	| expression ASSIGN_OP expression2 SCOL {$_stmt = new AstAssignStmt(loc($expression._expression, $SCOL), $expression._expression, $expression2._expression);}
	| IF expression THEN stmt (ELSE stmt2)? 
		{
			AstIfStmt s;
			if(_localctx.stmt2 != null){
				s = new AstIfStmt(loc($IF, $stmt._stmt), $expression._expression, $stmt._stmt, $stmt2._stmt);
			}
			else{
				s = new AstIfStmt(loc($IF, $stmt._stmt), $expression._expression, $stmt._stmt, null);
			}
			$_stmt = s;
		}
	| WHILE expression COL stmt {$_stmt = new AstWhileStmt(loc($WHILE, $stmt._stmt), $expression._expression, $stmt._stmt);}
	| RETURN expression SCOL {$_stmt = new AstReturnStmt(loc($RETURN, $SCOL), $expression._expression);}
	| block_begin {$_stmt = $block_begin.stmts;}
	;

//passing rules for duplicate expression/stmt symbols in rules for statements
expression2 returns [AstExpr _expression] //2nd expression
	: expression {$_expression = $expression._expression;}
	;

stmt2 returns [AstStmt _stmt] //2nd statement
	: stmt {$_stmt = $stmt._stmt;}
	;

//statement block
block_begin returns [AstBlockStmt stmts] locals [List<AstStmt> statementList = new Vector<AstStmt>()]
	: LCB stmt block_end[$statementList]
		{
			$statementList.add(0, $stmt._stmt);
			$stmts = new AstBlockStmt(loc($LCB, $block_end._end), $statementList);
		}
	;
block_end [List<AstStmt> statementList] returns [Location _end]
	: stmt block_end[$statementList] {$statementList.add(0, $stmt._stmt); $_end = $block_end._end;}
	| RCB {$_end = loc($RCB);}
	;

//type rules
type returns [AstType _type]
	: VOID {$_type = new AstAtomType(loc($VOID), AstAtomType.Type.VOID);}
	| BOOL {$_type = new AstAtomType(loc($BOOL), AstAtomType.Type.BOOL);}
	| CHAR {$_type = new AstAtomType(loc($CHAR), AstAtomType.Type.CHAR);}
	| INT  {$_type = new AstAtomType(loc($INT),  AstAtomType.Type.INT);}
	| LSB intconst RSB type {$_type = new AstArrType(loc($LSB, $type._type), $type._type, $intconst._intconst);} //new AstAtomExpr(loc($NUMERIC), AstAtomExpr.Type.INT , $NUMERIC.getText())
	| POW type {$_type = new AstPtrType(loc($POW, $type._type), $type._type);}
	| LB components RB {$_type = new AstStrType(loc($LB, $RB), $components._components);} //union type
	| LCB components RCB {$_type = new AstUniType(loc($LCB, $RCB), $components._components);} //struct type
	| IDENT {$_type = new AstNameType(loc($IDENT), $IDENT.getText());}
	;

//components
components returns [AstNodes<AstRecType.AstCmpDefn> _components] locals [List<AstRecType.AstCmpDefn> components_list = new Vector<AstRecType.AstCmpDefn>()]	
	: IDENT COL type components_next[$components_list] {$components_list.add(0,new AstRecType.AstCmpDefn(loc($IDENT, $type._type), $IDENT.getText(),$type._type)); $_components = new AstNodes($components_list);}
	;
components_next [List<AstRecType.AstCmpDefn> components_list] returns [Location _end]
	:
	| COM IDENT COL type components_next[$components_list] 
		{
			$components_list.add(0, new AstRecType.AstCmpDefn(loc($IDENT, $type._type), $IDENT.getText(),$type._type));
			$_end = loc($type._type,$type._type);
		}
	;

//expressions: left associative, hierarchical
expression returns [AstExpr _expression]
	: a {$_expression = $a._a;}
	// | //je tu veljavnu? zatu mam lahku { ;}!
	;

//hierarchy levels of expressions (weakest to strongest (a - h))
a returns [AstExpr _a]
	: a disj b {$_a = new AstBinExpr(loc(_prevctx._a, $b._b), $disj._op, _prevctx._a, $b._b);}
	| b {$_a = $b._b;}
	;
b returns [AstExpr _b]
	: b conj c {$_b = new AstBinExpr(loc(_prevctx._b, $c._c), $conj._op, _prevctx._b, $c._c);}
	| c {$_b = $c._c;}
	;
c returns [AstExpr _c]
	: c rel_op d {$_c = new AstBinExpr(loc(_prevctx._c, $d._d), $rel_op._op, _prevctx._c, $d._d);}
	| d {$_c = $d._d;}
	;
d returns [AstExpr _d]
	: d add_op e {$_d = new AstBinExpr(loc(_prevctx._d, $e._e), $add_op._op, _prevctx._d, $e._e);}
	| e {$_d = $e._e;};
e returns [AstExpr _e]
	: e mul_op f {$_e = new AstBinExpr(loc(_prevctx._e,$f._f), $mul_op._op, _prevctx._e, $f._f);}
	| f {$_e = $f._f;}
	;
f returns [AstExpr _f]
	: prefix_op f {$_f = new AstPfxExpr(loc($prefix_op._prefix_op, $f._f), $prefix_op._op, $f._f);}
	| LT type GT f {$_f = new AstCastExpr(loc($LT), $type._type, $f._f);} //ast type cast expression
	| g {$_f = $g._g;}
	;
g returns [AstExpr _g]
	: g postfix_op {$_g = new AstSfxExpr(loc(_prevctx._g, $postfix_op._postfix_op), $postfix_op._op, _prevctx._g);}
	| g DOT IDENT {$_g = new AstCmpExpr(loc(_prevctx._g, $IDENT), _prevctx._g, $IDENT.getText());} //parameter access (is postfix)
	| g LSB expression RSB {$_g = new AstArrExpr(loc(_prevctx._g, $RSB), _prevctx._g ,$expression._expression);} //array access (is postfix)
	| h {$_g = $h._h;}
	;
h returns [AstExpr _h]
	: voidconst {$_h = $voidconst._voidconst;}
	| charconst {$_h = $charconst._charconst;}
	| strconst  {$_h = $strconst._strconst;}
	| intconst  {$_h = $intconst._intconst;}
	| boolconst {$_h = $boolconst._boolconst;}
	| ptrconst  {$_h = $ptrconst._ptrconst;}
	| LB expression RB {$_h = $expression._expression;} //()
	| SIZEOF LB type RB {$_h = new AstSizeofExpr(loc($SIZEOF, $RB), $type._type);} //sizeof (should be prefix?)
	| IDENT (LB args RB)? //function call, identifier access
		{
			if(_localctx.args != null) $_h = new AstCallExpr(loc($IDENT), $IDENT.getText(), $args._args); //function call
			else $_h = new AstNameExpr(loc($IDENT, $RB), $IDENT.getText()); //identifier access
		} 
	;

//actual function arguments (parameters with actual values)
args returns [AstNodes<AstExpr> _args] locals [List<AstExpr> args_list = new Vector<AstExpr>()]
	: //nothing to be done, _args is already null
	| expression args_next[$args_list] {$args_list.add(0,$expression._expression); $_args = new AstNodes($args_list);}
	;
args_next [List<AstExpr> args_list]
	:
	| COM expression args_next[$args_list] {$args_list.add(0,$expression._expression);}
	;

//constants
voidconst returns [AstAtomExpr _voidconst]
	: NONE {$_voidconst = new AstAtomExpr(loc($NONE), AstAtomExpr.Type.VOID, $NONE.getText());}
	;
boolconst returns [AstAtomExpr _boolconst]
	: TRUE {$_boolconst = new AstAtomExpr(loc($TRUE), AstAtomExpr.Type.BOOL, $TRUE.getText());}
	| FALSE {$_boolconst = new AstAtomExpr(loc($FALSE), AstAtomExpr.Type.BOOL, $FALSE.getText());}
	;
charconst returns [AstAtomExpr _charconst]
	: CHARACTER {$_charconst = new AstAtomExpr(loc($CHARACTER), AstAtomExpr.Type.CHAR, $CHARACTER.getText());}
	;
intconst returns [AstAtomExpr _intconst]
	: NUMERIC {$_intconst = new AstAtomExpr(loc($NUMERIC), AstAtomExpr.Type.INT, $NUMERIC.getText());}
	;
strconst returns [AstAtomExpr _strconst]
	: STRING {$_strconst = new AstAtomExpr(loc($STRING), AstAtomExpr.Type.STR, $STRING.getText());}
	;
ptrconst returns [AstAtomExpr _ptrconst]
	: NIL {$_ptrconst = new AstAtomExpr(loc($NIL), AstAtomExpr.Type.PTR, $NIL.getText());}
	;

//operators
postfix_op returns [Token _postfix_op, AstSfxExpr.Oper _op]
	: POW {$_postfix_op = $POW; $_op = AstSfxExpr.Oper.PTR;}
	;
prefix_op returns [Token _prefix_op, AstPfxExpr.Oper _op] //also sizeof //_op za typecast? !!
	: NOT   {$_prefix_op = $NOT; $_op = AstPfxExpr.Oper.NOT;}
	| PLUS  {$_prefix_op = $PLUS; $_op = AstPfxExpr.Oper.ADD;}
	| MINUS {$_prefix_op = $MINUS; $_op = AstPfxExpr.Oper.SUB;}
	| POW   {$_prefix_op = $POW; $_op = AstPfxExpr.Oper.PTR;}
	;
mul_op returns [Token _mul_op, AstBinExpr.Oper _op]
	: MUL {$_mul_op = $MUL; $_op = AstBinExpr.Oper.MUL;}
	| DIV {$_mul_op = $DIV; $_op = AstBinExpr.Oper.DIV;}
	| REM {$_mul_op = $REM; $_op = AstBinExpr.Oper.MOD;}
	;	
add_op returns [Token _add_op, AstBinExpr.Oper _op]
	: PLUS  {$_add_op = $PLUS; $_op = AstBinExpr.Oper.ADD;}
	| MINUS {$_add_op = $MINUS; $_op = AstBinExpr.Oper.SUB;};
rel_op returns [Token _rel_op, AstBinExpr.Oper _op]
	: EQ {$_rel_op = $EQ; $_op = AstBinExpr.Oper.EQU;}
	| NE {$_rel_op = $NE; $_op = AstBinExpr.Oper.NEQ;}
	| LT {$_rel_op = $LT; $_op = AstBinExpr.Oper.LTH;}
	| GT {$_rel_op = $GT; $_op = AstBinExpr.Oper.GTH;}
	| LE {$_rel_op = $LE; $_op = AstBinExpr.Oper.LEQ;}
	| GE {$_rel_op = $GE; $_op = AstBinExpr.Oper.GEQ;}
	;
conj returns [Token _conj, AstBinExpr.Oper _op]
	: AND {$_conj = $AND; $_op = AstBinExpr.Oper.AND;}
	;
disj returns [Token _disj, AstBinExpr.Oper _op]
	: OR {$_disj = $OR; $_op = AstBinExpr.Oper.OR;}
	;


//additional notes (to self):
//When checking, whether some symbol exists (because it is optional (succeded by ? or *)), _localctx reference is used, because for *some* reason symbol $ does not work.
//When needed, _prevctx is used to reference previous node (in left recursion cases; simply using $ does not work, so workaround (is it?) must be used).
//Every context class has _prevctx and _localctx references, as well as those specified in returns and local blocks (and in the rules (symbols)).
//Tokens (terminals) are instances of Token class, so therefore methods like getText() work.
//Usage of loc-methods is strongly recommended whenever possible.
//Because nodes are added in reverse (from leaves upward), elements must be added to lists at the begining (list.add(0, Object) <- index 0) to preserve order 
// (order might not be preserved in every rule, because I discovered the 'reverse' problem late; if order seems incorrect, that might be the case (fix it!)).
//Return parameter names are mostly started by _, but this is not consistent in every rule (reason: I did not start doing that in the begining). It would, however,
// be extremely desirable if it was consistent (fix it!).
//Hierarchy levels in expression productions are named a through h, with a being weakest level and h strongest. This I will NOT change, because it is kind of logical
// (idea: increasing letters, increasing power).
//Some statement locations might be incorrect, because some productions do not start with terminal and I did not bother to find location of it (should be fixed).
// It therefore shows location of the first terminal of corresponding rule (example: expression SCOL: location of SCOL is used instead). -> fixed (might not be fixed in some places)

