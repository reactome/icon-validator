[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)

# Icon Validator

## What is the Reactome Icon Validator

Reactome improves the graphical representation of higher-level pathways in the Reactome pathway browser with the idea of [icon library](https://reactome.org/icon-info/icons-guidelines), this project is meant to validate the structure and value of each icon metadata file.

#### icon xml
The correct xml file should have the following structure:
```html
<metadata>
    <categories>
        <category>{CATEGORY}</category>
    </categories>
    <person orcid="{ORCID}" role="curator">{CURATOR_NAME}</person>
    <person role="designer" url="{DESIGNER_URL}">{DESIGNER_NAME}</person>
    <name>{ICON_NAME}</name>
    <description>{DESCRIPTION}</description>
    <references>
        <reference>
            <db>{REFERENCE_NAME}</db>
            <id>{REFERENCE_ID}</id>
        </reference>
    </references>
    <synonyms>
        <synonym>{SYNONYM}</synonym>
    </synonyms>
</metadata>
```

This validator validates the meaningful value of significant elements, catch the unmatching end-tags and mistaken attribute specifications, it also generates a validation log file in the end to display all errors. It won't cause any problems after sending the icon files to server.

## Installation & Configuration

#### Requirements 
    1. Maven 3.X - [Installation Guide](http://maven.apache.org/install.html)
     
#### Git Clone
```console
git clone https://github.com/reactome/icon-validator.git 
cd icon-validator
```

#### Usage
To validate icon files and generate a validation log file, package and use the validator tool as follows:

```console
mvn clean package
```
then start validating files by using the following line:

```console
java -jar ./target/icon-validator-jar-with-dependencies.jar
      -d path/to/icon/directory
```
the expected result should be as below:

```console
ERROR - Synonym is missing value in R-ICO-013884.xml.
ERROR - The element type "description" must be terminated by the matching end-tag "</description>". File: R-ICO-013655.xml
ERROR - Category [proteoin] is not in the list CATEGORIES in the R-ICO-013736.xml.
...
```











