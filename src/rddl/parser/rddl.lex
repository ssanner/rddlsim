/** RDDL Parser: Definitive Lexical Patterns for Tokens (for use with JLex)  
 * 
 *  @author Scott Sanner (ssanner@gmail.com)
 */

package rddl.parser;

import java_cup.runtime.Symbol;

%%
%unicode
%char
%line
%cup
%type Symbol
%implements java_cup.runtime.Scanner
%function next_token
%eofval{  
	return new Symbol(sym.EOF, "[End of file reached]"); 
%eofval}
%{
public int yyline() { return yyline; } 
%}
ALPHA=[A-Za-z]
DIGIT=[0-9]
WHITE_SPACE_CHAR=[\r\n\ \t\b\012]
%%
"//"[^\r\n]* { /* ignore comments */ }
"domain" { return new Symbol(sym.DOMAIN, yytext()); }
"instance" { return new Symbol(sym.INSTANCE, yytext()); }
"horizon" { return new Symbol(sym.HORIZON, yytext()); }
"discount" { return new Symbol(sym.DISCOUNT, yytext()); }
"objects" { return new Symbol(sym.OBJECTS, yytext()); }
"init-state" { return new Symbol(sym.INIT_STATE, yytext()); }
"requirements" { return new Symbol(sym.REQUIREMENTS, yytext()); }
"state-action-constraints" { return new Symbol(sym.STATE_ACTION_CONSTRAINTS, yytext()); }
"action-preconditions" { return new Symbol(sym.ACTION_PRECONDITIONS, yytext()); }
"state-invariants" { return new Symbol(sym.STATE_INVARIANTS, yytext()); }
"types" { return new Symbol(sym.TYPES, yytext()); }
"object" { return new Symbol(sym.OBJECT, yytext()); }
"bool" { return new Symbol(sym.BOOL, yytext()); }
"int" { return new Symbol(sym.INT, yytext()); }
"real" { return new Symbol(sym.REAL, yytext()); }
"neg-inf" { return new Symbol(sym.NEG_INF, yytext()); }
"pos-inf" { return new Symbol(sym.POS_INF, yytext()); }
"pvariables" { return new Symbol(sym.PVARIABLES, yytext()); }
"non-fluent" { return new Symbol(sym.NON_FLUENT, yytext()); }
"non-fluents" { return new Symbol(sym.NON_FLUENTS, yytext()); }
"state-fluent" { return new Symbol(sym.STATE, yytext()); }
"interm-fluent" { return new Symbol(sym.INTERMEDIATE, yytext()); }
"derived-fluent" { return new Symbol(sym.DERIVED_FLUENT, yytext()); }
"observ-fluent" { return new Symbol(sym.OBSERVATION, yytext()); }
"action-fluent" { return new Symbol(sym.ACTION, yytext()); }
"level" { return new Symbol(sym.LEVEL, yytext()); }
"default" { return new Symbol(sym.DEFAULT, yytext()); }
"max-nondef-actions" { return new Symbol(sym.MAX_NONDEF_ACTIONS, yytext()); }
"terminate-when" { return new Symbol(sym.TERMINATE_WHEN, yytext()); }
"terminal" { return new Symbol(sym.TERMINAL, yytext()); }
"cpfs" { return new Symbol(sym.CPFS, yytext()); }
"cdfs" { return new Symbol(sym.CDFS, yytext()); }
"reward" { return new Symbol(sym.REWARD, yytext()); }
"forall" { return new Symbol(sym.FORALL, yytext()); }
"exists" { return new Symbol(sym.EXISTS, yytext()); }
"true" { return new Symbol(sym.TRUE, yytext()); }
"false" { return new Symbol(sym.FALSE, yytext()); }
"if" { return new Symbol(sym.IF, yytext()); }
"then" { return new Symbol(sym.THEN, yytext()); }
"else" { return new Symbol(sym.ELSE, yytext()); }
"switch" { return new Symbol(sym.SWITCH, yytext()); }
"case" { return new Symbol(sym.CASE, yytext()); }
"otherwise" { return new Symbol(sym.OTHERWISE, yytext()); }
"KronDelta" { return new Symbol(sym.KRON_DELTA, yytext()); }
"DiracDelta" { return new Symbol(sym.DIRAC_DELTA, yytext()); }
"Uniform" { return new Symbol(sym.UNIFORM, yytext()); }
"Bernoulli" { return new Symbol(sym.BERNOULLI, yytext()); }
"Discrete" { return new Symbol(sym.DISCRETE, yytext()); }
"Normal" { return new Symbol(sym.NORMAL, yytext()); }
"Poisson" { return new Symbol(sym.POISSON, yytext()); }
"Exponential" { return new Symbol(sym.EXPONENTIAL, yytext()); }
"Weibull" { return new Symbol(sym.WEIBULL, yytext()); }
"Gamma" { return new Symbol(sym.GAMMA, yytext()); }
"Multinomial" { return new Symbol(sym.MULTINOMIAL, yytext()); }
"Dirichlet" { return new Symbol(sym.DIRICHLET, yytext()); }
"^" { return new Symbol(sym.AND, yytext()); }
"|" { return new Symbol(sym.OR, yytext()); }
"~" { return new Symbol(sym.NOT, yytext()); }
"+" { return new Symbol(sym.PLUS, yytext()); }
"*" { return new Symbol(sym.TIMES, yytext()); }
"(" { return new Symbol(sym.LPAREN, yytext()); }
")" { return new Symbol(sym.RPAREN, yytext()); }
"{" { return new Symbol(sym.LCURLY, yytext()); }
"}" { return new Symbol(sym.RCURLY, yytext()); }
"." { return new Symbol(sym.DOT, yytext()); }
"," { return new Symbol(sym.COMMA, yytext()); }
"_" { return new Symbol(sym.UNDERSCORE, yytext()); }
"[" { return new Symbol(sym.LBRACK, yytext()); }
"]" { return new Symbol(sym.RBRACK, yytext()); }
"=>" { return new Symbol(sym.IMPLY, yytext()); }
"<=>" { return new Symbol(sym.EQUIV, yytext()); }
"~=" { return new Symbol(sym.NEQ, yytext()); }
"<=" { return new Symbol(sym.LESSEQ, yytext()); }
"<" { return new Symbol(sym.LESS, yytext()); }
">=" { return new Symbol(sym.GREATEREQ, yytext()); }
">" { return new Symbol(sym.GREATER, yytext()); }
"=" { return new Symbol(sym.ASSIGN_EQUAL, yytext()); }
"==" { return new Symbol(sym.COMP_EQUAL, yytext()); }
"/" { return new Symbol(sym.DIV, yytext()); }
"-" { return new Symbol(sym.MINUS, yytext()); }
":" { return new Symbol(sym.COLON, yytext()); }
";" { return new Symbol(sym.SEMI, yytext()); }
"$" { return new Symbol(sym.DOLLAR_SIGN, yytext()); }
"?" { return new Symbol(sym.QUESTION, yytext()); }
"&" { return new Symbol(sym.AMPERSAND, yytext()); }

({ALPHA})(({ALPHA}|{DIGIT}|-|_)*({ALPHA}|{DIGIT}))?("'")? { return new Symbol(sym.IDENT, yytext()); }
("?")({ALPHA}|{DIGIT}|-|_)*({ALPHA}|{DIGIT}) { return new Symbol(sym.VAR, yytext()); }
("@")({ALPHA}|{DIGIT}|-|_)*({ALPHA}|{DIGIT}) { return new Symbol(sym.ENUM_VAL, yytext()); }
{DIGIT}*"."{DIGIT}+ { return new Symbol(sym.DOUBLE, new Double(yytext())); }
{DIGIT}+ { return new Symbol(sym.INTEGER, new Integer(yytext())); }
{WHITE_SPACE_CHAR}+ { /* ignore white space. */ }

. { System.err.println("Illegal character: "+yytext()+" at line "+(yyline()+1)); if (yytext().equals("'")) System.err.println("Note that ' comes before a parameter list as in var'(?x,?y)"); System.exit(1); }
