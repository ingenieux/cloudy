package com.intridea.io.vfs.provider.s3;


import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

/**
 * An S3 file system.
 *
 * @author Marat Komarov
 * @author Matthias L. Jugel
 */
public class S3FileSystem extends AbstractFileSystem {

    private AmazonS3 service;
    
    private Bucket bucket;

    private Log logger = LogFactory.getLog(S3FileSystem.class);

    public S3FileSystem(S3FileName fileName, AmazonS3 service,
            FileSystemOptions fileSystemOptions) throws FileSystemException {
        super(fileName, null, fileSystemOptions);
        String bucketId = fileName.getBucketId();
        try {
            this.service = service;
            bucket = new Bucket(bucketId);

            logger.info(String.format("Created new S3 FileSystem %s", bucketId));
        } catch (Exception e) {
            String s3message = e.getMessage();

            if (s3message != null) {
                throw new FileSystemException(s3message);
            } else {
                throw new FileSystemException(e);
            }
        }
    }

	@Override
    protected void addCapabilities(Collection<Capability> caps) {
        caps.addAll(S3FileProvider.capabilities);
    }

    @Override
    protected FileObject createFile(AbstractFileName fileName) throws Exception {
        return new S3FileObject(fileName, this, service, bucket);
    }

}
