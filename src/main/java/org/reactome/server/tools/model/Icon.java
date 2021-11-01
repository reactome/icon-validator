package org.reactome.server.tools.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by Chuqiao on 2019/11/11.
 */

@XmlRootElement(name = "metadata")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor
public class Icon {

    @XmlElementWrapper(name = "categories")
    @XmlElement(name = "category")
    private List<String> categories;

    @XmlElement
    private List<Person> person;

    @XmlElement
    private String name;

    @XmlElement
    private String description;

    @XmlElement
    private String info;

    @XmlElementWrapper(name = "references")
    @XmlElement(name = "reference")
    private List<Reference> references;

    @XmlElementWrapper(name = "synonyms")
    @XmlElement(name = "synonym")
    private List<String> synonyms;

    @XmlElement
    private boolean skip;
}
