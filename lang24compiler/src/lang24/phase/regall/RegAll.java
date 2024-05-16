package lang24.phase.regall;

import java.util.*;

import lang24.data.mem.*;
import lang24.data.asm.*;
import lang24.phase.*;
import lang24.phase.asmgen.*;
import lang24.phase.livean.LiveAn;

/**
 * Register allocation.
 */
public class RegAll extends Phase {

	/** Mapping of temporary variables to registers. */
	public final HashMap<MemTemp, Integer> tempToReg = new HashMap<MemTemp, Integer>();
	public static final int nRegDefault = 4; //default value: 4
	public int nReg;
	
	public RegAll() {
		super("regall");
		this.nReg = nRegDefault;
	}
	
	public void allocate() {
		this.allocate(nRegDefault); //default value: nRegDefault
	}
	public void allocate(int nReg){
		System.out.printf("RegAll alive, n. reg: %d\n", nReg);
		this.nReg = nReg;
		int n_iter_init = 20;
		int n_iter = n_iter_init;
		for(Code c : AsmGen.codes){
			System.out.printf("-----> code segment: [%s]\n", c.frame.label.name);
			boolean successful = false;
			while(!successful){
				this.stack.clear();
				IntGraph ig = this.buildInterferenceGraph(c);
				successful = this.simplify(ig);
				//could handle successful
				successful = this.build(ig);
				// System.out.println(ig);
				if(successful){
					System.out.printf("------ [%s] This chunk's temps can be successfully assigned to registers. <------\n", c.frame.label.name);
					// System.out.println(ig);
					

					//mt -> color
					for(MemTemp mt : ig.graph.keySet()){
						GNode gn =  ig.graph.get(mt);
						this.tempToReg.put(mt, gn.color);
					}
				}
				else{
					System.out.print("[!] problematic temps: ");
					
					//get all problematic temps (those which have potential spill (that is now actual) set to true)
					// HashSet<MemTemp> problematic = new HashSet<>();
					HashMap<MemTemp, Integer> problematic = new HashMap<>();
					int offset_k = 0;
					for(GNode n : ig.graph.values()){
						if(n.potentialSpill){
							System.out.print(n.temp+" ");
							problematic.put(n.temp, offset_k*8); //also calculate offsets; map needed: HashMap<MemTemp, Integer> (temp->offset) TODO
							offset_k = offset_k + 1;
						}
					}
					System.out.println();
					
					this.fix(c, problematic);

					//loop
					// successful = true; //so it does not loop indefinitely (remove before testing actual functionality)
					// if(n_iter == 0){
					// 	successful=true;
					// 	n_iter = n_iter_init;
					// }
					// n_iter = n_iter -1;
				}
			}
			// n_iter = n_iter_init;
		}
	}

	//1. build graph from control flow graph & temp usages
	private IntGraph buildInterferenceGraph(Code c){
		IntGraph vrni = new IntGraph();
		for(AsmInstr i : c.instrs){
			vrni.add(i.in());
		}
		return vrni;
	}

	//2. simplify: take nodes one by one, hide them, check if spill occurs (mark spill), add them on stack for build (TODO)
	private final Vector<GNode> stack = new Vector<>(); //stack for building phase
	private boolean simplify(IntGraph graph){
		// System.out.println("simplify:");
		//get least connected, hide it, repeat
		int n = graph.graph.size();
		for(int i = 0; i < n; i++){
			GNode node = graph.findMinVN();
			int visibleNeigh = node.visibleNeighbours();
			if(visibleNeigh >= this.nReg){ //mark potential spill
				// System.out.printf("[!] %s, vn: %s\n", node.temp, visibleNeigh);
				node.potentialSpill = true;
			}
			else{
				node.potentialSpill = false;
				// System.out.printf("%s, vn: %s\n", node.temp, visibleNeigh);
			}
			node.visible = false;
			stack.add(node);
		}
		// System.out.println();
		return true;
	}

	//3. barvanje grafa
	private boolean build(IntGraph graph){
		boolean vrni = true;
		// System.out.println("build");
		int n = stack.size()-1;
		//reverse order
		for(int i = n; i >= 0; i--){
			GNode node = this.stack.get(i);
			boolean[] colors = new boolean[this.nReg]; //init all false
			for(MemTemp mt : node.neighbours){
				GNode neighNode = graph.graph.get(mt);
				int nodeColor = neighNode.color;
				if(neighNode.visible && nodeColor != -1){
					colors[nodeColor] = true; //set all colors, that are already used
				}
			}
			//find first available color:
			int c = 0;
			for(; c < colors.length; c++){
				if(!colors[c]) break;
			}
			//na voljo ni nobene barve (na tak nacin ne moremo pobarvat, vseeno nadaljuj,
			// poglej se za ostale, mogoce lahko kak drugi potential spill pobarvamo, vsekakor pa ne sme vrniti true, ker moramo spremeniti kodo)
			if(c == colors.length){
				// System.out.println("no available color, potential spill is also actual spill");
				// return false; //do not return, continue, just set return flag to false
				vrni = false;
			}
			else{ //ok, pobarvaj ga z barvo c
				node.color = c;
				//set potential spill to false
				node.potentialSpill = false; //if was true before, now it is no longer
				node.visible = true; //node is visible again
			}

			// System.out.printf("%s, color: %s\n",node.temp, node.color);
		}
		return vrni;
	}
	
	//4. dodaj kodo, se enkrat livean, se enkrat vse ostalo
	private void fix(Code c, HashMap<MemTemp, Integer> problematic){
		// System.out.println("this is fix");
		//spremeni ukaze (dodaj save + load)
		Vector<AsmInstr> instrs = new Vector<>();
		for(AsmInstr instr : c.instrs){
			//instr.out/in.clear() -> pobrisi in, out

			// -> kjer je use: load before
			//presek ni prazen
			HashSet<MemTemp> problematicUses = this.intersection(instr.uses(), problematic);
			Vector<MemTemp> newUses = new Vector<>(instr.uses()); //new uses (possibly changed temps)
			Vector<MemTemp> newDefs = new Vector<>(instr.defs()); //new defs
			for(MemTemp mt : problematicUses){
				//za vsaki problematicni temp naredi naslednje: naredi novi temp, poglej za offset (problematic.get(temp) -> offset)
				Vector<MemTemp> defs = new Vector<>();
				Vector<MemTemp> uses = new Vector<>();
				
				MemTemp T1_ = new MemTemp();
				//setl T1_, problematic.get(mt) //load offset
				//sub T1_, FP, T1_
				//ldo T1_, T1_, 0
				defs.add(T1_);
				AsmInstr set_offset = new AsmOPER("SETL `d0,"+problematic.get(mt) +" # spilled load", null, defs, null); //fix actual offset (get current offset (size), increase it with offset)
				defs = new Vector<>();

				defs.add(T1_);
				uses.add(T1_);
				AsmInstr stack_offset = new AsmOPER("SUB `d0,`$254,`s0", uses, defs, null); //fp is stored in $254 //nalozi iz offset + FP
				AsmInstr load_relative = new AsmOPER("LDO `d0,`s0,0 # end spilled load", uses, defs, null); //load from offset
				//spremeni use
				
				int index = newUses.indexOf(mt);
				// System.out.printf("spreminjam iz: %s v %s", newUses.get(index), T1_);
				newUses.set(index, T1_);
				// System.out.printf(" (%s)\n", newUses.get(index));
				
				
				//add extra code:
				instrs.add(set_offset);
				instrs.add(stack_offset);
				instrs.add(load_relative);
			}
			
			// -> kjer je def: store after
			HashSet<MemTemp> problematicDefs = this.intersection(instr.defs(), problematic);
			Vector<AsmInstr> codeAfter = new Vector<>();
			for(MemTemp mt : problematicDefs){
				//-------------
				//za vsaki problematicni temp naredi naslednje: naredi novi temp, poglej za offset (problematic.get(temp) -> offset)
				Vector<MemTemp> defs = new Vector<>();
				Vector<MemTemp> uses = new Vector<>();
				
				MemTemp T1_ = new MemTemp();
				//setl T1_, problematic.get(mt) //load offset
				//sub T1_, FP, T1_
				//sto T1_, T1_, 0
				defs.add(T1_);
				AsmInstr set_offset = new AsmOPER("SETL `d0,"+problematic.get(mt) +" # spilled store", null, defs, null); //fix actual offset (get current offset (size), increase it with offset)
				defs = new Vector<>();

				defs.add(T1_);
				uses.add(T1_);
				AsmInstr stack_offset = new AsmOPER("SUB `d0,`$254,`s0", uses, defs, null); //fp is stored in $254 //nalozi na offset + FP
				AsmInstr load_relative = new AsmOPER("STO `d0,`s0,0 # end spilled store", uses, defs, null); //load from offset //FIX TODO
				//spremeni def
				int index = newDefs.indexOf(mt);
				newDefs.set(index, T1_);

				//add extra code (save to buffer, as original instruction has not yet been added):
				codeAfter.add(set_offset);
				codeAfter.add(stack_offset);
				codeAfter.add(load_relative);
				//-------------
			}
			
			//create new instruction with new defs&uses
			AsmInstr newInstr;
			if(instr instanceof AsmMOVE){
				newInstr = new AsmMOVE(((AsmOPER)instr).instr(), newUses, newDefs);
			}
			else if (instr instanceof AsmLABEL){
				((AsmLABEL)instr).removeAllFromIn();
				((AsmLABEL)instr).removeAllFromOut();
				newInstr = instr;
			}
			else {
				newInstr = new AsmOPER(((AsmOPER)instr).instr(), newUses, newDefs, instr.jumps());
			}
			
			
			instrs.add(newInstr);
			//add codeAfter
			instrs.addAll(codeAfter);
		}
		//pobrisi c.instrs, nastavi nove instrs
		c.instrs.clear();
		c.instrs.addAll(instrs);

		//naredi livean nad kodo
		System.out.println("analying again ...");
		LiveAn.analyzeCode(c);
	}

	//ne bo vredu, treba bo popravit
	private <T> HashSet<T> intersection(Vector<T> current, HashMap<T, Integer> other){
		HashSet<T> vrni = new HashSet<>();
		for(T el : other.keySet()){
			if(current.contains(el)) vrni.add(el);
		}
		return vrni;
	}



	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			logger.begElement("code");
			logger.addAttribute("body", code.entryLabel.name);
			logger.addAttribute("epilogue", code.exitLabel.name);
			logger.addAttribute("tempsize", Long.toString(code.tempSize));
			code.frame.log(logger);
			logger.begElement("instructions");
			for (AsmInstr instr : code.instrs) {
				logger.begElement("instruction");
				logger.addAttribute("code", instr.toString(tempToReg));
				// logger.addAttribute("code", instr.toString()); //used for debug
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

