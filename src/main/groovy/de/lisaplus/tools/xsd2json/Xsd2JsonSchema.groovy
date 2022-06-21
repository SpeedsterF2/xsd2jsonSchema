package de.lisaplus.tools.xsd2json

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Created by eiko on 10.07.17.
 */
class Xsd2JsonSchema {
    @Parameter(names = [ '-x', '--xsd' ], description = "Path to XSD schema to parse", required = true)
    String model

    @Parameter(names = [ '-o', '--output' ], description = "Path to the output file", required = true)
    String outputFilePath

    @Parameter(names = [ '-e', '--entryType' ], description = "What is the entry type for the model", required = true)
    String entryType

    @Parameter(names = ['-h','--help'], help = true)
    boolean help = false

    static void main(String ... args) {
        Xsd2JsonSchema xsd2JsonSchema = new Xsd2JsonSchema()
        try {
            JCommander jCommander = JCommander.newBuilder()
                    .addObject(xsd2JsonSchema)
                    .build()
            jCommander.setProgramName(xsd2JsonSchema.getClass().typeName)
            jCommander.parse(args)
            if (xsd2JsonSchema.help) {
                jCommander.usage()
                return
            }
            xsd2JsonSchema.run()
        }
        catch(ParameterException e) {
            e.usage()
        } catch(Exception e) {
            e.printStackTrace()
        }
        
    }

    void run() {
        File modelFile = new File (model)
        if (!modelFile.exists()) {
            def errorMsg = "XSD does not exist: '${modelFile}'"
            log.error(errorMsg)
            throw new Exception(errorMsg)
        }
        if (!modelFile.isFile()) {
            def errorMsg = "XSD is no normal file: '${modelFile}'"
            log.error(errorMsg)
            throw new Exception(errorMsg)
        }
        log.info("xsd=${model}")
        log.info("outPutBase=${outputFilePath}")
        def xjcCommandString = getXjcCommandString()
        def javacCommandString = getJavacCommandString()
        def classFileBasePath = generateJavaClassesFromXsd(xjcCommandString)
        def generatedClassPath = compileGenerated(classFileBasePath,javacCommandString)
        final GroovyClassLoader classLoader = new GroovyClassLoader()
        classLoader.addClasspath(generatedClassPath)
        classToSchema(classLoader,entryType,outputFilePath)
    }

    private static void classToSchema(def classLoader, def className, def outputFilePath) {
        Class c = classLoader.loadClass(className)
        ObjectMapper mapper = new ObjectMapper()
        def customMapping = new HashMap<String, String>();
        customMapping.put("java.time.ZonedDateTime", "date-time")
        customMapping.put("java.time.LocalDate", "date")

        def classTypeRemapping = new HashMap<Class, Class>()
        classTypeRemapping.put(ZonedDateTime.class, String.class);
        classTypeRemapping.put(LocalDate.class, String.class);

        JsonSchemaConfig config = JsonSchemaConfig.create(
                false, // autoGenerateTitleForProperties
                Optional.empty(), // defaultArrayFormat
                false,  // useOneOfForOption
                false,  // useOneOfForNullables
                false,   // usePropertyOrdering
                false, // hidePolymorphismTypeProperty
                false, // disableWarnings
                false, // useMinLengthForNotNull
                false, // useTypeIdForDefinitionName
                customMapping, // customType2FormatMapping, Map<String, String>
                false, // useMultipleEditorSelectViaProperty
                new HashSet<>(), // uniqueItemClasses, Set<Class>
                new HashMap<>(), // classTypeReMapping, Map<Class, Class>
                new HashMap<>(), // jsonSuppliers, Map<String, Supplier<JsonNode>>
                null, // SubclassesResolver
                false, // failOnUnknownProperties
                null // javaxValidationGroups, List<Class>
        )
        /*
        JsonSchemaConfig config = JsonSchemaConfig.vanillaJsonSchemaDraft4()
        def customMapping = config.customType2FormatMapping()
        customMapping.updated("java.time.ZonedDateTime", "date-time")
        customMapping.updated("java.time.LocalDate", "date")
         */
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper,true,config)
        JsonNode schema = schemaGen.generateJsonSchema(c);
        def objectMapper = new ObjectMapper()
        def writer = new ObjectMapper().writer().withFeatures(SerializationFeature.INDENT_OUTPUT)
        String s = writer.writeValueAsString(schema)
        def outputFile = new File(outputFilePath)
        outputFile.write(s)
        log.info('Done writing JSON schema to {}', outputFile.getAbsolutePath())
    }


    static String getCommandString(def commandStr,def msgPrefix) {
        def command = [commandStr,'-version']
        try {
            def process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
            process.waitFor()
            return commandStr
        }
        catch (IOException e) {
            log.info ("${msgPrefix} in searchPath")
            String javaHomePath = System.getenv('JAVA_HOME')
            if (javaHomePath) {
                commandStr = javaHomePath+'/bin/'+commandStr
                command = [commandStr,'-version']
                try {
                    def process = new ProcessBuilder(command)
                            .redirectErrorStream(true)
                            .start()
                    process.waitFor()
                    return commandStr
                }
                catch(IOException e2) {
                    def errorMsg = "${msgPrefix} in JAVA_HOME/bin"
                    log.error(errorMsg)
                    throw new Exception(errorMsg)
                }
            }
            else {
                def errorMsg = "${msgPrefix}, maybe it helps to set JAVA_HOME"
                log.error(errorMsg)
                throw new Exception(errorMsg)
            }
        }
    }

    static String getXjcCommandString() {
        return getCommandString('xjc',"can't find JAXB XML schema compiler 'xjc'")
    }


    static String getJavacCommandString() {
        return getCommandString('javac',"can't find Java compiler 'javac'")
    }


    String generateJavaClassesFromXsd (String xjcCommand) {
        File f = File.createTempDir()
        String pathToGenerate = f.getCanonicalPath()
        log.info ("create Java classes from XSD here: "+pathToGenerate)
        //def command = [xjcCommand,'-npa','-p','','-d',f.getCanonicalPath(),pathToGenerate,model]
        def command = [xjcCommand,'-npa','-p','','-d',f.getCanonicalPath(),'-b','src/main/resources/globalBindings.xml',pathToGenerate,model]
        println "executing command ${command.join(' ')}"
        def process = new ProcessBuilder(command).start()
        process.errorStream.eachLine {
            log.error(it)
        }
        process.inputStream.eachLine {
            log.info(it)
        }
        process.waitFor()
        if (process.exitValue()!=0) {
            def errorMsg = 'error while generate Java classes from XSD'
            log.error(errorMsg)
            throw new Exception(errorMsg)
        }
        log.info('Done writing Java classes to {}', f.getCanonicalPath())
        return f.getCanonicalPath()
    }

    static String compileGenerated(def sourceFileDir,String javacCommand) {
        File f = File.createTempDir()
        copyConverters(sourceFileDir)
        String pathToGenerate = f.getCanonicalPath()
        log.info("class path for compiled Java stuff: $pathToGenerate")
        def command = [javacCommand,'-d',f.getCanonicalPath(),'-source','1.8']


        def src = new File(sourceFileDir)
        src.eachFile { file ->
            command.add(file.getCanonicalPath())
        }

        println "executing command ${command.join(' ')}"

        def process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        process.inputStream.eachLine {
            log.info(it)
        }
        process.waitFor();
        if (process.exitValue()!=0) {
            def errorMsg = 'error while compile Java classes'
            log.error(errorMsg)
            throw new Exception(errorMsg)
        }
        log.info('Done compiling Java classes from {}', src.getCanonicalPath())
        return pathToGenerate
    }

    private static void copyConverters(def tmpDir) throws IOException {
        def converters = ['XsdDateConverter.java', 'XsdDateTimeConverter.java']
        def srcDir = Paths.get('src/main/java')
        def destDir = Paths.get(tmpDir)
        for (def converter : converters) {
            Files.copy(srcDir.resolve(converter), destDir.resolve(converter) )
        }
    }

    private static final Logger log=LoggerFactory.getLogger(Xsd2JsonSchema.class)

}
