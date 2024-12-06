package org.reactome.server.tools.model

import java.util.*
import javax.xml.bind.annotation.*


/**
 * Created by Chuqiao on 2019/11/11.
 */
@XmlRootElement(name = "metadata")
@XmlAccessorType(XmlAccessType.FIELD)
class Icon {
    @XmlElementWrapper(name = "categories")
    @XmlElement(name = "category")
    lateinit var categories: List<String>

    @XmlElement
    var person: List<Person>? = null

    @XmlElement
    var name: String? = null

    @XmlElement
    var description: String? = null

    @XmlElement
    var info: String? = null

    @XmlElementWrapper(name = "references")
    @XmlElement(name = "reference")
    var references: List<Reference>? = null

    @XmlElementWrapper(name = "synonyms")
    @XmlElement(name = "synonym")
    var synonyms: List<String>? = null

    @XmlElement
    var isSkip: Boolean = false

    override fun equals(o: Any?): Boolean {
        if (o == null || javaClass != o.javaClass) return false
        val icon = o as Icon
        return isSkip == icon.isSkip && categories == icon.categories && person == icon.person && name == icon.name && description == icon.description && info == icon.info && references == icon.references && synonyms == icon.synonyms
    }

    override fun hashCode(): Int {
        return Objects.hash(
            categories,
            person,
            name,
            description,
            info,
            references,
            synonyms,
            isSkip
        )
    }
}
