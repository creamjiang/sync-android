/**
 * Copyright (c) 2014 Cloudant, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.sync.datastore;

import com.cloudant.sync.util.Misc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * An attachment which has been been copied to a temporary location and had its sha1 calculated,
 * prior to being added to the datastore.
 *
 * In most cases, this class will only be used by the AttachmentManager and BasicDatastore classes.
 */
public class PreparedAttachment {

    public final Attachment attachment;
    public final File tempFile;
    public final byte[] sha1;
    public final long length;
    public final long encodedLength;

    /**
     * Prepare an attachment by copying it to a temp location and calculating its sha1.
     *
     * @param attachment The attachment to prepare
     * @param attachmentsDir The 'BLOB store' or location where attachments are stored for this database
     * @param length Length in bytes, before any encoding. This argument is ignored if the attachment is not encoded
     * @throws AttachmentNotSavedException
     */
    public PreparedAttachment(Attachment attachment,
                              String attachmentsDir,
                              long length) throws AttachmentException {
        this.attachment = attachment;
        this.tempFile = new File(attachmentsDir, "temp" + UUID.randomUUID());
        FileInputStream tempFileIS = null;
        try {
            FileUtils.copyInputStreamToFile(attachment.getInputStream(), tempFile);
            if (this.attachment.encoding == Attachment.Encoding.Plain) {
                this.length = tempFile.length();
                // 0 signals "no encoded length" - this is consistent with couch which does not send
                // encoded_length if the encoding is "plain"
                this.encodedLength = 0;
            } else {
                // the pre-encoded length is known, so store it
                this.length = length;
                this.encodedLength = tempFile.length();
            }
            this.sha1 = Misc.getSha1((tempFileIS = new FileInputStream(tempFile)));
        } catch (IOException e) {
            throw new AttachmentNotSavedException(e);
        } finally {
            //ensure the temp file is closed after calculating the hash
            IOUtils.closeQuietly(tempFileIS);
        }
    }


}

