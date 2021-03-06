/*
 * MindmapsDB - A Distributed Semantic Database
 * Copyright (C) 2016  Mindmaps Research Ltd
 *
 * MindmapsDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MindmapsDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package io.mindmaps.graql;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.sun.corba.se.impl.util.Version;
import io.mindmaps.MindmapsTransaction;
import io.mindmaps.core.MindmapsGraph;
import io.mindmaps.core.implementation.exception.InvalidConceptTypeException;
import io.mindmaps.core.implementation.exception.MindmapsValidationException;
import io.mindmaps.core.model.Concept;
import io.mindmaps.core.model.Instance;
import io.mindmaps.factory.MindmapsClient;
import io.mindmaps.graql.internal.parser.ANSI;
import io.mindmaps.graql.internal.parser.MatchQueryPrinter;
import io.mindmaps.graql.internal.shell.ErrorMessage;
import io.mindmaps.graql.internal.shell.GraQLCompleter;
import io.mindmaps.graql.internal.shell.ShellCommandCompleter;
import jline.console.ConsoleReader;
import jline.console.completer.AggregateCompleter;
import jline.console.history.FileHistory;
import org.apache.commons.cli.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Graql REPL shell that can be run from the command line
 */
public class GraqlShell implements AutoCloseable {
    private static final String LICENSE_PROMPT = "\n" +
            "MindmapsDB  Copyright (C) 2016  Mindmaps Research Ltd \n" +
            "This is free software, and you are welcome to redistribute it \n" +
            "under certain conditions; type 'license' for details.\n";

    private static final String LICENSE_LOCATION = "LICENSE.txt";

    private static final String NAMESPACE = "mindmaps";

    private static final String PROMPT = ">>> ";

    private static final String EDIT_COMMAND = "edit";
    private static final String COMMIT_COMMAND = "commit";
    private static final String LOAD_COMMAND = "load";
    private static final String CLEAR_COMMAND = "clear";
    private static final String EXIT_COMMAND = "exit";
    private static final String LICENSE_COMMAND = "license";

    /**
     * Array of available commands in shell
     */
    public static final String[] COMMANDS = {EDIT_COMMAND, COMMIT_COMMAND, LOAD_COMMAND, CLEAR_COMMAND, EXIT_COMMAND};

    private static final String TEMP_FILENAME = "/graql-tmp.gql";
    private static final String HISTORY_FILENAME = "/graql-history";

    private static final String DEFAULT_EDITOR = "vim";

    private final File tempFile = new File(System.getProperty("java.io.tmpdir") + TEMP_FILENAME);
    private ConsoleReader console;
    private PrintStream err;

    private final MindmapsGraph graph;
    private final MindmapsTransaction transaction;
    private final Reasoner reasoner;

    /**
     * Run a Graql REPL
     * @param args arguments to the Graql shell. Possible arguments can be listed by running {@code graql.sh --help}
     */
    public static void main(String[] args) {
        InputStream in = System.in;
        PrintStream out = System.out;
        PrintStream err = System.err;
        runShell(args, MindmapsClient::getGraph, MindmapsClient::getGraph, Version.VERSION, in, out, err);
    }

    public static void runShell(
            String[] args,
            Function<String, MindmapsGraph> localFactory, BiFunction<String, String, MindmapsGraph> remoteFactory,
            String version, InputStream in, PrintStream out, PrintStream err
    ) {
        // Disable horrid cassandra logs
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.OFF);

        Options options = new Options();
        options.addOption("n", "name", true, "name of the graph");
        options.addOption("e", "execute", true, "query to execute");
        options.addOption("f", "file", true, "graql file path to execute");
        options.addOption("u", "uri", true, "uri to connect to engine");
        options.addOption("h", "help", false, "print usage message");
        options.addOption("v", "version", false, "print version");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            err.println(e.getMessage());
            return;
        }

        String query = cmd.getOptionValue("e");
        String filePath = cmd.getOptionValue("f");

        // Print usage message if requested or if invalid arguments provided
        if (cmd.hasOption("h") || !cmd.getArgList().isEmpty()) {
            HelpFormatter helpFormatter = new HelpFormatter();
            PrintWriter printWriter = new PrintWriter(out);
            int width = helpFormatter.getWidth();
            int leftPadding = helpFormatter.getLeftPadding();
            int descPadding = helpFormatter.getDescPadding();
            helpFormatter.printHelp(printWriter, width, "graql.sh", null, options, leftPadding, descPadding, null);
            printWriter.flush();
            return;
        }

        if (cmd.hasOption("v")) {
            out.println(version);
            return;
        }

        String namespace = cmd.getOptionValue("n", NAMESPACE);

        MindmapsGraph graph;

        if (cmd.hasOption("u")) {
            graph = remoteFactory.apply(cmd.getOptionValue("u"), namespace);
        } else {
            graph = localFactory.apply(namespace);
        }

        try(GraqlShell shell = new GraqlShell(graph, in, out, err)) {
            if (filePath != null) {
                query = loadQuery(filePath);
            }

            if (query != null) {
                shell.executeQuery(query, false);
                shell.commit();
            } else {
                shell.executeRepl();
            }
        } catch (IOException e) {
            err.println(e.toString());
        }
    }

    private static String loadQuery(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        return lines.stream().collect(Collectors.joining("\n"));
    }

    /**
     * Create a new Graql shell
     * @param graph the graph to operate on
     */
    GraqlShell(MindmapsGraph graph, InputStream in, OutputStream out, PrintStream err) throws IOException {
        this.graph = graph;
        transaction = graph.getTransaction();
        reasoner = new Reasoner(transaction);
        console = new ConsoleReader(in, out);
        this.err = err;
    }

    @Override
    public void close() throws IOException {
        graph.close();
        console.flush();
    }

    /**
     * Run a Read-Evaluate-Print loop until the input terminates
     */
    void executeRepl() throws IOException {
        console.print(LICENSE_PROMPT);

        // Disable JLine feature when seeing a '!', which is used in our queries
        console.setExpandEvents(false);

        console.setPrompt(PROMPT);

        // Create temporary file
        if (!tempFile.exists()) {
            boolean success = tempFile.createNewFile();
            if (!success) print(ErrorMessage.COULD_NOT_CREATE_TEMP_FILE.getMessage());
        }

        // Create history file
        File historyFile = new File(System.getProperty("java.io.tmpdir") + HISTORY_FILENAME);
        //noinspection ResultOfMethodCallIgnored
        historyFile.createNewFile();
        FileHistory history = new FileHistory(historyFile);
        console.setHistory(history);

        // Add all autocompleters
        console.addCompleter(new AggregateCompleter(new GraQLCompleter(graph), new ShellCommandCompleter()));

        String queryString;

        while ((queryString = console.readLine()) != null) {
            history.flush();

            switch (queryString) {
                case EDIT_COMMAND:
                    executeQuery(runEditor(), true);
                    break;
                case COMMIT_COMMAND:
                    commit();
                    break;
                case CLEAR_COMMAND:
                    console.clearScreen();
                    break;
                case LICENSE_COMMAND:
                    printLicense();
                    break;
                case EXIT_COMMAND:
                    return;
                case "":
                    // Ignore empty command
                    break;
                default:
                    // Load from a file if load command used
                    if (queryString.startsWith(LOAD_COMMAND + " ")) {
                        String path = queryString.substring(LOAD_COMMAND.length() + 1);

                        try {
                            queryString = loadQuery(path);
                        } catch (IOException e) {
                            err.println(e.toString());
                            break;
                        }
                    }

                    executeQuery(queryString, true);
                    break;
            }
        }
    }

    private void printLicense(){
        StringBuilder result = new StringBuilder("");

        //Get file from resources folder
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(LICENSE_LOCATION);

        Scanner scanner = new Scanner(is);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            result.append(line).append("\n");
        }
        result.append("\n");
        scanner.close();

        this.print(result.toString());
    }

    private void executeQuery(String queryString, boolean setLimit) {
        Object query;

        try {
            QueryParser parser = QueryParser.create(transaction);
            query = parser.parseQuery(queryString);

            if (query instanceof MatchQueryPrinter) {
                printMatchQuery((MatchQueryPrinter) query, setLimit);
            } else if (query instanceof AskQuery) {
                printAskQuery((AskQuery) query);
            } else if (query instanceof InsertQuery) {
                printInsertQuery((InsertQuery) query);
                reasoner.linkConceptTypes();
            } else if (query instanceof DeleteQuery) {
                ((DeleteQuery) query).execute();
                reasoner.linkConceptTypes();
            } else if (query instanceof Long) {
                // Count query
                print(query.toString()+"\n");
            } else if (query instanceof Map) {
                // Degree query
                //noinspection unchecked
                ((Map<Instance, Long>) query).forEach((instance, degree) -> print(instance.getId() + "\t" + degree+"\n"));
            } else if (query == null) {
                print("Degrees have been persisted.\n");
            } else {
                throw new RuntimeException("Unrecognized query " + query);
            }
        } catch (IllegalArgumentException | IllegalStateException | InvalidConceptTypeException e) {
            err.println(e.getMessage());
        }
    }

    private void printMatchQuery(MatchQueryPrinter matchQuery, boolean setLimit) {
        // Expand match query with reasoner
        matchQuery.setMatchQuery(reasoner.expand(matchQuery.getMatchQuery()));

        Stream<String> results = matchQuery.resultsString();
        if (setLimit) results = results.limit(100);
        results.forEach(this::println);
    }

    private void printAskQuery(AskQuery askQuery) {
        if (askQuery.execute()) {
            println(ANSI.color("True", ANSI.GREEN));
        } else {
            println(ANSI.color("False", ANSI.RED));
        }
    }

    private void printInsertQuery(InsertQuery insertQuery) {
        insertQuery.stream().map(Concept::getId).forEach(this::println);
    }

    private void commit() {
        try {
            transaction.commit();
        } catch (MindmapsValidationException e) {
            err.println(e.getMessage());
        }
    }

    /**
     * load the user's preferred editor to edit a query
     * @return the string written to the editor
     */
    private String runEditor() throws IOException {
        // Get preferred editor
        Map<String, String> env = System.getenv();
        String editor = Optional.ofNullable(env.get("EDITOR")).orElse(DEFAULT_EDITOR);

        // Run the editor, pipe input into and out of tty so we can provide the input/output to the editor via Graql
        ProcessBuilder builder = new ProcessBuilder(
                "/bin/bash",
                "-c",
                editor + " </dev/tty >/dev/tty " + tempFile.getAbsolutePath()
        );

        // Wait for user to finish editing
        try {
            builder.start().waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return String.join("\n", Files.readAllLines(tempFile.toPath()));
    }

    private void print(String string) {
        try {
            console.print(string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void println(String string) {
        print(string + "\n");
    }
}
