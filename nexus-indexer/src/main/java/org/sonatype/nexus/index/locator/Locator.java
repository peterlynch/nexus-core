/**
 * Copyright (c) 2007-2008 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.sonatype.nexus.index.locator;

import java.io.File;

/**
 * An artifact locator used to locate repository "elements" relative to some file. 
 * 
 * @author Jason van Zyl
 */
public interface Locator
{
    String ROLE = Locator.class.getName();

    File locate( File source );
}
