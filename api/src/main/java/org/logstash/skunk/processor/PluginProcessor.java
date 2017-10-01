package org.logstash.skunk.processor;

import org.logstash.skunk.api.plugin.Plugin;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

import static javax.lang.model.SourceVersion.RELEASE_8;

@SupportedSourceVersion(RELEASE_8)
@SupportedAnnotationTypes("org.logstash.skunk.api.plugin.Plugin")
public class PluginProcessor extends AbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "HERE!!!!!!");



        for (Element element : roundEnv.getElementsAnnotatedWith(Plugin.class)) {

            if (element.getKind() != ElementKind.CLASS) {

                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Plugin must be applied to a class");

                return false;
            }

//            VariableElement configItem = (VariableElement) element;
//            String classType = configItem.asType().toString();
//            String configItemKey = configItem.getAnnotationMirrors().get(0).getElementValues().values().iterator().next().toString();
//
//            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, configItem.getSimpleName()+ " of type " + classType + " with key " + configItemKey);

        }
        return true;
    }

}
