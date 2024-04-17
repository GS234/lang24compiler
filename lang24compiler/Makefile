JAVA		= java --enable-preview
JAVAC		= javac --enable-preview --release 21 -encoding UTF8
ANTLR           = org.antlr.v4.Tool
ANTLRDIR	= lib/antlr-4.13.1-complete.jar
RM		= 'rm' -fr
FIND		= 'find'

all	:
	if [ -d src/lang24/phase/lexan ] ; then $(MAKE) -C src/lang24/phase/lexan ; fi
	if [ -d src/lang24/phase/synan ] ; then $(MAKE) -C src/lang24/phase/synan ; fi
	$(JAVAC) --module-path $(ANTLRDIR) --source-path src -d bin src/lang24/Compiler.java
	@echo ":-) OK"

.PHONY	: clean
clean	:
	if [ -d doc ] ; then $(MAKE) -C doc clean ; fi
	if [ -d src ] ; then $(MAKE) -C prg clean ; fi
	if [ -d src/lang24/phase/lexan ] ; then $(MAKE) -C src/lang24/phase/lexan clean ; fi
	if [ -d src/lang24/phase/synan ] ; then $(MAKE) -C src/lang24/phase/synan clean ; fi
	$(FIND) . -type f -iname "*~" -exec $(RM) {} \;
	$(FIND) . -type f -iname "*.class" -exec $(RM) {} \;
	$(RM) bin
