all: clean compile maven

clean:
	rm -f src/main/resources/libkbjp2.so

compile:
	mkdir -p src/main/resources
	gcc -shared -o src/main/resources/libkbjp2.so -I$(JAVA_HOME)/include/ -I$(JAVA_HOME)/include/linux/ -I${OPJ_INC} src/main/c/nl_kb_jp2_JP2Reader.c -fPIC -lopenjp2

maven:
	mvn clean install package assembly:single

