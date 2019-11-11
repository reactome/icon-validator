[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)

# Experiment Digester

## What is the Reactome Experiment Digester

This project is meant to import experiments from other resources (e.g. Gene Expression Atlas, Human Protein Atlas) and allow the Reactome PathwayBrowser to use them through an easy API. 

## Installation & Configuration

#### Requirements 
    1. Maven 3.X - [Installation Guide](http://maven.apache.org/install.html)
    
#### Git Clone
```console
git clone https://github.com/reactome/experiment-digester.git
cd experiment-digester;
```

#### Produce the experiments binary file
To import the experiments and produce the experiments binary file, compile and use the importer tool as follows:

```console
mvn clean compile
```
then create the binary file by using the following line:

```console
java -jar target/digester-importer-jar-with-dependencies.jar
      -o [pathToBinaryFile] \
      -e [comma separated list of experiment urls, optionally with names]
      -n [How empty (null) values are handled, e.g "0.0" will replace an empty value with zeroes]
```
Please note that the pathToBinaryFile refers to the location of the output binary file.

For example: 

```console
java -jar target/digester-importer-jar-with-dependencies.jar \
      -o /Users/home/experiments.bin \
      -e "[HPA (E-PROT-3)]https://www.ebi.ac.uk/gxa/experiments-content/E-PROT-3/resources/ExperimentDownloadSupplier.Proteomics/tsv"
```
The experiments.bin file has to be copied in the corresponding "AnalysisService/digester/" folder

#### Configuring the service
Maven Profile is a set of configuration values which can be used to set or override default values of Maven build. Using a build profile, you can customise build for different environments such as Production v/s Development environments. Add the following code-snippet containing all the Reactome properties inside the tag <profiles> into your ~/.m2/settings.xml. Please refer to Maven Profile Guideline if you don't have settings.xml
```html
<profile>
    <id>Experiment-Digester-Local</id>
    <properties>
        <!-- Path to the experiments binary file -->
        <experiments.data.file>[pathToBinaryFile]</experiments.data.file>
    </properties>
</profile>
```
Please note that the pathToBinaryFile has to point to the same location as mentioned previously (e.g. AnalysisService/digester/experiments.bin)

#### Generating the war file for deployment activating ```Experiment-Digester-Local``` profile
```console
mvn clean package -P Experiment-Digester-Local
```

#### Running the Experiment-Digester Service activating ```Experiment-Digester-Local``` profile
```console
mvn tomcat7:run -P Experiment-Digester-Local
```
