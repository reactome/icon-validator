package org.reactome.server.tools.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Chuqiao on 2019/11/12.
 */


@XmlRootElement(name = "references")
@XmlAccessorType(XmlAccessType.FIELD)
public class Reference {

    @XmlElement
    private String db;

    @XmlElement
    private String id;

    public Reference() {
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Reference{" +
                "db='" + db + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
