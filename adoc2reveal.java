///usr/bin/env echo '
/**** BOOTSTRAP jbang ****'>/dev/null
command -v jbang >/dev/null 2>&1 || curl -Ls https://sh.jbang.dev | bash -s app setup
exec `$SHELL -c "which jbang"` "$0" "$@" ; exit $?
\*** IMPORTANT: Any code including imports and annotations must come after this line ***/

//JAVA 21+
//JAVA_OPTIONS --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
//DEPS org.asciidoctor:asciidoctorj:2.5.3
//DEPS org.asciidoctor:asciidoctorj-diagram:2.2.8
//DEPS org.asciidoctor:asciidoctorj-revealjs:5.0.0.rc1
//DEPS org.asciidoctor:asciidoctorj-chart:1.0.0
//DEPS info.picocli:picocli:4.6.3

import com.sun.net.httpserver.SimpleFileServer;
import static com.sun.net.httpserver.SimpleFileServer.OutputLevel;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static picocli.CommandLine.*;

@Command(name="adoc2reveal", version="1.0")
public class adoc2reveal implements Callable<Integer> {

    @Parameters(index="0", description = ".adoc file to convert/render.", defaultValue = "index.adoc")
    private File file;

    @Option(names={"-w", "--watch"}, description="Watch for changes and re-render if .adoc file changes")
    private boolean watch;

    @Option(names={"--serve"}, description="Serve the rendered presentation on http://localhost:8080")
    private boolean serve;

    @Option(names={"--revealjsdir"}, defaultValue = "https://cdn.jsdelivr.net/npm/reveal.js@4.3.1", description = "revealjs directory or base url")
    String revealjsdir;

    @Option(names = "--verbose")
    boolean verbose;

    private Asciidoctor asciidoctor;
    private WatchService watchService;

    public static void main(String... args) {
       System.exit(new CommandLine(new adoc2reveal()).execute(args));
    }

    void verbose(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }

    private void registerRecursive(final Path root) throws IOException {
        root.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        // register all subfolders
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if(!dir.getFileName().toString().startsWith(".")) {
                    verbose("Register watch on " + root.relativize(dir));
                    dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                    return FileVisitResult.CONTINUE;
                } else {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        });
    }
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Booting up Asciidoctor...");
        asciidoctor = Asciidoctor.Factory.create();

        asciidoctor.registerLogHandler(new LogHandler() {
            @Override
            public void log(LogRecord logRecord) {
                System.out.println(logRecord.getMessage());
            }
        });
        asciidoctor.requireLibrary("asciidoctor-revealjs");
        asciidoctor.requireLibrary("asciidoctor-diagram");
        asciidoctor.requireLibrary("asciidoctor-chart");

        render();

        Path p2watch = file.getAbsoluteFile().getParentFile().toPath();

        if(serve) {
            var address = new InetSocketAddress(8000);
            System.out.println("Serving on http://" +  address.getHostString() + ":" + address.getPort() + " from " + p2watch + " ...");

            var server = SimpleFileServer.createFileServer(address, 
                            p2watch, 
                            OutputLevel.INFO); 
            server.start();
        }
        if(watch || serve) {
            watchService = FileSystems.getDefault().newWatchService();

            if(file.canRead()) {
                System.out.println("Watching files in " + p2watch);
                registerRecursive(p2watch);
                WatchKey wkey;
                while ((wkey = watchService.take()) != null) {
                    for (WatchEvent<?> event : wkey.pollEvents()) {
                       Path p = (Path) event.context();
                       verbose(p + " " + event.kind());
                       if(p.getFileName().toString().endsWith(".adoc")) {
                           render();
                       } else {
                           verbose("Skipping " + p.getFileName().toString() + " as no match.");
                       }
                    }
                    wkey.reset();
                }
            } else {
                System.err.println("Could not read " + file);
            }
        }

        return 0;
    }

    private void render() {
        System.out.printf("Start Rendering %s\n",file);

        asciidoctor.convertFile(file,
                Options.builder()
                        .backend("revealjs")
                        .safe(SafeMode.UNSAFE)
                        .attributes(
                                Attributes.builder()
                                        .attribute("revealjsdir", revealjsdir)
                                        .build()
                        ).build()
        );
        System.out.printf("Done Rendering %s\n",file);
    }
}
