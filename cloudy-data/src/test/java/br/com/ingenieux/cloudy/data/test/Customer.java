package br.com.ingenieux.cloudy.data.test;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;

import br.com.ingenieux.cloudy.data.SDBEntity;

@Entity
@Table(name="odesk-customer")
public class Customer extends SDBEntity {
    private static final long serialVersionUID = -7176287205718570971L;

    private String domain;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    private Integer revision = Integer.valueOf(0);
    
    @Version
    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }
}
