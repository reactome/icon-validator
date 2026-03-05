[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)

# Illustration Validator

## What is the Reactome Illustration Validator

Reactome improves the graphical representation of higher-level pathways in the Reactome pathway browser with the idea of [icon library](https://reactome.org/icon-info/icons-guidelines) and EHLD diagrams. This project validates the structure and value of each icon XML metadata file and the SVG structure of EHLD diagrams.

### Icon XML Validation

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
* {CATEGORY} with one or more of the suggested categories: arrow, cell_element, cell_type, compound, human_tissue, protein, receptor and transporter.
* {REFERENCE_NAME} for the name of the resource you have used to find this component (e.g.: UNIPROT, GO, CHEBI … ).
* {SYNONYM} as any synonyms or alternative names your component might be known of.

### EHLD SVG Validation

EHLD SVGs are validated for correct structure:
* Filename matches `R-HSA-[numbers].svg`
* `BG` group exists with a `LOGO` inside it
* At least one `REGION-R-HSA-[numbers]` group exists
* Each `REGION` has a matching `OVERLAY-R-HSA-[numbers]` inside it
* `FG` group is optional (warning if missing)

## Installation & Usage

#### Requirements
    1. Maven 3.X - [Installation Guide](http://maven.apache.org/install.html)

#### Git Clone
```console
git clone https://github.com/reactome/illustration-validator.git
cd illustration-validator
```

#### Usage
To validate icon files and generate a validation log file, package and use the validator tool as follows:

```console
mvn clean package
```
then start validating files by using the following line:

```console
java -jar ./target/illustration-validator-jar-with-dependencies.jar
      -d path/to/icon/directory
      -s path/to/ehld/directory
      -r references.txt
      -c categories.txt
```
the expected result should be as below:

```console
ERROR - Element "synonym" is missing value in R-ICO-013884.xml.
ERROR - The element type "description" must be terminated by the matching end-tag "</description>". File: R-ICO-013655.xml
ERROR - [proteoin] at the element "category" is not in the list CATEGORIES in the R-ICO-013736.xml.
ERROR - EHLD 'R-HSA-9735786.svg': missing 'LOGO' inside 'BG' group
...
```
