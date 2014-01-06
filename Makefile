.PHONY: export

# Check that 1) git is available and 2) the current working directory is a git
# reposity, so that it warns me about potentially uncommitted files before
# exporting the assignment.
CHANGES=$(shell if which git > /dev/null 2>&1 && git status > /dev/null 2>&1; then git diff-index --quiet HEAD; echo $$?; else echo 0; fi)

TD=$(shell basename `pwd`)

all: src/*.java
	-mkdir -p bin/
	javac -d bin/ src/*.java

test: all
	java -cp bin/ -ea Test

clean:
	rm -rf bin/* *.class

export: rungitcommit$(CHANGES)
	-mkdir -p bin/
	git archive --format tgz -o ../$(TD).tar.gz HEAD . --prefix=$(TD)/

rungitcommit0:
