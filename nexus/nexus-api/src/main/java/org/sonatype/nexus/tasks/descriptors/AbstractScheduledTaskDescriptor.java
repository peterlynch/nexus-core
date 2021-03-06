/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.tasks.descriptors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepoComboFormField;
import org.sonatype.nexus.formfields.RepoOrGroupComboFormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.tasks.descriptors.properties.AbstractBooleanPropertyDescriptor;
import org.sonatype.nexus.tasks.descriptors.properties.AbstractNumberPropertyDescriptor;
import org.sonatype.nexus.tasks.descriptors.properties.AbstractRepositoryOrGroupPropertyDescriptor;
import org.sonatype.nexus.tasks.descriptors.properties.AbstractRepositoryPropertyDescriptor;
import org.sonatype.nexus.tasks.descriptors.properties.AbstractStringPropertyDescriptor;
import org.sonatype.nexus.tasks.descriptors.properties.ScheduledTaskPropertyDescriptor;

public abstract class AbstractScheduledTaskDescriptor
    implements ScheduledTaskDescriptor
{
    public boolean isExposed()
    {
        return true;
    }
    
    @Deprecated
    public List<ScheduledTaskPropertyDescriptor> getPropertyDescriptors()
    {
        return Collections.emptyList();
    }
    
    /**
     * Helper method, that will convert from old api to new api, saving plugin devs
     * some headaches
     */
    public List<FormField> formFields()
    {
        if ( getPropertyDescriptors().size() > 0 )
        {
            List<FormField> formFields = new ArrayList<FormField>();
            
            for ( ScheduledTaskPropertyDescriptor prop : getPropertyDescriptors() )
            {
                if ( prop instanceof AbstractBooleanPropertyDescriptor )
                {
                    formFields.add( new CheckboxFormField( prop.getId(), prop.getName(), prop.getHelpText(), prop.isRequired() ) );
                }
                else if ( prop instanceof AbstractNumberPropertyDescriptor )
                {
                    formFields.add( new NumberTextFormField( prop.getId(), prop.getName(), prop.getHelpText(), prop.isRequired(), prop.getRegexValidation() ) );
                }
                else if ( prop instanceof AbstractStringPropertyDescriptor )
                {
                    formFields.add( new StringTextFormField( prop.getId(), prop.getName(), prop.getHelpText(), prop.isRequired(), prop.getRegexValidation() ) );
                }
                else if ( prop instanceof AbstractRepositoryOrGroupPropertyDescriptor )
                {
                    formFields.add( new RepoOrGroupComboFormField( prop.getId(), prop.getName(), prop.getHelpText(), prop.isRequired(), prop.getRegexValidation() ) );
                }
                else if ( prop instanceof AbstractRepositoryPropertyDescriptor )
                {
                    formFields.add( new RepoComboFormField( prop.getId(), prop.getName(), prop.getHelpText(), prop.isRequired(), prop.getRegexValidation() ) );
                }
            }
            
            return formFields;
        }
        else
        {
            return Collections.emptyList();
        }
    }
}
