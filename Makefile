# Shortcuts around the Maven wrapper. Run `make help` to see the targets.
# The wrapper (./mvnw) downloads the right Maven version itself, so only a JDK is needed.

MVN := ./mvnw -B
JAR := target/library-management-system.jar
DIAGRAM_DIR := docs/diagram

.DEFAULT_GOAL := build
.PHONY: build run test clean rebuild diagram help

build: ## Compile and package the jar (skips tests)
	@$(MVN) -q -DskipTests package

run: build ## Run the program: reads stdin, or `make run INPUT=src/test/resources/golden/sample.in`
ifdef INPUT
	@java -jar $(JAR) < $(INPUT)
else
	@java -jar $(JAR)
endif

test: ## Compile and run every unit and golden test
	@$(MVN) verify

clean: ## Delete target/
	@$(MVN) -q clean

rebuild: clean build ## Clean, then build

diagram: ## Re-export the class diagram PDF from its Mermaid source
	npx -y @mermaid-js/mermaid-cli -q -i $(DIAGRAM_DIR)/class_diagram.mmd -o $(DIAGRAM_DIR)/class_diagram.pdf -c $(DIAGRAM_DIR)/mermaid-config.json --size 2200

help: ## Show this list
	@grep -E '^[a-z]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/^([a-z]+):.*## (.*)/  make \1\t\2/'
