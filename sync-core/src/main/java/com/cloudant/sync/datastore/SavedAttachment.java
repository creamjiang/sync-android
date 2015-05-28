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

import com.cloudant.sync.replication.PushAttachmentsInline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Created by tomblench on 14/03/2014.
 */

/**
 * An Attachment which has been retrieved from the Database
 */
class SavedAttachment extends Attachment {

    // how many bytes should an attachment be to be considered large?
    static final int largeSizeBytes = 65536;
    private static final Logger logger = Logger.getLogger(SavedAttachment.class.getCanonicalName());

    private final AttachmentStreamFactory attachmentStreamFactory;

    protected SavedAttachment(String name, long revpos, long seq, byte[] key, String type, File file, Encoding encoding,
                              AttachmentStreamFactory asf) {
        super(name, type, encoding);
        this.revpos = revpos;
        this.seq = seq;
        this.key = key;
        this.file = file;
        this.encoding = encoding;

        this.attachmentStreamFactory = asf;
    }

    public InputStream getInputStream() throws IOException {
        return this.attachmentStreamFactory.getInputStream(file, encoding);
    }

    public boolean isLarge() {
        return this.getSize() > largeSizeBytes;
    }

    public boolean shouldInline(PushAttachmentsInline inlinePreference) {
        // push_attachments_inline: false = always push multipart; small = push small attachments inline; true = always push inline
        if (inlinePreference == PushAttachmentsInline.False || inlinePreference == PushAttachmentsInline.Small && this.isLarge()) {
            logger.finer("inline false");
            return false;
        } else {
            logger.finer("inline true");
            return true;
        }
    }

    public long getSize() {
        return file.length();
    }

    protected final long revpos;
    protected final long seq;
    protected final byte[] key;  // sha of file, used for file path on disk.
    private final File file;
    private Encoding encoding;

}
