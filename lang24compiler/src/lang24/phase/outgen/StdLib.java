package lang24.phase.outgen;

import java.util.List;

public class StdLib {
    private static final List<String> putchar = List.of(
        // "_putchar\tLDB  $255,SP,#0F",
        "_putchar\tLDO  $255,SP,#8", // load octa, because it is stored as octa
        "\tSTB  $255,SP,8",
        "\tADD  $255,SP,8",  // Save address of the character to print, 8 is to skip static link
        "\tTRAP  0,Fputs,StdOut",  // Do syscall
        "\tPOP  0,0"
    );


    private static final List<String> getchar = List.of(
        "FgetsBuf\tBYTE  0,0",
        "FgetsArgs\tOCTA  FgetsBuf,2",
        "_getchar\tLDA  $255,FgetsArgs",
        "\tTRAP  0,Fgets,StdIn",
        "\tLDA  $255,FgetsBuf",
        "\tSETL  $0,#0",
        "\tSTO  $0,SP,0",
        "\tLDB  $255,$255,0",
        "\tSTB  $255,SP,7",  // Skip 7 bytes as characters are 8 bytes long in our lang
        "\tPOP  0,0"
    );
    private static final List<String> getint = List.of(
        "_getint\tSET  $0,SP",
        "\tSUB  SP,SP,40",
        "\tSTO  FP,SP,#8",
        "\tGET  FP,rJ",
        "\tSTO  FP,SP,#0",
        "\tSET  FP,$0",
        "\tSUB  SP,SP,8",
        
        "\tSETL  $0,#0008",
        "\tNEG  $0,$0",
        "\tADD  $1,$253,$0",
        "\tSETL  $0,#0",
        "\tSTOU  $0,$1,0",
        "\tSETL  $0,#021D",
        "\tSTOU  $0,$254,0",
        "\tPUSHJ  $8,_getchar",
        "\tLDOU  $0,$254,0",
        "\tADD  $1,$0,0",
        "\tSETL  $0,#0010",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tSTOU  $1,$0,0",
        "\tSETL  $0,#0018",
        "\tNEG  $0,$0",
        "\tADD  $1,$253,$0",
        "\tSETL  $0,#0001",
        "\tSTOU  $0,$1,0",
        "\tSETL  $0,#0010",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tLDOU  $1,$0,0",
        "\tSETL  $0,#002D",
        "\tCMP  $0,$1,$0",
        "\tZSZ  $0,$0,1",
        "\tBZ  $0,GIWHL",
        "\tADDU  $0,$0,0",
        "\tSETL  $0,#0018",
        "\tNEG  $0,$0",
        "\tADD  $1,$253,$0",
        "\tSETL  $0,#0001",
        "\tNEG  $0,$0",
        "\tSTOU  $0,$1,0",
        "\tSETL  $0,#021D",
        "\tSTOU  $0,$254,0",
        "\tPUSHJ  $8,_getchar",
        "\tLDOU  $0,$254,0",
        "\tADD  $1,$0,0",
        "\tSETL  $0,#0010",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tSTOU  $1,$0,0",
        "\tJMP  GIWHL",
        "GIWHL\tADDU  $0,$0,0",
        "\tSETL  $0,#0010",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tLDOU  $1,$0,0",
        "\tSETL  $0,#0030",
        "\tCMP  $0,$1,$0",
        "\tZSNN  $1,$0,1",
        "\tSETL  $0,#0010",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tLDOU  $2,$0,0",
        "\tSETL  $0,#0039",
        "\tCMP  $0,$2,$0",
        "\tZSNP  $0,$0,1",
        "\tAND  $0,$1,$0",
        "\tBZ  $0,MULSIGN",
        "\tADDU  $0,$0,0",
        "\tSETL  $0,#0008",
        "\tNEG  $0,$0",
        "\tADD  $2,$253,$0",
        "\tSETL  $0,#0008",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tLDOU  $1,$0,0",
        "\tSETL  $0,#000A",
        "\tMUL  $1,$1,$0",
        "\tSETL  $0,#0010",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tLDOU  $0,$0,0",
        "\tADD  $1,$1,$0",
        "\tSETL  $0,#0030",
        "\tSUB  $0,$1,$0",
        "\tSTOU  $0,$2,0",
        "\tSETL  $0,#021D",
        "\tSTOU  $0,$254,0",
        "\tPUSHJ  $8,_getchar",
        "\tLDOU  $0,$254,0",
        "\tADD  $1,$0,0",
        "\tSETL  $0,#0010",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tSTOU  $1,$0,0",
        "\tJMP  GIWHL",
        "MULSIGN\tADDU  $0,$0,0",
        "\tSETL  $0,#0008",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tLDOU  $1,$0,0",
        "\tSETL  $0,#0018",
        "\tNEG  $0,$0",
        "\tADD  $0,$253,$0",
        "\tLDOU  $0,$0,0",
        "\tMUL  $0,$1,$0",
        "\tADD  $0,$0,0",

        "\tSTO  $0,FP,#0",
        "\tADD  SP,SP,8",
        "\tLDO  $0,SP,#0",
        "\tPUT  rJ,$0",
        "\tLDO  $0,SP,#8",
        "\tSET  SP,FP",
        "\tSET  FP,$0",
        "\tPOP  0,0"
    );
    private static final List<String> _new = List.of(
        "_new\tLDO  $0,SP,8",
        "\tSTOU  HP,SP,0",
        "\tADD  HP,HP,$0",
        "\tPOP  0,0"
    );
    private static final List<String> delete = List.of(
        "_delete\tPOP 0,0"
    );
    private static final List<String> exit = List.of(
        "_exit\tLDO  $0,SP,8",
        "\tTRAP  0,Halt,$0"
    );
    private static final List<String> putint = List.of(
        "_putint\tSET $0,SP",
        "\tSUB SP,SP,16",
        "\tSTO FP,SP,#8",
        "\tGET FP,rJ",
        "\tSTO FP,SP,#0",
        "\tSET FP,$0",
        "\tSUB SP,SP,16",
        
        "\tSETL $0,#0008",
        "\tADD $0,$253,$0",
        "\tLDOU $1,$0,0",
        "\tSETL $0,#0",
        "\tCMP $0,$1,$0",
        "\tZSN $0,$0,1",
        "\tBZ $0,LPI",
        "\tSETL $0,#002D",
        "\tSTOU $0,$254,8",
        "\tSETL $0,#021D",
        "\tSTOU $0,$254,0",
        "\tPUSHJ $8,_putchar",
        "\tLDOU $0,$254,0",
        "\tADD $0,$0,0",
        "\tSETL $0,#0008",
        "\tADD $1,$253,$0",
        "\tSETL $0,#0008",
        "\tADD $0,$253,$0",
        "\tLDOU $0,$0,0",
        "\tNEG $0,$0",
        "\tSTOU $0,$1,0",
        "LPI\tSETL $0,#0008",
        "\tADD $0,$253,$0",
        "\tLDOU $1,$0,0",
        "\tSETL $0,#000A",
        "\tCMP $0,$1,$0",
        "\tZSNN $0,$0,1",
        "\tBZ $0,LPI1",
        "\tSETL $0,#0008",
        "\tADD $0,$253,$0",
        "\tLDOU $1,$0,0",
        "\tSETL $0,#000A",
        "\tDIV $0,$1,$0",
        "\tADD $0,$0,0",
        "\tSTOU $0,$254,8",
        "\tSETL $0,#021D",
        "\tSTOU $0,$254,0",
        "\tPUSHJ $8,_putint",
        "\tLDOU $0,$254,0",
        "\tADD $0,$0,0",
        "LPI1\tSETL $0,#0008",
        "\tADD $0,$253,$0",
        "\tLDOU $1,$0,0",
        "\tSETL $0,#000A",
        "\tDIV $0,$1,$0",
        "\tGET $1,rR",
        "\tSETL $0,#0030",
        "\tADD $1,$1,$0",
        "\tSETL $0,#00FF",
        "\tDIV $0,$1,$0",
        "\tGET $0,rR",
        "\tADD $0,$0,0",
        "\tSTOU $0,$254,8",
        "\tSETL $0,#021D",
        "\tSTOU $0,$254,0",
        "\tPUSHJ $8,_putchar",
        "\tLDOU $0,$254,0",
        "\tADD $0,$0,0",
        "\tSETL $0,#0",
        "\tADD $0,$0,0",
        
        "\tSTO $0,FP,#0",
        "\tADD SP,SP,16",
        "\tLDO $0,SP,#0",
        "\tPUT rJ,$0",
        "\tLDO $0,SP,#8",
        "\tSET SP,FP",
        "\tSET FP,$0",
        "\tPOP 0,0"
    );

    public static String instr2String(List<String> instrs){
        StringBuilder sb = new StringBuilder();
        for(String s : instrs){
            sb.append(s).append('\n');
        }
        return sb.toString();
    }
    
    public static String getFunctions(){
        StringBuilder sb = new StringBuilder();
        sb.append(instr2String(putchar));
        sb.append(instr2String(getchar));
        sb.append(instr2String(getint));
        sb.append(instr2String(_new));
        sb.append(instr2String(delete));
        sb.append(instr2String(exit));
        sb.append(instr2String(putint));
        return sb.toString();
    }    
}
