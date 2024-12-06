package org.reactome.server.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Chuqiao on 2019/11/12.
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
@XmlRootElement(name = "references")
@XmlAccessorType(XmlAccessType.FIELD)
public class Reference {

    @XmlElement
    private String db;

    @XmlElement
    private String id;
}
