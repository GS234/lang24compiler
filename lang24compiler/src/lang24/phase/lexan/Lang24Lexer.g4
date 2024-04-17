lexer grammar Lang24Lexer;

// " = ...!"#...
// ' = ...&'(...

@header {
	package lang24.phase.lexan;
	import lang24.common.report.*;
	import lang24.data.token.*;
}

@members {
    @Override
	public LocLogToken nextToken() {
		return (LocLogToken) super.nextToken();
	}
	
	public int tabW = 8; //tab width
	
	public void lexErr(Lexer l, final String message) throws Report.Error{
		int ln = l.getLine();
		int col = l.getCharPositionInLine();
		Location loc = new Location(ln, col);
		throw new Report.Error("lexer ) ["+loc+"]: " + message);
	}
}

//literals:
//NUMERIC 	: [+-]?[0-9]+ ; //numeric with sign, sign is detected independently
NUMERIC 	: [0-9]+ ;
CHARACTER	: '\''([ -&(-[\]-~] | '\\'[0-9A-F][0-9A-F] | '\\'[n\\'])?'\'';
STRING		:  '"'([ -!#-[\]-~] | '\\'[0-9A-F][0-9A-F] | '\\'[n\\"])*'"' ;

//WHITESPACES: //SKIP THAT
NL	: '\n' -> skip;
CR	: '\r' -> skip;		//windows specific
TC	: '\t'
	{
	{
		//calculate new position
		int line = this.getLine();
		int charPos = this.getCharPositionInLine();
		int nWhitespaces = this.tabW - (charPos % this.tabW);
		int skipTo = charPos + nWhitespaces; //
		//System.out.printf("[%3d] tab position: %d, next char position: %d\n", line, charPos, skipTo+1);
		this.setCharPositionInLine(skipTo);
	}
	case -1:
	} -> skip; 	
WS	: ' '+ -> skip;

//comments:
COMMENT	: '#'[ -~]*'\n' -> skip;

//symbols:
LB	: '(';
RB	: ')';
LCB	: '{';
RCB	: '}';
LSB	: '[';
RSB	: ']';
DOT	: '.';
COM	: ',';
COL	: ':';
SCOL	: ';';

EQ	: '==';
NE	: '!=';
LT	: '<';
GT	: '>';
LE	: '<=';
GE	: '>=';

PLUS	: '+'; //either sign or multiplication operator
MINUS	: '-'; //same as plus, but different

MUL	: '*';
DIV	: '/';
REM	: '%';
POW	: '^';


ASSIGN_OP : '=';

//constants:
TRUE: 'true';
FALSE: 'false';

//keywords:
AND	: 'and';
NOT	: 'not';
OR	: 'or';

VOID	: 'void';
NIL	: 'nil';
NONE	: 'none';
BOOL	: 'bool';
CHAR	: 'char';
INT	: 'int';

IF	: 'if';
THEN	: 'then';
ELSE	: 'else';
WHILE	: 'while';

SIZEOF	: 'sizeof';
RETURN	: 'return';


//identifier:
IDENT	: [A-Za-z_][A-Za-z_0-9]*;

//unknown tokens:
UNKNOWN	: 
	'\\'[ -~]* 
	{
	{
		lexErr(this, "Unexpected \\."); //'\\'[0-9A-F][0-9A-F] | '\\'[n\\'])?'\''
	}
	case -1:
	}
	| '\''[ -&(-~]* //missing closing ', could also be problem with character
	{
	{
		lexErr(this, "Error in character literal: missing closing ' or contains problematic character.");
	}
	case -2:
	}
	
	//|
	// '\''[ -&(-[\]-~]*'\\'[ -&(-~]* //illegal  escape sequence
	//{
	//{
	//	lexErr(this, "Unknown escape sequence");
	//}
	//	case -5:
	//}
	
	| '"'[ -!#-~]* //missing closing " or contains problematic character
	{
	{
		lexErr(this, "Error in string literal: missing closing \" or contains problematic character.");
	}
	case -3:
	}
	| ~[ -~] 
	{
	{
		lexErr(this, "Illegal character(s).");
	}
	case -4: //could also be while(true)
	}
	|
	'#'[ -~]*~[ -~]
	{
	{
		lexErr(this, "Illegal character(s) inside comment.");
	}
	case -5:
	}
	;

//whatever is inside {} is just replaced in handler function (inside switch(){ case: ... } construct) of generated java class Lang24Lexer.java


