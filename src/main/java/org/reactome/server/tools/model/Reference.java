package org.reactome.server.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

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
