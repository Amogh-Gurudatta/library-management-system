# Build helper for the Library Management System (the Java equivalent of a C Makefile).
# Run `make help` to see the targets.

JAVAC   ?= javac
JAVA    ?= java
RELEASE := 21
OUT     := out
SRC     := $(wildcard src/*.java)
STAMP   := $(OUT)/.built

# Diagram export (needs Node.js; downloads the Mermaid CLI on first use)
DIAGRAM_DIR := docs/diagram

.DEFAULT_GOAL := build
.PHONY: build run test clean rebuild diagram help

build: $(STAMP) ## Compile everything into out/ (only if a source changed)

# Recompile all sources whenever any .java file is newer than the stamp.
# javac needs the whole set at once, so there is no per-file rule.
$(STAMP): $(SRC)
	@mkdir -p $(OUT)
	$(JAVAC) --release $(RELEASE) -Xlint:all,-serial -Werror -d $(OUT) $(SRC)
	@touch $@

run: build ## Run the program: reads stdin, or `make run INPUT=tests/sample.in`
ifdef INPUT
	@$(JAVA) -cp $(OUT) Main < $(INPUT)
else
	@$(JAVA) -cp $(OUT) Main
endif

test: ## Compile and run every test in tests/ (same as ./run_tests.sh)
	@./run_tests.sh

clean: ## Delete compiled classes
	rm -rf $(OUT)

rebuild: clean build ## Clean, then compile

diagram: ## Re-export the class diagram PDF from its Mermaid source
	npx -y @mermaid-js/mermaid-cli -q -i $(DIAGRAM_DIR)/class_diagram.mmd -o $(DIAGRAM_DIR)/class_diagram.pdf -c $(DIAGRAM_DIR)/mermaid-config.json --size 2200

help: ## Show this list
	@grep -E '^[a-z]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/^([a-z]+):.*## (.*)/  make \1\t\2/'
