package unity.annotations;

import arc.func.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.pooling.Pool.*;
import unity.annotations.Annotations.*;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

import com.squareup.javapoet.*;
import com.sun.source.tree.*;

/**
 * @author Anuke
 * @author GlennFolker
 */
@SuppressWarnings("unchecked")
@SupportedAnnotationTypes({
    "unity.annotations.Annotations.EntityComponent",
    "unity.annotations.Annotations.EntityBaseComponent",
    "unity.annotations.Annotations.EntityDef"
})
public class EntityProcessor extends BaseProcessor{
    Seq<TypeElement> comps = new Seq<>();
    Seq<TypeElement> baseComps = new Seq<>();
    ObjectMap<String, TypeElement> compNames = new ObjectMap<>();
    Seq<TypeSpec.Builder> baseClasses = new Seq<>();
    ObjectMap<TypeElement, ObjectSet<TypeElement>> baseClassDeps = new ObjectMap<>();
    ObjectMap<TypeElement, Seq<TypeElement>> componentDependencies = new ObjectMap<>();
    Seq<TypeElement> inters = new Seq<>();
    Seq<Element> defs = new Seq<>();
    Seq<EntityDefinition> definitions = new Seq<>();

    StringMap varInitializers = new StringMap();
    StringMap methodBlocks = new StringMap();
    ObjectMap<String, Seq<String>> imports = new ObjectMap<>();
    ObjectMap<TypeElement, String> groups;

    {
        rounds = 3;
    }

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception{
        comps.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(EntityComponent.class));
        baseComps.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(EntityBaseComponent.class));
        inters.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(EntityInterface.class));
        defs.addAll(roundEnv.getElementsAnnotatedWith(EntityDef.class));

        if(round == 1){
            groups = ObjectMap.of(
                /*toComp(Entityc.class), "all",
                toComp(Playerc.class), "player",
                toComp(Bulletc.class), "bullet",
                toComp(Unitc.class), "unit",
                toComp(Buildingc.class), "build",
                toComp(Syncc.class), "sync",
                toComp(Drawc.class), "draw",
                toComp(Firec.class), "fire",
                toComp(Puddlec.class), "puddle",
                toComp(WeatherStatec.class), "weather"*/
            );

            for(TypeElement inter : (List<TypeElement>)((PackageElement)elementUtils.getPackageElement("mindustry.gen")).getEnclosedElements()){
                if(
                    simpleName(inter).endsWith("c") &&
                    inter.getKind() == ElementKind.INTERFACE
                ){
                    inters.add(inter);
                }
            }

            for(TypeElement comp : comps){
                for(ExecutableElement m : methods(comp)){
                    if(is(m, Modifier.ABSTRACT, Modifier.NATIVE)) continue;

                    methodBlocks.put(descString(m), treeUtils.getTree(m).getBody().toString()
                        .replaceAll("this\\.<(.*)>self\\(\\)", "this")
                        .replaceAll("self\\(\\)(?!\\s+instanceof)", "this")
                        .replaceAll(" yield ", "")
                        .replaceAll("\\/\\*missing\\*\\/", "var")
                    );
                }

                for(VariableElement var : vars(comp)){
                    VariableTree tree = (VariableTree)treeUtils.getTree(var);
                    if(tree.getInitializer() != null){
                        varInitializers.put(descString(var), tree.getInitializer().toString());
                    }
                }

                imports.put(interfaceName(comp), getImports(comp));
                compNames.put(simpleName(comp), comp);

                Seq<TypeElement> depends = getDependencies(comp);

                EntityComponent compAnno = annotation(comp, EntityComponent.class);
                if(compAnno.write()){
                    TypeSpec.Builder inter = TypeSpec.interfaceBuilder(interfaceName(comp))
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(EntityInterface.class);

                    for(TypeElement type : depends){
                        inter.addSuperinterface(procName(type, this::interfaceName));
                    }

                    ObjectSet<String> preserved = new ObjectSet<>();
                    for(ExecutableElement m : methods(comp).select(me -> !isConstructor(me) && !is(me, Modifier.PRIVATE, Modifier.STATIC))){
                        String name = simpleName(m);
                        preserved.add(m.toString());

                        if(annotation(m, Override.class) == null){
                            inter.addMethod(
                                MethodSpec.methodBuilder(name)
                                    .addTypeVariables(Seq.with(m.getTypeParameters()).map(TypeVariableName::get))
                                    .addExceptions(Seq.with(m.getThrownTypes()).map(TypeName::get))
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .addParameters(Seq.with(m.getParameters()).map(ParameterSpec::get))
                                    .returns(TypeName.get(m.getReturnType()))
                                .build()
                            );
                        }
                    }

                    for(VariableElement var : vars(comp).select(v -> !is(v, Modifier.STATIC) && !is(v, Modifier.PRIVATE) && annotation(v, Import.class) == null)){
                        String name = simpleName(var);

                        if(!preserved.contains(name + "()")){
                            inter.addMethod(
                                MethodSpec.methodBuilder(name)
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .addAnnotation(Getter.class)
                                    .returns(tName(var))
                                .build()
                            );
                        }

                        if(
                            !is(var, Modifier.FINAL) &&
                            !preserved.contains(name + "(" + var.asType().toString() + ")") &&
                            annotation(var, ReadOnly.class) == null
                        ){
                            inter.addMethod(
                                MethodSpec.methodBuilder(name)
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .addParameter(tName(var), name)
                                    .addAnnotation(Setter.class)
                                    .returns(TypeName.VOID)
                                .build()
                            );
                        }
                    }

                    write(inter.build());

                    if(compAnno.base()){
                        Seq<TypeElement> deps = depends.copy().and(comp);
                        baseClassDeps.get(comp, ObjectSet::new).addAll(deps);

                        if(annotation(comp, EntityDef.class) == null){
                            TypeSpec.Builder base = TypeSpec.classBuilder(baseName(comp)).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

                            for(TypeElement dep : deps){
                                for(VariableElement var : vars(dep).select(v -> !is(v, Modifier.PRIVATE) && !is(v, Modifier.STATIC) && annotation(v, Import.class) == null && annotation(v, ReadOnly.class) == null)){
                                    FieldSpec.Builder field = FieldSpec.builder(tName(var), simpleName(var), Modifier.PUBLIC);

                                    if(is(var, Modifier.TRANSIENT)) field.addModifiers(Modifier.TRANSIENT);
                                    field.addAnnotations(Seq.with(var.getAnnotationMirrors()).map(AnnotationSpec::get));

                                    if(varInitializers.containsKey(descString(var))){
                                        field.initializer(varInitializers.get(descString(var)));
                                    }

                                    base.addField(field.build());
                                }

                                base.addSuperinterface(procName(dep, this::interfaceName));
                            }

                            baseClasses.add(base);
                        }
                    }
                }else if(compAnno.base()){
                    Seq<TypeElement> deps = depends.copy().and(comp);
                    baseClassDeps.get(comp, ObjectSet::new).addAll(deps);
                }
            }
        }else if(round == 2){
            ObjectMap<String, Element> usedNames = new ObjectMap<>();
            ObjectMap<Element, ObjectSet<String>> extraNames = new ObjectMap<>();

            for(Element def : defs){
                EntityDef ann = annotation(def, EntityDef.class);

                Seq<TypeElement> defComps = elements(ann::value)
                    .<TypeElement>as()
                    .map(t -> inters.find(i -> simpleName(i).equals(simpleName(t))))
                    .select(t -> t != null)
                    .map(this::toComp);

                if(defComps.isEmpty()) continue;

                Seq<String> defGroups = groups.values().toSeq().select(name -> defComps.contains(groups.findKey(name, false)));

                ObjectMap<String, Seq<ExecutableElement>> methods = new ObjectMap<>();
                ObjectMap<FieldSpec, VariableElement> specVariables = new ObjectMap<>();
                ObjectSet<String> usedFields = new ObjectSet<>();

                Seq<TypeElement> baseClasses = defComps.select(s -> annotation(s, EntityComponent.class).base());
                if(baseClasses.size > 2){
                    throw new IllegalStateException("No entity may have more than 2 base classes.");
                }

                TypeElement baseClassType = baseClasses.any() ? baseClasses.first() : null;
                TypeName baseClass = baseClasses.any()
                ?   procName(baseClassType, this::baseName)
                :   null;

                boolean typeIsBase = baseClassType != null && annotation(def, EntityComponent.class) != null && annotation(def, EntityComponent.class).base();

                if(def instanceof TypeElement && !simpleName(def).endsWith("Comp")){
                    throw new IllegalStateException("All entity def names must end with 'Comp'");
                }

                String name = def instanceof TypeElement ?
                    simpleName(def).replace("Comp", "") :
                    createName(defComps);

                defComps.addAll(defComps.copy().flatMap(this::getDependencies)).distinct();

                if(!typeIsBase && baseClass != null && name.equals(baseName(baseClassType))){
                    name += "Entity";
                }

                if(usedNames.containsKey(name)){
                    extraNames.get(usedNames.get(name), ObjectSet::new).add(simpleName(def));
                    continue;
                }

                TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(
                        AnnotationSpec.builder(SuppressWarnings.class)
                            .addMember("value", "$S", "all")
                        .build()
                    );

                builder.addMethod(
                    MethodSpec.methodBuilder("serialize").addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(TypeName.BOOLEAN)
                        .addStatement("return " + ann.serialize())
                    .build()
                );

                Seq<VariableElement> syncedFields = new Seq<>();
                Seq<VariableElement> allFields = new Seq<>();
                Seq<FieldSpec> allFieldSpecs = new Seq<>();

                boolean isSync = defComps.contains(s -> simpleName(s).contains("Sync"));

                for(TypeElement comp : defComps){
                    boolean isShadowed = baseClass != null && !typeIsBase && baseClassDeps.get(baseClassType, ObjectSet::new).contains(comp);

                    Seq<VariableElement> fields = vars(comp).select(v -> annotation(v, Import.class) == null);
                    for(VariableElement field : fields){
                        if(!usedFields.add(simpleName(field))){
                            throw new IllegalStateException("Field '" + simpleName(field) + "' of component '" + simpleName(comp) + "' redefines a field in entity '" + simpleName(def) + "'");
                        }

                        FieldSpec.Builder fbuilder = FieldSpec.builder(tName(field), simpleName(field));

                        if(is(field, Modifier.STATIC)){
                            fbuilder.addModifiers(Modifier.STATIC);
                            if(is(field, Modifier.FINAL)) fbuilder.addModifiers(Modifier.FINAL);
                        }

                        if(is(field, Modifier.TRANSIENT)){
                            fbuilder.addModifiers(Modifier.TRANSIENT);
                        }

                        if(varInitializers.containsKey(descString(field))){
                            fbuilder.initializer(varInitializers.get(descString(field)));
                        }

                        fbuilder.addModifiers(annotation(field, ReadOnly.class) != null ? Modifier.PROTECTED : Modifier.PUBLIC);
                        fbuilder.addAnnotations(Seq.with(field.getAnnotationMirrors()).map(AnnotationSpec::get));
                        FieldSpec spec = fbuilder.build();

                        boolean isVisible = !is(field, Modifier.STATIC) && !is(field, Modifier.PRIVATE) && annotation(field, ReadOnly.class) == null;

                        if(!isShadowed || !isVisible){
                            builder.addField(spec);
                        }

                        specVariables.put(spec, field);

                        allFieldSpecs.add(spec);
                        allFields.add(field);

                        if(annotation(field, SyncField.class) != null && isSync){
                            if(field.asType().getKind() != TypeKind.FLOAT) throw new IllegalStateException("All SyncFields must be of type float");

                            syncedFields.add(field);
                            builder.addField(FieldSpec.builder(TypeName.FLOAT, simpleName(field) + "_TARGET_").addModifiers(Modifier.TRANSIENT, Modifier.PRIVATE).build());
                            builder.addField(FieldSpec.builder(TypeName.FLOAT, simpleName(field) + "_LAST_").addModifiers(Modifier.TRANSIENT, Modifier.PRIVATE).build());
                        }
                    }

                    for(ExecutableElement elem : methods(comp).select(m -> !isConstructor(m))){
                        methods.get(elem.toString(), Seq::new).add(elem);
                    }
                }

                syncedFields.sortComparing(e -> simpleName(e));

                builder.addMethod(
                    MethodSpec.methodBuilder("toString")
                        .addAnnotation(Override.class)
                        .returns(String.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("return $S + $L", name + "#", "id")
                    .build()
                );

                boolean hasIO = ann.genio() && (defComps.contains(s -> simpleName(s).contains("Sync")) || ann.serialize());

                for(Entry<String, Seq<ExecutableElement>> entry : methods){
                    if(entry.value.contains(m -> annotation(m, Replace.class) != null)){
                        if(entry.value.first().getReturnType().getKind() == TypeKind.VOID){
                            entry.value = entry.value.select(m -> annotation(m, Replace.class) != null);
                        }else{
                            if(entry.value.count(m -> annotation(m, Replace.class) != null) > 1){
                                throw new IllegalStateException("Type " + simpleName(def) + " has multiple components replacing non-void method " + entry.key + ".");
                            }
    
                            ExecutableElement base = entry.value.find(m -> annotation(m, Replace.class) != null);
                            entry.value.clear();
                            entry.value.add(base);
                        }
                    }

                    if(entry.value.count(m -> !is(m, Modifier.NATIVE, Modifier.ABSTRACT) && m.getReturnType().getKind() != TypeKind.VOID) > 1){
                        throw new IllegalStateException("Type " + simpleName(def) + " has multiple components implementing non-void method " + entry.key + ".");
                    }

                    entry.value.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(e -> simpleName(e))));

                    ExecutableElement first = entry.value.first();

                    if(annotation(first, InternalImpl.class) != null){
                        continue;
                    }

                    boolean isPrivate = is(first, Modifier.PRIVATE);
                    MethodSpec.Builder mbuilder = MethodSpec.methodBuilder(simpleName(first)).addModifiers(isPrivate ? Modifier.PRIVATE : Modifier.PUBLIC);
                    if(!isPrivate) mbuilder.addAnnotation(Override.class);

                    if(is(first, Modifier.STATIC)) mbuilder.addModifiers(Modifier.STATIC);
                    mbuilder.addTypeVariables(Seq.with(first.getTypeParameters()).map(TypeVariableName::get));
                    mbuilder.returns(TypeName.get(first.getReturnType()));
                    mbuilder.addExceptions(Seq.with(first.getThrownTypes()).map(TypeName::get));

                    for(VariableElement var : first.getParameters()){
                        mbuilder.addParameter(tName(var), simpleName(var));
                    }

                    boolean writeBlock = first.getReturnType().getKind() == TypeKind.VOID && entry.value.size > 1;

                    if((is(entry.value.first(), Modifier.ABSTRACT) || is(entry.value.first(), Modifier.NATIVE)) && entry.value.size == 1 && annotation(entry.value.first(), InternalImpl.class) == null){
                        throw new IllegalStateException(simpleName(entry.value.first().getEnclosingElement()) + "#" + entry.value.first() + " is an abstract method and must be implemented in some component");
                    }

                    if(simpleName(first).equals("add") || simpleName(first).equals("remove")){
                        mbuilder.addStatement("if(added == $L) return", simpleName(first).equals("add"));

                        for(String group : defGroups){
                            mbuilder.addStatement("Groups.$L.$L(this)", group, simpleName(first));
                        }
                    }

                    if(hasIO){
                        if(simpleName(first).equals("read") || simpleName(first).equals("write")){
                            //io.write(mbuilder, simpleName(first).equals("write"));
                        }

                        if(simpleName(first).equals("readSync") || simpleName(first).equals("writeSync")){
                            //io.writeSync(mbuilder, simpleName(first).equals("writeSync"), syncedFields, allFields);
                        }

                        if(simpleName(first).equals("readSyncManual") || simpleName(first).equals("writeSyncManual")){
                            //io.writeSyncManual(mbuilder, simpleName(first).equals("writeSyncManual"), syncedFields);
                        }

                        if(simpleName(first).equals("snapSync")){
                            mbuilder.addStatement("updateSpacing = 16");
                            mbuilder.addStatement("lastUpdated = $T.millis()", Time.class);
                            for(VariableElement field : syncedFields){
                                mbuilder.addStatement("$L = $L", simpleName(field) + "_LAST_", simpleName(field) + "_TARGET_");
                                mbuilder.addStatement("$L = $L", simpleName(field), simpleName(field) + "_TARGET_");
                            }
                        }

                        if(simpleName(first).equals("snapInterpolation")){
                            mbuilder.addStatement("updateSpacing = 16");
                            mbuilder.addStatement("lastUpdated = $T.millis()", Time.class);
                            for(VariableElement field : syncedFields){
                                mbuilder.addStatement("$L = $L", simpleName(field) + "_LAST_", simpleName(field));
                                mbuilder.addStatement("$L = $L", simpleName(field) + "_TARGET_", simpleName(field));
                            }
                        }
                    }

                    boolean firstc = true;
                    for(ExecutableElement elem : entry.value){
                        if(!firstc) mbuilder.addCode(lnew());
                        firstc = false;

                        String descStr = descString(elem);

                        if(is(elem, Modifier.ABSTRACT) || is(elem, Modifier.NATIVE) || !methodBlocks.containsKey(descStr)) continue;

                        String blockName = simpleName(elem.getEnclosingElement()).toLowerCase().replace("comp", "");
                        String str = methodBlocks.get(descStr);
                        str = str.substring(1, str.length() - 1).trim().replace("\n    ", "\n");
                        str += '\n';

                        if(writeBlock){
                            str = str.replace("return;", "break " + blockName + ";");

                            if(str.isBlank()) continue;
                            mbuilder.beginControlFlow("$L:", blockName);
                        }

                        mbuilder.addCode(str);

                        if(writeBlock) mbuilder.endControlFlow();
                    }

                    if(simpleName(first).equals("remove") && ann.pooled()){
                        mbuilder.addStatement("mindustry.gen.Groups.queueFree(($T)this)", Poolable.class);
                    }

                    builder.addMethod(mbuilder.build());
                }

                if(ann.pooled()){
                    builder.addSuperinterface(Poolable.class);

                    MethodSpec.Builder resetBuilder = MethodSpec.methodBuilder("reset")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class);

                    for(FieldSpec spec : allFieldSpecs){
                        VariableElement variable = specVariables.get(spec);
                        if(variable != null && is(variable, Modifier.STATIC, Modifier.FINAL)) continue;
                        String desc = descString(variable);

                        if(spec.type.isPrimitive()){
                            resetBuilder.addStatement("$L = $L", spec.name, variable != null && varInitializers.containsKey(desc) ? varInitializers.get(desc) : getDefault(spec.type.toString()));
                        }else{
                            if(!varInitializers.containsKey(desc)){
                                resetBuilder.addStatement("$L = null", spec.name);
                            }
                        }
                    }

                    builder.addMethod(resetBuilder.build());
                }

                builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PROTECTED).build());

                builder.addMethod(
                    MethodSpec.methodBuilder("create").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(packageName, name))
                        .addStatement(ann.pooled() ? "return Pools.obtain($L.class, " + name + "::new)" : "return new $L()", name)
                    .build()
                );

                definitions.add(new EntityDefinition(packageName + "." + name, builder, def, typeIsBase ? null : baseClass, defComps, defGroups, allFieldSpecs));
            }
        }else if(round == 3){
            for(TypeSpec.Builder b : baseClasses){
                TypeSpec spec = b.build();
                write(spec, imports.get(spec.name));
            }

            for(EntityDefinition def : definitions){
                ObjectSet<String> methodNames = def.components.flatMap(type -> methods(type).map(this::simpleString)).asSet();

                if(def.extend != null){
                    def.builder.superclass(def.extend);
                }

                for(TypeElement comp : def.components){
                    TypeElement inter = inters.find(i -> simpleName(i).equals(interfaceName(comp)));
                    if(inter == null){
                        throw new IllegalStateException("Failed to generate interface for " + simpleName(comp));
                    }

                    def.builder.addSuperinterface(cName(inter));

                    for(ExecutableElement method : methods(inter)){
                        String var = simpleName(method);
                        FieldSpec field = Seq.with(def.fieldSpecs).find(f -> f.name.equals(var));

                        if(field == null || methodNames.contains(simpleString(method))) continue;

                        if(method.getReturnType().getKind() != TypeKind.VOID){
                            def.builder.addMethod(
                                MethodSpec.methodBuilder(var).addModifiers(Modifier.PUBLIC)
                                    .returns(TypeName.get(method.getReturnType()))
                                    .addAnnotation(Override.class)
                                    .addStatement("return $L", var)
                                .build()
                            );
                        }

                        if(method.getReturnType().getKind() == TypeKind.VOID && !Seq.with(field.annotations).contains(f -> f.type.toString().equals("@unity.annotations.Annotations.ReadOnly"))){
                            def.builder.addMethod(
                                MethodSpec.methodBuilder(var).addModifiers(Modifier.PUBLIC)
                                    .returns(TypeName.VOID)
                                    .addAnnotation(Override.class)
                                    .addParameter(field.type, var)
                                    .addStatement("this.$L = $L", var, var)
                                .build()
                            );
                        }
                    }
                }

                write(def.builder.build(), def.components.flatMap(comp -> imports.get(interfaceName(comp))));
            }
        }
    }

    Seq<TypeElement> getDependencies(TypeElement component){
        if(!componentDependencies.containsKey(component)){
            ObjectSet<TypeElement> out = new ObjectSet<>();

            out.addAll(Seq.with(component.getInterfaces())
                .map(this::toEl)
                .<TypeElement>as()
                .map(t -> inters.find(i -> simpleName(t).equals(simpleName(i))))
                .select(t -> t != null)
                .map(this::toComp)
            );

            out.remove(component);

            ObjectSet<TypeElement> result = new ObjectSet<>();
            for(TypeElement type : out){
                result.add(type);
                result.addAll(getDependencies(type));
            }

            if(annotation(component, EntityBaseComponent.class) == null){
                result.addAll(baseComps);
            }

            out.remove(component);
            componentDependencies.put(component, result.asArray());
        }

        return componentDependencies.get(component);
    }

    TypeName procName(TypeElement comp, Func<TypeElement, String> name){
        return ClassName.get(
            comp.getEnclosingElement().toString().contains("fetched") ? "mindustry.gen" : packageName,
            name.get(comp)
        );
    }

    TypeElement toComp(TypeElement inter){
        String name = simpleName(inter);
        if(!name.endsWith("c")) return null;

        return comps.find(t -> simpleName(t).equals(
            name.substring(0, name.length() - 1) + "Comp"
        ));
    }

    TypeElement toComp(Class<?> inter){
        return toComp(elementUtils.getTypeElement(inter.getCanonicalName()));
    }

    String interfaceName(TypeElement type){
        return baseName(type) + "c";
    }

    String baseName(TypeElement type){
        String name = simpleName(type);
        if(!name.endsWith("Comp")){
            throw new IllegalStateException("All types annotated with @EntityComp must have 'Comp' as the name's suffix");
        }

        return name.substring(0, name.length() - 4);
    }

    String createName(Seq<TypeElement> comps){
        comps.sortComparing(e -> simpleName(e));
        return comps.toString("", s -> simpleName(s).replace("Comp", ""));
    }

    Seq<String> getImports(Element e){
        return Seq.with(treeUtils.getPath(e).getCompilationUnit().getImports()).map(Object::toString);
    }

    class EntityDefinition{
        final Seq<String> groups;
        final Seq<TypeElement> components;
        final Seq<FieldSpec> fieldSpecs;
        final TypeSpec.Builder builder;
        final Element naming;
        final String name;
        final @Nullable TypeName extend;
        int classID;

        public EntityDefinition(String name, TypeSpec.Builder builder, Element naming, TypeName extend, Seq<TypeElement> components, Seq<String> groups, Seq<FieldSpec> fieldSpec){
            this.builder = builder;
            this.name = name;
            this.naming = naming;
            this.groups = groups;
            this.components = components;
            this.extend = extend;
            this.fieldSpecs = fieldSpec;
        }

        @Override
        public String toString(){
            return "Definition{" +
            "groups=" + groups +
            "components=" + components +
            ", base=" + naming +
            '}';
        }
    }
}
