[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)

# Icon Validator

## What is the Reactome Icon Validator

This project is meant to validate icon files before sending them to the server.

## Installation & Configuration

#### Requirements 
    1. Maven 3.X - [Installation Guide](http://maven.apache.org/install.html)
    
#### Git Clone
```console
git clone https://github.com/reactome/experiment-digester.git 
cd experiment-digester
```

#### Usage
To validate icon files and generate a validation log file, package and use the validator tool as follows:

```console
mvn clean package
```
then start validating files by using the following line:

```console
java -jar target/icon-validator-jar-with-dependencies.jar
      -d path/To/icon/directory
```






