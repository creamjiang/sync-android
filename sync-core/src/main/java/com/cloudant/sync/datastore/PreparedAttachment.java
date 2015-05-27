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

    // TODO this ctor when the attachment comes from the server

    /**
     * Prepare an attachment by copying it to a temp location and calculating its sha1.
     *
     * @param attachment The attachment to prepare
     * @param attachmentsDir The 'BLOB store' or location where attachments are stored for this database
     * @throws AttachmentNotSavedException
     */
    public PreparedAttachment(Attachment attachment,
                              long length,
                              long encodedLength,
                              String attachmentsDir) throws AttachmentException {
        this.attachment = attachment;
        this.tempFile = new File(attachmentsDir, "temp" + UUID.randomUUID());
        this.length = length;
        this.encodedLength = encodedLength;
        FileInputStream tempFileIS = null;
        try {
            FileUtils.copyInputStreamToFile(attachment.getInputStream(), tempFile);
            long expectedLength;
            if (this.attachment.encoding == Attachment.Encoding.Plain) {
                expectedLength = length;
            } else {
                expectedLength = encodedLength;
            }
            if (this.tempFile.length() != expectedLength) {
                FileUtils.deleteQuietly(tempFile);
                // FIXME - message
                throw new IllegalStateException("Lengths do not match");
            }
            this.sha1 = Misc.getSha1((tempFileIS = new FileInputStream(tempFile)));
        } catch (IOException e) {
            throw new AttachmentNotSavedException(e);
        } finally {
            //ensure the temp file is closed after calculating the hash
            IOUtils.closeQuietly(tempFileIS);
        }
    }

    // TODO this ctor when the attachment comes from the user (so there is no external metadata about the length)

    /**
     * Prepare an attachment by copying it to a temp location and calculating its sha1.
     *
     * @param attachment The attachment to prepare
     * @param attachmentsDir The 'BLOB store' or location where attachments are stored for this database
     * @throws AttachmentNotSavedException
     */
    public PreparedAttachment(Attachment attachment,
                              String attachmentsDir) throws AttachmentException {
        this.attachment = attachment;
        this.tempFile = new File(attachmentsDir, "temp" + UUID.randomUUID());
        FileInputStream tempFileIS = null;
        try {
            FileUtils.copyInputStreamToFile(attachment.getInputStream(), tempFile);
            if (this.attachment.encoding == Attachment.Encoding.Plain) {
                this.length = tempFile.length();
                this.encodedLength = 0;
            } else {
                this.length = 0; // TODO - what is the unencoded length?
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

    public final Attachment attachment;
    public final File tempFile;
    public final byte[] sha1;
    public final long length;
    public final long encodedLength;

}

