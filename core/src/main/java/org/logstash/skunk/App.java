package org.logstash.skunk;


import org.logstash.skunk.api.config.Configuration;
import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.WriteQueue;
import org.logstash.skunk.api.plugin.Filter;
import org.logstash.skunk.api.plugin.Input;
import org.logstash.skunk.api.plugin.LogStashPlugin;
import org.logstash.skunk.api.plugin.Output;
import org.reflections.Reflections;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class App {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException, MalformedURLException, ClassNotFoundException, URISyntaxException {

        Queue queue = new ArrayBlockingQueue<Event>(1024);
        int workers = 2;

        WriteQueue writeQueue = new WriteQueue() {
            @Override
            public void put(Event event) {
                queue.add(event);
            }

            @Override
            public void put(Collection<Event> events) {
                events.forEach(e -> queue.add(e));
            }
        };
        Configuration config = new Configuration() {
        };

        //This strategy can be adapted to loading jars from .zip files in well known named directories, then programmatically un-zip to a staging location, add the .jar files to
        // the class path via the the URL class loader.

        //Instead of 1 class loader for the inputs and one for the outputs, there should be 1 class loader per .zip, A zip may contain inputs, outputs, and filters and thus
        // should all use the same loader. Logic will need to be added to know the grouping so that we can correctly set the thread Context class loader.


        //By default this (Java) is a parent first class loader, meaning that any all classes be attempted be loaded from the parent class loader first . This means that while the
        // plugins are isolated from each other, they are not isolated from the dependencies of the main app. e.g. if core depends on jackson 2.7, then all plugins will
        // have to use that version, and any version they ship with will be ignored. We should provide an option to use a child first class loader strategy to allow
        // plugins to optionally override versions in core.

//        URLClassLoader loader = URLClassLoader.newInstance(new URL[] {new URL("file:///Users/jake/workspace/skunkstash/input/target/input-1.0-SNAPSHOT.jar")}); //via jar

        URI baseURI = App.class.getClassLoader().getResource(".").toURI().resolve("../../../");

        //via .class, don't forget the trailing /
        URLClassLoader inputLoader = URLClassLoader.newInstance(new URL[]{baseURI.resolve("input/target/classes/").toURL()});
        URLClassLoader outputLoader = URLClassLoader.newInstance(new URL[]{
                baseURI.resolve("filter/target/classes/").toURL(),
                baseURI.resolve("output/target/classes/").toURL()});

        //Find plugin via reflections
        Reflections reflections = new Reflections("org.logstash.skunk.plugin", inputLoader, outputLoader);
        Set<Class<?>> plugins = reflections.getTypesAnnotatedWith(org.logstash.skunk.api.plugin.LogStashPlugin.class);
        Set<Class<?>> inputClasses = new HashSet<>();
        Set<Class<?>> filterClasses = new HashSet<>();
        Set<Class<?>> outputClasses = new HashSet<>();


        for (Class<?> clazz : plugins) {
            Class<?>[] interfaces = clazz.getInterfaces();
            LogStashPlugin plugin = clazz.getAnnotation(LogStashPlugin.class);
            String type = null;
            for (Class<?> i : interfaces) {
                if ("org.logstash.skunk.api.plugin.Input".equals(i.getCanonicalName())) {
                    inputClasses.add(clazz);
                    type= "input";
                } else if ("org.logstash.skunk.api.plugin.Filter".equals(i.getCanonicalName())) {
                    filterClasses.add(clazz);
                    type= "filter";
                } else if ("org.logstash.skunk.api.plugin.Output".equals(i.getCanonicalName())) {
                    outputClasses.add(clazz);
                    type= "output";
                }

                System.out.println("Found " + type + " plugin \"" + plugin.value() + "\" (singleton=" + plugin.singleton() + ")");
            }
        }


        List<Filter> filters = new ArrayList<>();
        for (Class<?> clazz : filterClasses) {
            Filter filter = (Filter) clazz.newInstance();
            filters.add(filter);
        }

        List<Output> outputs = new ArrayList<>();
        for (Class<?> clazz : outputClasses) {
            Output output = (Output) clazz.newInstance();
            outputs.add(output);
        }
        System.out.println(String.format("Found %d inputs, %d filters, %d outputs", inputClasses.size(), filterClasses.size(), outputClasses.size()));

        List<Input> inputs = new ArrayList<>();
        //Each input gets its own thread
        ExecutorService inputExecutorService = Executors.newFixedThreadPool(inputClasses.size(), new PluginThreadFactory("input", inputLoader));
        for (Class<?> clazz : inputClasses) {
            Input input = (Input) clazz.newInstance();
            inputs.add(input);
            inputExecutorService.execute(() -> input.start(config, writeQueue));
        }

        List<Processor> processors = new ArrayList<>();
        //a thread for each worker
        ExecutorService processingExecutorService = Executors.newFixedThreadPool(workers, new PluginThreadFactory("output", outputLoader));
        IntStream.of(workers).forEach(__ -> {
            Processor processor = new Processor(config, filters, outputs, queue);
            processors.add(processor);
            processingExecutorService.execute(() -> processor.start());
        });


        //Shutdown hook to allow for call to stop
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            System.out.println("Shutting down inputs...");
            inputs.forEach(input -> input.stop());
            shutdown(inputExecutorService, "inputs");

            System.out.println("Shutting down outputs...");
            processors.forEach(p -> p.stop());
            shutdown(processingExecutorService, "outputs");

            System.out.println("Bye Bye...");
        }));

    }


    /**
     * Attempt to clean shutdown, after a bit, interrupt the thread
     */
    public static void shutdown(ExecutorService service, String name) {
        try {
            service.shutdown();
            int max_wait = 10;
            int i = 0;
            while (i++ < max_wait) {
                System.out.println("Waiting for the " + name + " to terminate...");
                if (service.awaitTermination(1, TimeUnit.SECONDS)) {
                    break;
                }
            }
            if (i >= max_wait) {
                //knock knock, whose there? , interrupting thread, interrupting thread who ?
                service.shutdownNow();
                if (!service.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.out.println("Shut down of " + name + " not stop cleanly after " + max_wait + " seconds");
                }
            }
        } catch (InterruptedException e) {
            //do nothing
        }
    }

}
