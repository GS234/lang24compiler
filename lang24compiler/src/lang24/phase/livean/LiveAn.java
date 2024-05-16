package lang24.phase.livean;

import lang24.data.mem.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Vector;

import lang24.data.asm.*;
import lang24.phase.*;
import lang24.phase.asmgen.*;

/**
 * Liveness analysis.
 */
public class LiveAn extends Phase {

	public LiveAn() {
		super("livean");
	}

	public void analysis() {
		//iter over codes:
		for(Code c : AsmGen.codes){
			// this.analyzeCode(c);
			analyzeCode(c);
		}
	}

	//main thing happens here (check if it works as intended)
	//this function is also used in regall
	public static void analyzeCode(Code c){
		int nIter = 1;
		do{
			//for all n: set in, out
			boolean inChanged = false;
			boolean outChanged = false;

			//gledamo v obratnem vrstnem redu! (mogoce prisparamo kako iteracijo, glej zapiske predavanja)
			Vector<AsmInstr> v = new Vector<>(c.instrs); //new vector, reverse it!
			Collections.reverse(v); //reverse list
			
			AsmInstr previous = null; //previous instruction (in reverse order: next)
			for(AsmInstr i : v){
				//in:
				HashSet<MemTemp> in_new = new HashSet<>(i.uses()); //first, fill with uses
				HashSet<MemTemp> out_current = i.out();
				
				for(MemTemp mt : i.defs()){
					out_current.remove(mt); //remove all from def
				}
				in_new.addAll(out_current);
				//check if in changed, set flag:
				if(!inChanged){ //not changed yet, maybe this one has changed
					HashSet<MemTemp> current_in = i.in(); //get new in
					inChanged = !current_in.equals(in_new);
				}
				
				//out:
				HashSet<MemTemp> out_new = new HashSet<>();
				//possible jumps:
				for(MemLabel ml : i.jumps()){ //possible successors (jump)
					AsmInstr ai = AsmGen.label2instr.get(ml);
					if(ai == null) continue;
					// int label_i = c.instrs.indexOf(ai)+1;
					// while(label_i != 0 && ai instanceof AsmLABEL && label_i < c.instrs.size()){ //find first instruction that is not label
					// 	ai = c.instrs.get(label_i); //ni reverse, to je ok
					// 	label_i++;
					// }
					// if(!(ai instanceof AsmLABEL)) out_new.addAll(ai.in()); //ce ni label, lahko dobimo in
					out_new.addAll(ai.in());
				}
				//direct neighbour (successor)
				if(previous != null){
					out_new.addAll(previous.in());
				}
				//check if changed
				if(!outChanged){ //not changed yet, maybe this one has changed
					HashSet<MemTemp> current_out = i.out(); //get new in
					outChanged = !current_out.equals(out_new); //if empty, then it is the same, so not changed (false)
				}

				i.addInTemps(in_new);
				i.addOutTemp(out_new);

				previous = i;
			}

			if(!inChanged && !outChanged) break;
			nIter = nIter+1;
		}
		while(true);
		System.out.printf("iter: [%s] %d\n",c.frame.label.name,nIter);
	}
	
	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			logger.begElement("code");
			logger.addAttribute("prologue", code.entryLabel.name);
			logger.addAttribute("body", code.entryLabel.name);
			logger.addAttribute("epilogue", code.exitLabel.name);
			logger.addAttribute("tempsize", Long.toString(code.tempSize));
			code.frame.log(logger);
			logger.begElement("instructions");
			for (AsmInstr instr : code.instrs) {
				logger.begElement("instruction");
				logger.addAttribute("code", instr.toString());
				logger.begElement("temps");
				logger.addAttribute("name", "use");
				for (MemTemp temp : instr.uses()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "def");
				for (MemTemp temp : instr.defs()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "in");
				for (MemTemp temp : instr.in()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "out");
				for (MemTemp temp : instr.out()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.endElement();
			}
			logger.endElement();
			logger.endElement();
		}
	}

}
