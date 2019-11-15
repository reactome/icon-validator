package org.reactome.server.tools.model;

import javax.xml.bind.annotation.*;

/**
 * Created by Chuqiao on 2019/11/12.
 */

@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.FIELD)
public class Person {

    @XmlAttribute
    private String role;

    @XmlAttribute
    private String orcid;

    @XmlAttribute
    private String url;

    @XmlValue
    private String name;

    public Person() { }

    public Person(String role, String orcid, String url, String name) {
        this.role = role;
        this.orcid = orcid;
        this.url = url;
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
