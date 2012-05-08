package br.com.ingenieux.cloudy.data;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

@MappedSuperclass
@EntityListeners(SDBEntityListener.class)
public class SDBEntity implements Serializable, Comparable<SDBEntity> {
    private static final long serialVersionUID = 4614840181866173966L;

    protected String itemName;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty("_id")
    @NotEmpty
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    protected Date created;

    @Basic(fetch=FetchType.EAGER)
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    protected Date updated;

    @Basic(fetch=FetchType.EAGER)
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    private Integer revision = Integer.valueOf(0);

    @Basic(fetch=FetchType.EAGER)
    @JsonProperty("_rev")
    @Version
    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    @Override
    public int compareTo(SDBEntity o) {
    	if (!this.getClass().equals(o.getClass()))
    		return -1;
    	
    	int result = this.getItemName().compareTo(o.getItemName());
    	
    	if (0 != result)
    		return result;
    	
    	return this.revision.compareTo(o.getRevision());
    }
}
