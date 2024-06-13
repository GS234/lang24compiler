package lang24.phase.outgen;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Vector;

import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
import lang24.data.asm.Code;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.MemFrame;
import lang24.data.mem.MemTemp;
import lang24.phase.Phase;
import lang24.phase.asmgen.AsmGen;
import lang24.phase.asmgen.AsmGenerator;
import lang24.phase.imclin.ImcLin;
import lang24.phase.memory.MemEvaluator;
import lang24.phase.regall.RegAll;

public class OutGen extends Phase{
	private static final String FILE_NAME = "lang24.out";
	private static final long HEAP_POINTER = 0x4000000000000000L;
    private static final long STACK_POINTER = 0x6000000000000000L;
	private int nReg = RegAll.nRegDefault;

	public OutGen() {
		super("outgen");
	}

	public void setNreg(int nReg){
		this.nReg = nReg;
	}

	public void generateOutput(){
		// open file for writing
		try(PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))){
			// add all necessary 'things' before code (init heap, stack, greg, necessary section names, ...)
			out.print(this.entryCode());
			// add stdlib
			out.print("### stdlib begin ###\n");
			out.print(StdLib.getFunctions());
			out.print("### stdlib end   ###\n");
			// go over each code segment, print it to file (add prologue, epilogue)
			for(Code c : AsmGen.codes){
				out.print(this.addPrologue(c)); // add prologue
				for(AsmInstr instr : c.instrs){
					// print codes
					if(instr instanceof AsmLABEL) out.print(instr.toString(RegAll.tempToReg));
					else out.println("\t"+instr.toString(RegAll.tempToReg));
				}
				out.print(this.addEpilogue(c)); // add epilogue
			}
		}
		catch(IOException e){
			System.out.println("[!] (outgen) error, could not open file for writing, details: "+e);
		}
	}

	// TODO
	private String entryCode(){
		StringBuilder sb = new StringBuilder();

        // Set up pointers (gregs)
        // Trap register
        // sb.append("TR GREG 0\n");
        // Stack pointer
        sb.append("SP\tGREG 0\n");
        // Frame pointer
        sb.append("FP\tGREG 0\n");
        // Heap pointer
        sb.append("HP\tGREG 0\n");

        // Data segment
        sb.append("\tLOC Data_Segment\n");
		// add global data
		sb.append(this.addGlobalData());


        // Text segment
        sb.append("\tLOC #100\n");
        sb.append("Main\tSETL SP,#0\n");
		
		// init stack pointer, heap pointer
		Vector<AsmInstr> instrs = AsmGenerator.loadConstantToReg("SP", STACK_POINTER);
		for(AsmInstr inst : instrs){
			sb.append('\t').append(inst).append('\n');
		}
        
        sb.append("\tSET FP,SP\n");
		instrs = AsmGenerator.loadConstantToReg("HP", HEAP_POINTER);
		for(AsmInstr inst : instrs){
			sb.append('\t').append(inst).append('\n');
		}

        // Jump to _main
        sb.append("\tPUSHJ $" + this.nReg + ",_main\n");

        // Load return value
        sb.append("\tLDO $0,SP,#0\n");

        // Halt
        sb.append("\tTRAP 0,Halt,0\n");

		return sb.toString();
	}

	// TODO
	private String addGlobalData(){
		Vector<LinDataChunk> dataChunks = ImcLin.dataChunks(); //get data chunks
		StringBuilder sb = new StringBuilder();

		for(LinDataChunk c : dataChunks){
			// System.out.printf("%s: size: %d, init: %s\n", c.label.name, c.size, c.init);
			String name = c.label.name;
			if(c.init != null){ //je string, in je inicializiran
				sb.append(String.format("%s\tOCTA ", name));
				// System.out.printf("%s\tOCTA ", name);
				String value = c.init;
				value = value.substring(1,value.length()-1); //remove parenthesis
				value = value+'\0'; //add null terminator
				for(int i = 0; i < value.length(); i++){
					if(i > 0){
						sb.append(',');
						// System.out.print(",");
					}
					sb.append((int)value.charAt(i));
					// System.out.print((int)value.charAt(i));
				}
				sb.append('\n');
				// System.out.println();
			}
			else{ //ostali
				String sizeString;
				switch ((int)c.size) {
					case 1: sizeString = "BYTE"; break;
					case 2: sizeString = "WYDE"; break;
					case 4: sizeString = "TETRA"; break;
					case 8: sizeString = "OCTA"; break;
					default: sizeString = null; break;
				}
				if(sizeString != null){
					// System.out.printf("%s\t%s 0\n", name, sizeString);
					sb.append(String.format("%s\t%s 0\n", name, sizeString));
				}
				else{
					// System.out.printf("%s\tOCTA 0\n", name);
					// System.out.printf("\t\tLOC @+#%x\n", (c.size-1));

					sb.append(String.format("%s\tOCTA 0\n", name));
					sb.append(String.format("\t\tLOC @+#%x\n", (c.size-1)));
				}
			}
		}
		return sb.toString();
		// return "### global data: TODO ###\n";
	}



	// prologue, epilogue:
	private String addPrologue(Code c){
		final long pointerSize = MemEvaluator.ptrSize; //size of pointer
		final long c_temp_count = c.tempSize; // get number of temporary variables created during regall
		// get all necessary information from memframe, construct prologue

		// stvari, ki grejo v epilog:
		
		// function label
		// shrani fp, shrani ra
		
		// premik fp, sp
		// skok v jedro funkcije (ubistvu ni treba, ker se nadaljuje ze takoj naprej)

		StringBuilder sb = new StringBuilder();
		sb.append("# prologue\n");
		sb.append(c.frame.label.name).append('\t');

		// save current SP
		sb.append("SET $0,SP # save SP\n");
		long locals_size = c.frame.locsSize + 2*pointerSize; // torej preskoci eno pomnilno polje/besedo za static link
		
		// move SP forward (jump over local vars)
		sb.append("\tSUB SP,SP,").append(locals_size).append('\n');
		
		// store old fp
		sb.append("\tSTO FP,SP,#").append(pointerSize).append('\n');

		// Store return address
		sb.append("\tGET FP,rJ").append('\n'); // get return address
		sb.append("\tSTO FP,SP,#0").append('\n'); // store it
        
        // Set frame pointer to old sp
		sb.append("\tSET FP,$0").append('\n');
        
        // Set new sp
		// System.out.printf("function %s argsize: %s ", c.frame.label.name, c.frame.argsSize);
		// System.out.printf("function %s temps: %s (%s)\n", c.frame.label.name, c_temp_count, c_temp_count*pointerSize);
        long temp_size = c_temp_count * pointerSize + c.frame.argsSize;
        sb.append("\tSUB SP,SP,").append(temp_size).append('\n');

		// jump to body
		sb.append("\tJMP ").append(c.entryLabel.name).append('\n'); 
		return sb.toString();
	}
	
	private String addEpilogue(Code c){
		final long pointerSize = MemEvaluator.ptrSize;
		final long c_temp_count = c.tempSize;

		// get all necessary information from memframe, construct epilogue
		StringBuilder sb = new StringBuilder();
		sb.append("# epilogue\n");
		// starts with exit label
		sb.append(c.exitLabel.name + "\t");
        // Store return value
        Integer returnReg = RegAll.tempToReg.get(c.frame.RV);
		sb.append("STO $" + returnReg + ",FP,0\n");

        // Add to SP in order to then restore old FP and return address
        long size = c.frame.argsSize + c_temp_count * pointerSize;
        sb.append("\tADD SP,SP," + size + "\n");

        // Load return address
        sb.append("\tLDO $0,SP,#0\n");
        // Restore it
        sb.append("\tPUT rJ,$0\n");

        // Load old FP
        sb.append("\tLDO $0,SP,#8\n");

        // Resore SP
        sb.append("\tSET SP,FP\n");

        // Restore FP
        sb.append("\tSET FP,$0\n");

        sb.append("\tPOP 0,0\n");
		sb.append('\n');
		return sb.toString();
	}

	public void log() {
		if (logger == null)
			return;
		logger.begElement("none");
		logger.endElement();
		
	}
	
}
