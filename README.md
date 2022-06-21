# xsd2jsonSchema 
A simple Groovy based program to do convert XSD definitions to JSON schema.

This tool is in a terrible early state, but for me it works :D



## Requirements
* Java 8
* Gradle v4.*
* xjc in JAVA_HOME/bin or in search path


## Handle with gradle
### Using with gradle
```bash
# do a complete release to configured maven repository
gradle publish

# builds a release with all dependencies
# release is built in PROJECT_DIR/build/release
# before a release is build the tests are executed - skip not possible
gradle buildRelease

# run program without any arguments from project
gradle myRun
```
### Usage of the release
After you built a release with gradle or you download a release bundle you can start
the program with the contained start script. If you start it with the help option you
get a full description of the possible parameters
```bash
# or a similar path
cd build/release
# start program in bash environment
./xsd2jsonSchema.sh -x PATH_TO_XSD_FILE -o PATH_TO_OUTPUT_FILE -e ENTRY_TYPE_FOR_SERIALIZATION

# show help in bash environment
./xsd2jsonSchema.sh --help
```

### JAXP tuning
Some XSD schemas define very high values for maxOccurs, which may trigger error messages like this:
`[ERROR] Current configuration of the parser doesn't allow a maxOccurs attribute value to be set greater than the value 5,000`
One can work around this problem by increasing the limit using the property `jdk.xml.maxOccurLimit` via file `{java.home}/jre/lib/jaxp.properties`.
See [JAXP Processing Limit Definitions](https://docs.oracle.com/javase/tutorial/jaxp/limits/limits.html)

```properties
# Path for this file: {java.home}/jre/lib/jaxp.properties
jdk.xml.entityExpansionLimit=64000
jdk.xml.elementAttributeLimit=10000
jdk.xml.maxOccurLimit=5000
jdk.xml.totalEntitySizeLimit=50000000
jdk.xml.maxGeneralEntitySizeLimit=0
jdk.xml.maxParameterEntitySizeLimit=1000000
jdk.xml.entityReplacementLimit=3000000
jdk.xml.maxElementDepth=0
jdk.xml.maxXMLNameLimit=1000
```
