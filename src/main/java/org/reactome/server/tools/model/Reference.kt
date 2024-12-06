package org.reactome.server.tools.model

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

/**
 * Created by Chuqiao on 2019/11/12.
 */
@XmlRootElement(name = "references")
@XmlAccessorType(XmlAccessType.FIELD)
class Reference {
    @XmlElement
    lateinit var db: String
    @XmlElement
    lateinit var id: String

    constructor()

    constructor(db: String, id: String) {
        this.db = db
        this.id = id
    }
}
