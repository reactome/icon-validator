package org.reactome.server.tools.model;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by Chuqiao on 2019/11/11.
 */

@XmlRootElement(name = "metadata")
@XmlAccessorType(XmlAccessType.FIELD)
public class Icon {

    @XmlElementWrapper(name="categories")
    @XmlElement(name="category")
    private List<String> categories;

    @XmlElement
    private List<Person> person;

    @XmlElement
    private String name;

    @XmlElement
    private String description;

    @XmlElement
    private String info;

    @XmlElementWrapper(name="references")
    @XmlElement(name="reference")
    private List<Reference> references;

    @XmlElementWrapper(name="synonyms")
    @XmlElement(name="synonym")
    private List<String> synonyms;

    @XmlElement
    private  boolean skip;

    public Icon() { }

    public Icon(List<String> categories, List<Person> person, String name, String description, String info, List<Reference> references, List<String> synonyms, boolean skip) {
        this.categories = categories;
        this.person = person;
        this.name = name;
        this.description = description;
        this.info = info;
        this.references = references;
        this.synonyms = synonyms;
        this.skip = skip;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<Person> getPerson() {
        return person;
    }

    public void setPerson(List<Person> person) {
        this.person = person;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    @Override
    public String toString() {
        return "Icon{" +
                "categories=" + categories +
                ", person=" + person +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", info='" + info + '\'' +
                ", references=" + references +
                ", synonyms=" + synonyms +
                ", skip=" + skip +
                '}';
    }
}
