package br.com.ingenieux.cloudy.data;

import java.util.Date;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

public class SDBEntityListener {
    @PrePersist
    public void prePersist(Object obj) {
        handleEntity((SDBEntity) obj);
    }
    
    @PreUpdate
    public void preUpdate(Object obj) {
        handleEntity((SDBEntity) obj);
    }

    protected void handleEntity(SDBEntity sdbEntity) {
        Date now = new Date();

        if (null == sdbEntity.getCreated()) {
            sdbEntity.setCreated(now);
        }
        
        sdbEntity.setUpdated(now);
    }
}
