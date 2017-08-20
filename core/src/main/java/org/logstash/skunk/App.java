package org.logstash.skunk;


import org.logstash.skunk.api.config.Configuration;
import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.WriteQueue;
import org.logstash.skunk.api.plugin.Filter;
import org.logstash.skunk.api.plugin.Input;
import org.logstash.skunk.api.plugin.Output;
import org.reflections.Reflections;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class App {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException {

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

        //TODO: implement custom classpath and load the class via reflections

        //Find plugin via reflections
        Reflections reflections = new Reflections("org.logstash.skunk.plugin");
        Set<Class<?>> plugins = reflections.getTypesAnnotatedWith(org.logstash.skunk.api.plugin.LogStashPlugin.class);

        Set<Class<?>> inputClasses = new HashSet<>();
        Set<Class<?>> filterClasses = new HashSet<>();
        Set<Class<?>> outputClasses = new HashSet<>();

        //I am sure there is a more elegant way to do this.
        for (Class<?> clazz : plugins) {
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> i : interfaces) {
                if ("org.logstash.skunk.api.plugin.Input".equals(i.getCanonicalName())) {
                    inputClasses.add(clazz);
                } else if ("org.logstash.skunk.api.plugin.Filter".equals(i.getCanonicalName())) {
                    filterClasses.add(clazz);
                } else if ("org.logstash.skunk.api.plugin.Output".equals(i.getCanonicalName())) {
                    outputClasses.add(clazz);
                }
            }
        }

        //Each input gets its own thread
        ExecutorService inputExecutorService = Executors.newFixedThreadPool(inputClasses.size());
        for (Class<?> clazz : inputClasses) {
            Input input = (Input) clazz.newInstance();
            inputExecutorService.execute(() -> input.start(config, writeQueue));
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

        //a thread for each worker
        ExecutorService processingExecutorService = Executors.newFixedThreadPool(workers);
        IntStream.of(workers).forEach(__ -> {
            Processor processor = new Processor(config, filters, outputs, queue);
            processingExecutorService.execute(() -> processor.start());
        });

    }
}
