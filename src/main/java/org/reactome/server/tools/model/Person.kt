package org.reactome.server.tools.model

import javax.xml.bind.annotation.*

/**
 * Created by Chuqiao on 2019/11/12.
 */
@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.FIELD)
class Person {
    @XmlAttribute
    var role: String? = null

    @XmlAttribute
    var orcid: String? = null

    @XmlAttribute
    var url: String? = null

    @XmlValue
    var name: String? = null

    constructor()

    constructor(role: String?, orcid: String?, url: String?, name: String?) {
        this.role = role
        this.orcid = orcid
        this.url = url
        this.name = name
    }
}
