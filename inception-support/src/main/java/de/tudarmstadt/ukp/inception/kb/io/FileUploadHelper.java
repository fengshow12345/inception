/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb.io;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.file.IFileCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUploadHelper
{

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final IFileCleaner fileTracker;

    public FileUploadHelper(Application application)
    {
        fileTracker = application.getResourceSettings().getFileCleaner();
    }

    /**
     * Writes the input stream to a temporary file. The file is deleted if the object behind marker
     * is garbage collected. The temporary file will keep its extension based on the specified file
     * name.
     *
     * @param fileUpload The file upload handle to write to the temporary file
     * @param marker The object to whose lifetime the temporary file is bound
     * @return A handle to the created temporary file
     * @throws Exception
     */
    public File writeToTemporaryFile(FileUpload fileUpload, Object marker) throws Exception
    {
        String fileName = fileUpload.getClientFileName();
        File tmpFile = File.createTempFile("inception_upload", fileName);
        log.debug("Creating temporary file for [{}] in [{}]", fileName, tmpFile.getAbsolutePath());
        fileTracker.track(tmpFile, marker);
        try (InputStream is = fileUpload.getInputStream()) {
            FileUtils.copyInputStreamToFile(is, tmpFile);
        }
        return tmpFile;
    }
}
