JAR     := lib/postgresql-42.7.4.jar
SRC_DIR := src
OUT_DIR := out

# Separator is : on Unix/macOS, ; on Windows
SEP := :

.PHONY: all compile test-db compile-logic test-logic run clean

## Compile everything (main app + TestLogic) and TestDB
all: compile compile-logic compile-db

## Compile the main application sources
compile:
	mkdir -p $(OUT_DIR)
	javac -cp $(JAR) -d $(OUT_DIR) \
	    $(SRC_DIR)/com/tracker/dao/*.java \
	    $(SRC_DIR)/com/tracker/service/*.java \
	    $(SRC_DIR)/com/tracker/controller/*.java \
	    $(SRC_DIR)/com/tracker/Main.java

## Compile TestDB.java (database connection smoke-test)
compile-db:
	javac -cp $(JAR) TestDB.java

## Run the database connection smoke-test
test-db: compile-db
	java -cp .$(SEP)$(JAR) TestDB

## Compile TestLogic.java (pure-logic unit tests, no DB required)
compile-logic:
	mkdir -p $(OUT_DIR)
	javac -cp $(JAR) -d $(OUT_DIR) \
	    $(SRC_DIR)/com/tracker/dao/*.java \
	    $(SRC_DIR)/com/tracker/service/*.java \
	    TestLogic.java

## Run the logic unit tests
test-logic: compile-logic
	java -cp $(OUT_DIR)$(SEP)$(JAR) TestLogic

## Start the HTTP server
run: compile
	java -cp $(OUT_DIR)$(SEP)$(JAR) com.tracker.Main

## Remove compiled output
clean:
	rm -rf $(OUT_DIR) TestDB.class TestLogic.class
