/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package com.phenix.pct;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.FileSet;

import java.io.File;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Binary load task
 * 
 * @author <a href="mailto:g.querret+PCT@gmail.com">Gilles QUERRET</a>
 * @version $Revision$
 */
public class PCTBinaryLoad extends PCT {
    private List<PCTConnection> dbConnList = null;
    private List<FileSet> filesets = new ArrayList<FileSet>();
    private int indexRebuildTimeout = 0;
    private boolean rebuildIndexes = true;

    /**
     * Adds a database connection
     * 
     * @param dbConn Instance of DBConnection class
     */
    public void addPCTConnection(PCTConnection dbConn) {
        if (this.dbConnList == null) {
            this.dbConnList = new ArrayList<PCTConnection>();
        }

        this.dbConnList.add(dbConn);
    }

    /**
     * Adds a set of files to archive.
     * 
     * @param set FileSet
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
    }

    /**
     * Sets the timeout before indexes are rebuilt (-G parameter)
     * 
     * @param timeout Timeout
     */
    public void setIndexRebuildTimeout(int timeout) {
        if (timeout < 0) {
            throw new BuildException(Messages.getString("PCTBinaryLoad.0")); //$NON-NLS-1$
        }

        this.indexRebuildTimeout = timeout;
    }

    /**
     * Sets to false if indexes shouldn't be rebuilt
     * 
     * @param rebuildIndexes boolean
     */
    public void setRebuildIndexes(boolean rebuildIndexes) {
        this.rebuildIndexes = rebuildIndexes;
    }

    /**
     * Do the work
     * 
     * @throws BuildException Something went wrong
     */
    public void execute() throws BuildException {
        ExecTask exec = null;

        checkDlcHome();
        if (this.dbConnList == null) {
            throw new BuildException(Messages.getString("PCTBinaryLoad.1")); //$NON-NLS-1$
        }

        if (this.dbConnList.size() > 1) {
            throw new BuildException(MessageFormat.format(
                    Messages.getString("PCTBinaryLoad.2"), "1")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        for (FileSet fs : filesets) {
            for (String str : fs.getDirectoryScanner(this.getProject()).getIncludedFiles()) {
                File foo = new File(fs.getDir(this.getProject()), str);
                exec = loadTask(foo);
                exec.execute();
            }
        }
    }

    private ExecTask loadTask(File binaryFile) {
        ExecTask exec = new ExecTask(this);
        File a = getExecPath("_proutil"); //$NON-NLS-1$
        exec.setExecutable(a.toString());

        // Database connections
        for (PCTConnection dbc : dbConnList) {
            dbc.createArguments(exec);
        }

        // Binary load
        exec.createArg().setValue("-C"); //$NON-NLS-1$
        exec.createArg().setValue("load"); //$NON-NLS-1$

        // File to load
        exec.createArg().setValue(binaryFile.getAbsolutePath());

        // Rebuild indexes
        if (rebuildIndexes) {
            exec.createArg().setValue("build"); //$NON-NLS-1$
            exec.createArg().setValue("indexes"); //$NON-NLS-1$
            exec.createArg().setValue("-G"); //$NON-NLS-1$
            exec.createArg().setValue(Integer.toString(indexRebuildTimeout));
        }

        return exec;
    }
}
